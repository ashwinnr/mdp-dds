package mdp.solve.online;

import java.util.Arrays;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import mdp.define.PolicyStatistics;
import mdp.generalize.trajectory.Generalization;
import mdp.generalize.trajectory.GenericTransitionGeneralization;
import mdp.generalize.trajectory.OptimalActionGeneralization;
import mdp.generalize.trajectory.ValueGeneralization;
import mdp.generalize.trajectory.parameters.GeneralizationParameters;
import mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH;
import mdp.generalize.trajectory.parameters.GenericTransitionParameters;
import mdp.generalize.trajectory.parameters.OptimalActionParameters;
import mdp.generalize.trajectory.parameters.ValueGeneralizationParameters;
import mdp.generalize.trajectory.type.GeneralizationType;
import rddl.EvalException;
import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredReward;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.mdp.RDDLFactoredTransition;
import util.CommandLineOptionable;
import util.Timer;
import util.UnorderedPair;
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
	P extends GeneralizationParameters<T> > extends RDDLOnlineActor 
	implements CommandLineOptionable< SymbolicRTDP<T,P> > {

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
	
	private GenericTransitionGeneralization<T, P> _generalizer;
	private GenericTransitionParameters<T, P, RDDLFactoredStateSpace, RDDLFactoredActionSpace> _genaralizeParameters;
	private Exploration< RDDLFactoredStateSpace, RDDLFactoredActionSpace > exploration;  
	private static FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> cur_action 
		= new FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>( );

	public SymbolicRTDP(){};

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
			final Exploration<RDDLFactoredStateSpace, RDDLFactoredActionSpace> exploration ) {
	    
		super( domain, instance, FAR, debug, order, seed, useDiscounting, numStates, numRounds, init_state_conf,
				init_state_prob );
		this.exploration = exploration; 
		
		_generalizer = new GenericTransitionGeneralization<T, P>(_dtr);
		
		_genaralizeParameters = new GenericTransitionParameters<T, P, 
				RDDLFactoredStateSpace, RDDLFactoredActionSpace>(_manager, 
						GENERALIZE_PATH.ALL_PATHS, new Random( _rand.nextLong() ),
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
		final UnorderedPair<ADDRNode, ADDRNode> init 
			= _dtr.computeLAOHeuristic( steps_heuristic, heuristic_type, CONSTRAIN_NAIVELY,
				do_apricodd, apricodd_epsilon[0], apricodd_type, MB, time_heuristic_mins );
		
		_valueDD = new ADDRNode[ steps_lookahead ];
		_policyDD = new ADDRNode[ steps_lookahead ];
		
		_valueDD[ steps_lookahead-1 ] = init._o1;
		_policyDD[ steps_lookahead-1 ] = init._o2;//_dtr.getNoOpPolicy( _mdp.get_actionVars(), _manager );

		final ADDRNode RMAX = _mdp.getRMax();
		//why add RMAX? init is admissible
		//why not add R?
//		final ADDRNode reward_with_action = _mdp.getSumOfRewards();
//		final UnorderedPair<ADDRNode, ADDRNode> greedy_policy 
//			= _dtr.getGreedyPolicy( reward_with_action );
//		final ADDRNode reward = greedy_policy._o1;
		
		for( int depth = steps_lookahead-2; depth >= 0; --depth ){
		    _valueDD[ depth ] = _manager.apply( RMAX, _valueDD[ depth+1 ] , DDOper.ARITH_PLUS );
		    _policyDD[ depth ] = init._o2;
		}
		
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
			int steps_to_go = 0;//steps_lookahead;
//			System.out.println("value of init state : " + _manager.evaluate(value_fn, init_state.getFactoredState() ).getNode().toString() );
			
			NavigableMap<String, Boolean> state_assign = cur_state.getFactoredState();
			trajectory_states[ steps_to_go ].setFactoredState( state_assign );
			while( steps_to_go < steps_lookahead-1 ){
//				System.out.println( cur_state.toString() );
				//				System.out.println(_manager.evaluate(value_fn, cur_state.getFactoredState() ).getNode().toString() );
			    
//			    System.out.println( state_assign );
			    	
				final ADDRNode action_dd = _manager.restrict( _policyDD[steps_to_go], state_assign );
				final NavigableMap<String, Boolean> action = _manager.sampleOneLeaf(action_dd, _rand );
				
				trajectory_actions[ steps_to_go ].setFactoredAction( action );
//				System.out.println( action );
				cur_action.setFactoredAction( action );
				
				cur_state = _transition.sampleFactored(cur_state, cur_action);
				state_assign = cur_state.getFactoredState();
				++steps_to_go;
				trajectory_states[ steps_to_go ].setFactoredState( state_assign );

				//				System.out.println( "Steps to go " + steps_to_go );
//				System.out.println( cur_action.toString() );
//				System.out.print("-");
				
			}
//			trajectory_states[ trajectory_states.length-1 ].setFactoredState( cur_state.getFactoredState() );
			
//			System.out.println("Updating : " + _manager.countPaths(trajectory_states) );
			final ADDRNode[] gen_trajectory = generalize_trajectory( trajectory_states,
					trajectory_actions );
			_DPTimer.ResumeTimer();			
			update_generalized_trajectory( gen_trajectory );
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
		
		return _generalizer.generalize_trajectory(trajectory_states, trajectory_actions, _genaralizeParameters);
	}
	
	protected void update_generalized_trajectory( final ADDRNode[] trajectory ){
		
		int i = trajectory.length-1;
		int j = steps_lookahead-1;
				
		while( i >= 2 ){
			
			final ADDRNode this_next_states = trajectory[i--];
			final ADDRNode this_actions = trajectory[i--];
			final ADDRNode this_states = trajectory[i];
			
			final ADDRNode source_val = _valueDD[ j ];
			final ADDRNode target_val = _valueDD[ j-1 ];
			final ADDRNode target_policy = _policyDD[ j-1 ];
			
			final ADDRNode next_states = _dtr.BDDImageAction(this_states, DDQuantify.EXISTENTIAL,
					this_actions, true, _actionVars );//WARNING : constant true
			
			UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> backup  
			= _dtr.backup( target_val, target_policy, source_val, next_states, this_states, dp_type, 
			do_apricodd, apricodd_epsilon[j-1], apricodd_type, true, MB, CONSTRAIN_NAIVELY);
			
			_valueDD[ j-1 ] = backup._o1;
			_policyDD[ j-1 ] = backup._o2._o1;
			_manager.flushCaches();
			
//			System.out.println(j + " " + backup._o2._o2 );
			--j;
			
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

		SymbolicRTDP srtdp = new SymbolicRTDP();
		srtdp = srtdp.instantiateMe(args);
		
		Thread t = new Thread( srtdp );
		t.start();
		t.join();
	}

	@Override
	public Options createOptions() {
		Options ret = new Options();
		ret.addOption("discounting", false, "use discounting in planning " +
				"and testing - double in [0,1]" );
		ret.addOption("testStates", true, "number of initial states to test policy on - integer" );
		ret.addOption("testRounds", true, "number of rounds to test each initial state on - integer" );
		ret.addOption("domain", true, "RDDL domain file name - string");
		ret.addOption("instance", true, "RDDL instance file name - string" );
		ret.addOption("seed", true, "random seed - long" );
		ret.addOption("actionVars", false, "use action variables in ADDs" );
		ret.addOption("constraintPruning", false, "use ADD pruning for constraints" );
		ret.addOption("doApricodd", false, "enable APRICODD for value functions" );
		ret.addOption("apricoddError", true, "error for ADD approximation - double" );
		ret.addOption("apricoddType", true, "APRICODD type - UPPER/LOWER/AVERAGE/NONE/RANGE");
		ret.addOption("apricoddGP", true, "geometric error progression with depth > 1" );
		ret.addOption("heuristicType", true, "backups to use for computing heuristic " +
				"- VI_FAR/VI_SPUDD/VI_MBFAR/VMAX" );
		ret.addOption("heuristicMins",  true, "max. minutes to run heuristic computation - double" );
		ret.addOption("heuristicSteps", true, "number of steps of VI for heuristic computation - int" );
		ret.addOption("memoryBoundNodes", true, "memory bound for MBFAR - long" );
		ret.addOption("initialStateConf", true, "IID initial state distribution " +
				"- CONJUNCTIVE/BERNOULLI/UNIFORM" );
		ret.addOption("initialStateProb", true, "IID parameter - double in [0,1]" );
		ret.addOption("backupType", true, "backups for RTDP - VI_FAR/VI_SPUDD/VI_MBFAR" );
		ret.addOption("numTrajectories", true, "number of trajectories to sample - int");
		ret.addOption("stepsDP", true, "number of trajectory replays -int " );
		ret.addOption("stepsLookahead", true, "number of states in trajectories - int");
		ret.addOption("generalizeStates", false, "whether to generalize states in trajectory" );
		ret.addOption("generalizeActions", false, "whether to generalize actions in trajectory" );
		ret.addOption("limitGeneralizedStates", true, "sample size for number of " +
				"states in generalized states - int" );
		ret.addOption("limitGeneralizedActions", true, "sample size for number of " + 
				"actions in generalized actions - int" );
		ret.addOption("generalization", true, "type of generalization - value/action/off");
//		ret.addOption("exploration", true, "exploration for trajectory - epsilon/off");
		ret.addOption("generalizationRule", true, "rule for generalizing within an ADD - ALL_PATHS/SHARED_PATHS/NONE" );
		
		return ret;
	}

	@Override
	public CommandLine parseOptions(String[] args, Options opts) {
		try {
			return new GnuParser().parse(opts, args);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(1);
		} 
		return null;
	}

	@Override
	public SymbolicRTDP instantiateMe(String[] args) {
		final Options options = createOptions();
		final CommandLine cmd = parseOptions(args, options);

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
				exploration );		
	}

}