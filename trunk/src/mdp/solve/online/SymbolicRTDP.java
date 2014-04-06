package mdp.solve.online;

import java.io.IOException;
import java.util.Arrays;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import mdp.define.PolicyStatistics;
import mdp.generalize.trajectory.Generalization;
import mdp.generalize.trajectory.GenericTransitionGeneralization;
import mdp.generalize.trajectory.GenericTransitionGeneralization.Consistency;
import mdp.generalize.trajectory.OptimalActionGeneralization;
import mdp.generalize.trajectory.ValueGeneralization;
import mdp.generalize.trajectory.parameters.GeneralizationParameters;
import mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH;
import mdp.generalize.trajectory.parameters.GenericTransitionParameters;
import mdp.generalize.trajectory.parameters.OptimalActionParameters;
import mdp.generalize.trajectory.parameters.ValueGeneralizationParameters;
import mdp.generalize.trajectory.type.GeneralizationType;
import mdp.solve.solver.HandCodedPolicies;
import rddl.EvalException;
import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredReward;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.mdp.RDDLFactoredTransition;
import util.InstantiateArgs;
import util.Timer;
import util.UnorderedPair;
import add.ADDLeaf;
import add.ADDManager;
import add.ADDRNode;
import dd.DDManager.APPROX_TYPE;
import dd.DDManager.DDOper;
import dd.DDManager.DDQuantify;
import dtr.add.ADDDecisionTheoreticRegression;
import dtr.add.ADDDecisionTheoreticRegression.BACKUP_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;

public class SymbolicRTDP< T extends GeneralizationType, 
	P extends GeneralizationParameters<T> > extends RDDLOnlineActor  {

	private boolean CONSTRAIN_NAIVELY = false;
//	private double	EPSILON;
	protected Timer _DPTimer = null;
	
	private APPROX_TYPE apricodd_type;
	private double[] apricodd_epsilon;
	private boolean do_apricodd;
	
	private long MB;
	private BACKUP_TYPE dp_type;
	protected int nTrials;
	private int steps_dp;
	protected int steps_lookahead;
	
	private ADDRNode[] _valueDD;
	private ADDRNode[] _policyDD;
//	private ADDRNode base_line;
	
	protected static final FactoredAction[] EMPTY_ACTION_ARRAY = {};
	
	private GenericTransitionGeneralization<T, P> _generalizer;
	private GenericTransitionParameters<T, P, RDDLFactoredStateSpace, RDDLFactoredActionSpace> _genaralizeParameters;
	private Exploration< RDDLFactoredStateSpace, RDDLFactoredActionSpace > exploration;  
	private static FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> cur_action 
		= new FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>( );
	
	private ADDRNode[] _visited; 
	
	private boolean truncateTrials;

	public SymbolicRTDP(
			final String domain, 
			final String instance, 
//			final double epsilon,
			final DEBUG_LEVEL debug, 
			final ORDER order, 
			final long seed,
			final boolean useDiscounting, 
			final int numStates,
			final int numRounds,
			final boolean FAR ,
			final boolean constrain_naively ,
			final boolean do_apricodd,
			final double[] apricodd_epsilon,
			final APPROX_TYPE apricodd_type,
			final BACKUP_TYPE heuristic_type,
			final double time_heuristic_mins, 
			final int steps_heuristic, 
			final long MB ,
			final INITIAL_STATE_CONF init_state_conf ,
			final double init_state_prob,
			final BACKUP_TYPE dp_type,
			final int nTrials,
			final int steps_dp,
			final int steps_lookahead ,
			final Generalization< RDDLFactoredStateSpace, RDDLFactoredActionSpace, T, P > generalizer, 
			final P generalize_parameters_wo_manager ,
			final boolean gen_fix_states,
			final boolean gen_fix_actions,
			final int gen_num_states,
			final int gen_num_actions, 
			final GENERALIZE_PATH gen_rule,
			final Exploration<RDDLFactoredStateSpace, RDDLFactoredActionSpace> exploration,
			final Consistency[] cons,
			final boolean  truncateTrials ) {
	    
		super( domain, instance, FAR, debug, order, seed, useDiscounting, numStates, numRounds, init_state_conf,
				init_state_prob );
		this.exploration = exploration; 
		this.truncateTrials = truncateTrials;
		_generalizer = new GenericTransitionGeneralization<T, P>( _dtr, cons );
		
		_genaralizeParameters = new GenericTransitionParameters<T, P, 
				RDDLFactoredStateSpace, RDDLFactoredActionSpace>(_manager, 
						gen_rule, new Random( _rand.nextLong() ),
						gen_fix_states, gen_fix_actions, gen_num_actions, gen_num_states, 
						generalizer, generalize_parameters_wo_manager);
		//		EPSILON = epsilon;
		this.steps_dp = steps_dp;
		this.steps_lookahead = steps_lookahead;

		this.nTrials = nTrials;
		_manager = _mdp.getManager();
		CONSTRAIN_NAIVELY = constrain_naively;
		this.do_apricodd = do_apricodd;
		this.apricodd_epsilon = apricodd_epsilon;
		this.apricodd_type = apricodd_type;
		this.MB = MB;
		this.dp_type = dp_type;
		
		//heuristic too bad anyways
		//final UnorderedPair<ADDRNode, ADDRNode> init 
		//	= _dtr.computeLAOHeuristic( steps_heuristic, heuristic_type, CONSTRAIN_NAIVELY,
		//		false, 0.0d, apricodd_type, MB, time_heuristic_mins );
		
		_valueDD = new ADDRNode[ steps_lookahead ];
		_policyDD = new ADDRNode[ steps_lookahead ];
		
		final ADDRNode sum_rew = _mdp.getSumOfRewards();
		final UnorderedPair<ADDRNode, ADDRNode> greedy = _dtr.getGreedyPolicy( sum_rew );
		
		_valueDD[ steps_lookahead-1 ] = greedy._o1;
		
		_policyDD[ steps_lookahead-1 ] = greedy._o2;//_dtr.getGreedyPolicy( sum_rew );
		    //HandCodedPolicies.get( domain , _dtr, _manager, _mdp.get_actionVars() );

		final ADDRNode RMAX = _manager.getLeaf( _mdp.getRMax() );
		//why add RMAX? init is admissible
		//why not add R?
//		final ADDRNode reward_with_action = _mdp.getSumOfRewards();
//		final UnorderedPair<ADDRNode, ADDRNode> greedy_policy 
//			= _dtr.getGreedyPolicy( reward_with_action );
//		final ADDRNode reward = greedy_policy._o1;
		
		for( int depth = steps_lookahead-2; depth >= 0; --depth ){
		    _valueDD[ depth ] = _manager.apply( RMAX, _valueDD[ depth+1 ] , DDOper.ARITH_PLUS );
		    _policyDD[ depth ] = _policyDD[ depth+1 ];
		}
		
		_visited = new ADDRNode[ steps_lookahead ];
		for( int depth =  0; depth < steps_lookahead-1; ++depth ){
		    _visited[ depth ] = _manager.DD_ZERO;
		}
		_visited[ steps_lookahead-1 ] = _manager.DD_ONE;
		
		_DPTimer = new Timer();
		_DPTimer.PauseTimer();
//		try {
//			base_line = ADDDecisionTheoreticRegression.getRebootDeadPolicy(_manager, _dtr, _mdp.get_actionVars() );
//		} catch (EvalException e) {
//			e.printStackTrace();
//		}
//		display(_valueDD, _policyDD);
		
	}

	@Override
	public FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> act(
			final FactoredState<RDDLFactoredStateSpace> state) {
		if( _policyDD == null ){
			try {
				throw new Exception("policy is null");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		do_sRTDP( state );
		display( _valueDD, _policyDD );

		final ADDRNode action_dd 
			= _manager.restrict(_policyDD[0] ,  state.getFactoredState() );
		final NavigableMap<String, Boolean> action 
			= ADDManager.sampleOneLeaf(action_dd, _rand );
		
		_manager.flushCaches();
		
		cur_action.setFactoredAction(action);
		
		return cur_action;
	}

	private void display(
			final ADDRNode[] values, final ADDRNode[] policies ) {
		for( int i = 0 ; i < values.length; ++i ){
			ADDRNode vfn = values[i];
			ADDRNode plcy = policies[i];
			System.out.println("i = " + i );
			System.out.println("Size of Value fn. " + _manager.countNodes(vfn) );
			System.out.println("Size of policy " + _manager.countNodes( plcy ) );
			System.out.println("DP time: " + _DPTimer.GetElapsedTimeInMinutes() );	
		}
	}

	protected void do_sRTDP( final FactoredState<RDDLFactoredStateSpace> init_state ) {

		int trials_to_go = nTrials;
		_DPTimer.ResetTimer();
		final FactoredState<RDDLFactoredStateSpace>[] trajectory_states = new FactoredState[ steps_lookahead ];
		final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>[] trajectory_actions 
			= new FactoredAction[ steps_lookahead - 1 ];
		for( int i = 0 ; i < steps_lookahead-1; ++i ){
		    trajectory_actions[i] = new FactoredAction( );
		    trajectory_states[i] = new FactoredState( );
		}
		trajectory_states[ steps_lookahead - 1 ] = new FactoredState();
		
		while( trials_to_go --> 0 ){
			FactoredState<RDDLFactoredStateSpace> cur_state = init_state;
			int num_actions = 0;//steps_lookahead;
//			System.out.println("value of init state : " + _manager.evaluate(value_fn, init_state.getFactoredState() ).getNode().toString() );
			
			NavigableMap<String, Boolean> state_assign = cur_state.getFactoredState();
			trajectory_states[ num_actions ].setFactoredState( state_assign );
			
			while( num_actions < steps_lookahead-1 ){
//				System.out.println( cur_state.toString() );
				//				System.out.println(_manager.evaluate(value_fn, cur_state.getFactoredState() ).getNode().toString() );
			    
//			    System.out.println( state_assign );
			    final boolean newState = _manager.evaluate( _visited[ num_actions ], 
						state_assign ).equals( _manager.DD_ZERO );
				_visited[ num_actions ] = _manager.BDDUnion(_visited[ num_actions ], 
						_manager.getProductBDDFromAssignment(cur_state.getFactoredState()) );
				if( newState && truncateTrials ){
					System.out.println("Truncating trial : " + num_actions );
//					System.out.println( state_assign );
					break;
				}
			    	
				final ADDRNode action_dd = _manager.restrict( _policyDD[num_actions], state_assign );
				final NavigableMap<String, Boolean> action = _manager.sampleOneLeaf(action_dd, _rand );
				
				trajectory_actions[ num_actions ].setFactoredAction( action );
//				System.out.println( action );
				cur_action.setFactoredAction( action );
//				System.out.println( cur_action.toString() );
				
				cur_state = _transition.sampleFactored(cur_state, cur_action);
				state_assign = cur_state.getFactoredState();
				
				//				System.out.println( "Steps to go " + steps_to_go );
				++num_actions;
				trajectory_states[ num_actions ].setFactoredState( state_assign );
				
//				System.out.print("-");
				
			}
//			System.out.println( state_assign );
			
//			trajectory_states[ trajectory_states.length-1 ].setFactoredState( cur_state.getFactoredState() );
			
//			System.out.println("Updating : " + _manager.countPaths(trajectory_states) );
			final FactoredState<RDDLFactoredStateSpace>[] trajectory_states_non_null 
				= truncateTrials ? Arrays.copyOfRange(trajectory_states, 0, num_actions+1)
						: trajectory_states ;
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>[] 
					trajectory_actions_non_null = truncateTrials ? 
							( num_actions == 0 ? EMPTY_ACTION_ARRAY : Arrays.copyOfRange(trajectory_actions, 0, num_actions) ) 
							: trajectory_actions;
			
			final ADDRNode[] gen_trajectory = generalize_trajectory( trajectory_states_non_null,
					trajectory_actions_non_null );
			_DPTimer.ResumeTimer();			
			update_generalized_trajectory( trajectory_states_non_null, trajectory_actions_non_null, 
					gen_trajectory , num_actions+1 );
			_DPTimer.PauseTimer();
			System.out.print("*");			
//			System.out.println("Trials to go  " + trials_to_go );
		}
		System.out.println();
	}
	
	protected ADDRNode[] generalize_trajectory(final FactoredState<RDDLFactoredStateSpace>[] trajectory_states, 
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>[] trajectory_actions  ){

		_genaralizeParameters.set_valueDD(_valueDD);
		_genaralizeParameters.set_policyDD( _policyDD );
		_genaralizeParameters.getParameters().set_valueDD(_valueDD);
		_genaralizeParameters.getParameters().set_policyDD(_policyDD);
		_genaralizeParameters.set_visited(_visited);
		_genaralizeParameters.getParameters().set_visited(_visited);
//		System.out.println( "Generalizing " );
		
		return _generalizer.generalize_trajectory(trajectory_states, trajectory_actions, _genaralizeParameters);
	}
	
	protected void update_generalized_trajectory( 
			final FactoredState<RDDLFactoredStateSpace>[] trajectory_states, 
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>[] trajectory_actions, 
			final ADDRNode[] trajectory,
			final int num_states ){
		
	    
		int i = (num_states-1)*2;//-1;
		int j = num_states-1;
		
		if( num_states != steps_lookahead  && truncateTrials ){
	    	//new state in last step
	    	//dont update it rather get heuristic value for it
	    	int idx = -1;
	    	for( int searcher = j; searcher >= 0; --searcher ){
	    		//search for previous visitation and update heuristic
	    		//otherwise leave it alone
	    		if( _manager.evaluate( _visited[ j ], trajectory_states[ j ].getFactoredState() ).equals( _manager.DD_ONE ) ){
	    			idx = searcher;
	    			break;
	    		}
	    	}
	    	
	    	if( idx != -1 ){
	    		//update heuristic
		    	//max rewards heuristic
		    	//min rewards heuristic
	    		final double old_val = _manager.evaluate( _valueDD[j],
	    				trajectory_states[j].getFactoredState() ).getMax();
	    		final double new_val =  _manager.evaluate( _valueDD[idx],
	    				trajectory_states[j].getFactoredState() ).getMax()
	    					- _mdp.getRmin();
//	    		System.out.println( "Delta = " + (old_val - new_val) );
	    		
	    		_valueDD[ j ] = _manager.assign( 
	    				_valueDD[j],
	    				trajectory_states[j].getFactoredState(), 
	    				Math.min(old_val, new_val) );
	    	}
		}
				
		while( i >= 2 ){
		
//	    	System.out.println("Updating trajectories " + j );
	    	
	    	ADDRNode this_next_states, this_states, this_actions;
	    	ADDRNode source_val, target_val, target_policy;
	    	
	    	this_next_states = trajectory[i--];
			this_actions = trajectory[i--];
			this_states = trajectory[i];
		    
			source_val = _valueDD[ j ];
			target_val = _valueDD[ j-1 ];
			target_policy = _policyDD[ j-1 ];
			
			final ADDRNode next_states = _dtr.BDDImageAction(this_states, DDQuantify.EXISTENTIAL,
					this_actions, true, _actionVars );//WARNING : constant true
			
			
			UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> backup  
			= _dtr.backup( target_val, target_policy, source_val, next_states, this_states, dp_type, 
			do_apricodd, do_apricodd ? apricodd_epsilon[j-1] : 0 , apricodd_type, true, MB, CONSTRAIN_NAIVELY);
			
			_valueDD[ j-1 ] = backup._o1;
			_policyDD[ j-1 ] = backup._o2._o1;
			
			_manager.flushCaches();
			
//			System.out.println(j + " " + backup._o2._o2 );
			--j;
			
//			System.out.println(j + " " +
//			_manager.enumeratePaths(this_states, false, true,
//					(ADDLeaf)(_manager.DD_ONE.getNode()), false ).size() );
			
			
		}
//		display(_valueDD, _policyDD);
//		System.out.println();
		
	}

//	private UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> 
//		update_trajectory_backwards(
//				final Stack< NavigableMap<String, Boolean> > trajectory,
//				final ADDRNode current_value,
//				final ADDRNode current_policy ){
//		ADDRNode value_fn_ret = current_value;
//		ADDRNode policy_ret = current_policy;
//		UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> backed = null;
////		System.out.println("Updating trajectory");
//		
//		while( !trajectory.isEmpty() ){
//			final NavigableMap<String, Boolean>  state_assign = trajectory.pop();
////			System.out.println( state_assign );
//			
//			final ADDRNode abstract_state 
//				= _dtr.generalize(value_fn_ret, state_assign, _genRule );
//			final ADDRNode next_states 
//				= _dtr.BDDImage(abstract_state, _actionVars, DDQuantify.EXISTENTIAL);
//			_DPTimer.ResumeTimer();
//			backed = _dtr.backup(value_fn_ret, policy_ret, next_states, abstract_state, 
//					dp_type, do_apricodd, apricodd_epsilon, apricodd_type, 
//					true, MB, CONSTRAIN_NAIVELY );
//			_DPTimer.PauseTimer();
//			value_fn_ret = backed._o1;
//			policy_ret = backed._o2._o1;
//		}
//		return backed;
//	}
	
	public static void main(String[] args) throws InterruptedException {

//		boolean useDiscount = true, actionVars = false, constraint_pruning = false, do_apricodd = false,
//				gen_states = false, gen_actions = false;
//		int num_states = 0, num_rounds = 0, heuristic_steps = -1, 
//				num_trajectories = 0, steps_dp = 0, steps_lookahead = 0, num_gen_states = 0, num_gen_actions = 0;
//		String domain_file = null, instance_file = null;
//		long rand_seed = 83, MB = -1;
//		double apricodd_epsilon = 0, heuristic_mins = 10, init_state_prob = 0;
//		APPROX_TYPE apricodd_type = null;
//		BACKUP_TYPE heuristic_type = BACKUP_TYPE.VI_FAR, backup_type = BACKUP_TYPE.VI_FAR;
//		INITIAL_STATE_CONF init_state = INITIAL_STATE_CONF.UNIFORM;
//		Generalization generalization = null;
//		Exploration<RDDLFactoredStateSpace, RDDLFactoredActionSpace> exploration = null;
//		GeneralizationParameters generalizationParameters = null;
//		
//		for( final String arg : args ){
//
//			int index = arg.indexOf('=');
//			
//			if( arg.startsWith("discounting") ){
//				if( arg.endsWith("true") ){
//					useDiscount = true;
//				}else{
//					useDiscount = false;
//				}
//			}
//			
//			if( arg.startsWith("testStates") ){
//				num_states = Integer.parseInt( arg.substring( index+1 ) );
//			}
//			
//			if( arg.startsWith("testRounds") ){
//				num_rounds = Integer.parseInt( arg.substring( index+1 ) );
//			}
//			
//			if( arg.startsWith("domain") ){
//				domain_file = arg.substring(index+1);
//			}
//			
//			if( arg.startsWith("instance") ){
//				instance_file = arg.substring(index+1);
//			}
//			
//			if( arg.startsWith("randSeed") ){
//				rand_seed = Long.parseLong( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("actionVars") ){
//				actionVars = Boolean.parseBoolean( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("constraintPruning") ){
//				constraint_pruning = Boolean.parseBoolean( arg.substring( index + 1 ) );
//			}
//			
//			if( arg.startsWith("doApricodd") ){
//				do_apricodd = Boolean.parseBoolean( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("apricoddError") ){
//				apricodd_epsilon  = Double.parseDouble(arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("apricoddType") ){
//				apricodd_type = APPROX_TYPE.valueOf( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("heuristicType") ){
//				heuristic_type = BACKUP_TYPE.valueOf( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("heuristicMins") ){
//				heuristic_mins = Double.parseDouble( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("heuristicSteps") ){
//				heuristic_steps = Integer.parseInt( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("memoryBoundNodes") ){
//				MB = Long.parseLong( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("initialStateConf") ){
//				init_state = INITIAL_STATE_CONF.valueOf( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("initialStateProb") ){
//				init_state_prob = Double.parseDouble( arg.substring( index+1 ) );
//			}
//			
//			if( arg.startsWith("backupType") ){
//				backup_type = BACKUP_TYPE.valueOf( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("numTrajectories") ){
//				num_trajectories = Integer.parseInt( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("stepsDP") ){
//				steps_dp = Integer.parseInt( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("stepsLookahead") ){
//				steps_lookahead = Integer.parseInt( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("generalizeStates" ) ){
//				gen_states = Boolean.parseBoolean( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("generalizeActions") ){
//				gen_actions = Boolean.parseBoolean( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("limitGeneralizedStates") ){
//				num_gen_states = Integer.parseInt( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("limitGeneralizedActions") ){
//				num_gen_actions = Integer.parseInt( arg.substring(index+1) );
//			}
//			
//			if( arg.startsWith("generalization" ) ){
//				if( arg.endsWith("value") ){
//					generalization = new ValueGeneralization();
//					generalizationParameters = new ValueGeneralizationParameters(null, 
//							GENERALIZE_PATH.ALL_PATHS, new Random(rand_seed), 
//							Arrays.copyOf(new double[]{apricodd_epsilon}, steps_lookahead), 
//							null, apricodd_type, num_states);
//				}else if( arg.endsWith("action") ){
//					generalization = new OptimalActionGeneralization();
//					generalizationParameters = new OptimalActionParameters(null, GENERALIZE_PATH.ALL_PATHS, 
//							new Random(rand_seed), null, true );
//				}
//			}
//			
//			if( arg.startsWith("exploration-epsilon-greedy" ) ){
//					exploration = new EpsilonGreedyExploration<RDDLFactoredStateSpace, RDDLFactoredActionSpace>
//						(Double.parseDouble(arg.substring(index+1)), rand_seed, 1);
//			}else if( arg.equals("exploration=off") ){
//				exploration = null;
//			}
//		}
//			
//			
//		Runnable worker = new SymbolicRTDP(
//				domain_file, instance_file,
////				Double.parseDouble( args[2] ), 
//				DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, 
//				rand_seed, useDiscount, 
//				num_states, num_rounds, actionVars,
//				constraint_pruning, 
//				do_apricodd, 
//				apricodd_epsilon,  
//				apricodd_type,
//				heuristic_type, 
//				heuristic_mins,
//				heuristic_steps,
//				MB,
//				init_state,
//				init_state_prob, 
//				backup_type,
//				num_trajectories, 
//				steps_dp,
//				steps_lookahead,
//				generalization,
//				generalizationParameters, 
//				!gen_states, 
//				!gen_actions, 
//				num_gen_states,
//				num_gen_actions, 
//				exploration );

		SymbolicRTDP srtdp = SymbolicRTDP.instantiateMe(args);
		
		Thread t = new Thread( srtdp );
		t.start();
		t.join();
	}


	public static SymbolicRTDP instantiateMe(String[] args) {
		
		final Options options = InstantiateArgs.createOptions();
		final CommandLine cmd = InstantiateArgs.parseOptions(args, options);
		try{
		Generalization generalizer = null;
		if( cmd.getOptionValue("generalization").equals("value") ){
			generalizer = new ValueGeneralization();
		}else if( cmd.getOptionValue("generalization").equals("action") ){
			generalizer = new OptimalActionGeneralization();
		}
		
		final Random rand = new Random( Long.parseLong( cmd.getOptionValue("seed") ) );
		
		GeneralizationParameters inner_params = null;
		if( cmd.getOptionValue("generalization").equals("value") ){
				inner_params = new ValueGeneralizationParameters( 
			null, GENERALIZE_PATH.valueOf( cmd.getOptionValue("generalizationRule") ), 
			new Random( rand.nextLong() )  );
		}else if( cmd.getOptionValue("generalization").equals("action") ){
				inner_params = new OptimalActionParameters(
						null, GENERALIZE_PATH.valueOf( cmd.getOptionValue("generalizationRule") ), 
						new Random( rand.nextLong() ) );
		}
		
		double[] epsilons = null;
		if( Boolean.parseBoolean( cmd.getOptionValue("doApricodd") ) ){
			epsilons = new double[ Integer.parseInt( cmd.getOptionValue("stepsLookahead") ) ];
			double epsilon = Double.parseDouble( cmd.getOptionValue("apricoddError") );
			double factor = Double.parseDouble( cmd.getOptionValue("apricoddGP") );
			
			for( int i = 0 ; i < epsilons.length; ++i ){
				epsilons[i] = epsilon;
				epsilon = epsilon*factor ;
			}
		}
		
		Exploration exploration = null;
		
		final String[] const_options = ( cmd.getOptionValues("consistencyRule") );
		final Consistency[] consistency = new Consistency[ const_options.length ];
		for( int i = 0 ; i < const_options.length; ++i ){
		    consistency[i] = Consistency.valueOf(const_options[i]);
		}
		 
		
		return new SymbolicRTDP(
				cmd.getOptionValue("domain"), cmd.getOptionValue("instance"),
				DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, 
				Long.parseLong( cmd.getOptionValue("seed") ), 
				Boolean.parseBoolean( cmd.getOptionValue( "discounting" ) ), 
				Integer.parseInt( cmd.getOptionValue("testStates") ), 
				Integer.parseInt( cmd.getOptionValue( "testRounds" ) ),
				Boolean.parseBoolean( cmd.getOptionValue( "actionVars" ) ), 
				Boolean.parseBoolean( cmd.getOptionValue("constraintPruning") ),
				Boolean.parseBoolean( cmd.getOptionValue("doApricodd") ), 
				epsilons,
				APPROX_TYPE.valueOf( cmd.getOptionValue("apricoddType") ), 
				BACKUP_TYPE.valueOf( cmd.getOptionValue("heuristicType") ),
				Double.parseDouble( cmd.getOptionValue("heuristicMins") ),
				Integer.parseInt( cmd.getOptionValue("heuristicSteps") ),
				Long.parseLong( cmd.getOptionValue("memoryBoundNodes") ),
				INITIAL_STATE_CONF.valueOf( cmd.getOptionValue("initialStateConf") ),
				Double.parseDouble( cmd.getOptionValue("initialStateProb") ),
				BACKUP_TYPE.valueOf( cmd.getOptionValue("backupType") ),
				Integer.parseInt( cmd.getOptionValue("numTrajectories") ),
				Integer.parseInt( cmd.getOptionValue("stepsDP") ),
				Integer.parseInt( cmd.getOptionValue("stepsLookahead") ),
				generalizer, 
				inner_params, 
				!Boolean.parseBoolean( cmd.getOptionValue("generalizeStates") ),
				!Boolean.parseBoolean( cmd.getOptionValue("generalizeActions") ),
				Integer.parseInt( cmd.getOptionValue("limitGeneralizedStates") ),
				Integer.parseInt( cmd.getOptionValue("limitGeneralizedActions") ),
				GENERALIZE_PATH.valueOf( cmd.getOptionValue("generalizationRule") ), 
				exploration,
				consistency,
				Boolean.parseBoolean( cmd.getOptionValue("truncateTrials") ) );
		
		}catch( Exception e ){
			HelpFormatter help = new HelpFormatter();
			help.printHelp("symbolicRTDP", options);
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

}