package mdp.solve.online;

import java.util.NavigableMap;
import java.util.Random;
import java.util.Stack;
import java.util.TreeMap;

import mdp.define.PolicyStatistics;
import mdp.generalize.trajectory.Generalization;
import mdp.generalize.trajectory.GenericTransitionGeneralization;
import mdp.generalize.trajectory.parameters.GeneralizationParameters;
import mdp.generalize.trajectory.parameters.GenericTransitionParameters;
import mdp.generalize.trajectory.type.GeneralizationType;
import rddl.EvalException;
import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredReward;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.mdp.RDDLFactoredTransition;
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

public class SymbolicRTDP< T extends GeneralizationType, P extends GeneralizationParameters<T> > extends RDDLOnlineActor {

	private boolean CONSTRAIN_NAIVELY = false;
	private double	EPSILON;
	protected ADDManager _manager;
	protected Timer _DPTimer = null;
	
	private APPROX_TYPE apricodd_type;
	private double apricodd_epsilon;
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
		= new FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>( null );
	

	public SymbolicRTDP(
			final String domain, 
			final String instance, 
			final double epsilon,
			final DEBUG_LEVEL debug, 
			final ORDER order, final long seed,
			final boolean useDiscounting, 
			final int numStates,
			final int numRounds,
			final boolean FAR ,
			final boolean constrain_naively ,
			final boolean do_apricodd,
			final double apricodd_epsilon,
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
			final Exploration<RDDLFactoredStateSpace, RDDLFactoredActionSpace> exploration ) {
	    
		super( domain, instance, FAR, debug, order, seed, useDiscounting, numStates, numRounds, init_state_conf,
				init_state_prob );
		this.exploration = exploration; 
		
		_generalizer = new GenericTransitionGeneralization<T, P>(_dtr);
		_genaralizeParameters.setGeneralizer(generalizer);
		_genaralizeParameters.set_manager( this._manager );
		_genaralizeParameters.setParameters(generalize_parameters_wo_manager);
		
		EPSILON = epsilon;
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
				do_apricodd, apricodd_epsilon, apricodd_type, MB, time_heuristic_mins );
		
		_valueDD = new ADDRNode[ steps_lookahead ];
		_policyDD = new ADDRNode[ steps_lookahead ];
		
		_valueDD[ steps_lookahead-1 ] = init._o1;
		_policyDD[ steps_lookahead-1 ] = init._o2;//_dtr.getNoOpPolicy( _mdp.get_actionVars(), _manager );

		final ADDRNode RMAX = _mdp.getRMax();
		
		for( int depth = steps_lookahead-2; depth >= 0; --depth ){
		    _valueDD[ depth ] = _manager.apply( RMAX, _valueDD[depth+1], DDOper.ARITH_PLUS );
		    _policyDD[ depth ] = init._o2;
		}
		
		_DPTimer = new Timer();
		_DPTimer.PauseTimer();
//		try {
//			base_line = ADDDecisionTheoreticRegression.getRebootDeadPolicy(_manager, _dtr, _mdp.get_actionVars() );
//		} catch (EvalException e) {
//			e.printStackTrace();
//		}
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
			final UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> result ) {
		ADDRNode vfn = result._o1;
		ADDRNode plcy = result._o2._o1;
		System.out.println("Size of Value fn. " + _manager.countNodes(vfn) );
		System.out.println("Size of policy " + _manager.countNodes( plcy ) );
		System.out.println("DP time: " + _DPTimer.GetElapsedTimeInMinutes() );
	}

	protected void do_sRTDP( final FactoredState<RDDLFactoredStateSpace> init_state ) {

		int trials_to_go = nTrials;
		_DPTimer.ResetTimer();
		final FactoredState<RDDLFactoredStateSpace>[] trajectory_states = new FactoredState[ steps_lookahead ];
		final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>[] trajectory_actions 
			= new FactoredAction[ steps_lookahead ];
		for( int i = 0 ; i < steps_lookahead; ++i ){
		    trajectory_actions[i] = new FactoredAction( );
		    trajectory_states[i] = new FactoredState( );
		}
		
		while( trials_to_go --> 0 ){
			FactoredState<RDDLFactoredStateSpace> cur_state = init_state;
			int steps_to_go = 0;//steps_lookahead;
//			System.out.println("value of init state : " + _manager.evaluate(value_fn, init_state.getFactoredState() ).getNode().toString() );
			
			while( steps_to_go < steps_lookahead ){
//				System.out.println( cur_state.toString() );
				//				System.out.println(_manager.evaluate(value_fn, cur_state.getFactoredState() ).getNode().toString() );
			    	final NavigableMap<String, Boolean> state_assign = cur_state.getFactoredState();
			    	trajectory_states[ steps_to_go ].setFactoredState( state_assign );
			    	
				final ADDRNode action_dd = _manager.restrict( _policyDD[steps_to_go], state_assign );
				final NavigableMap<String, Boolean> action = _manager.sampleOneLeaf(action_dd, _rand );
				
				trajectory_actions[ steps_to_go ].setFactoredAction( action );
				
				cur_action.setFactoredAction( action );
				cur_state = _transition.sampleFactored(cur_state, cur_action);
//				System.out.println( "Steps to go " + steps_to_go );
//				System.out.println( cur_action.toString() );
//				System.out.print("-");		
			}
//			System.out.println("Updating : " + _manager.countPaths(trajectory_states) );
			_DPTimer.ResumeTimer();			
			final ADDRNode[] gen_trajectory = generalize_trajectory( trajectory_states, value_fn, policy );
			_DPTimer.PauseTimer();
			
			value_fn = backed._o1;
			policy = backed._o2._o1;
			System.out.print("*");			
//			System.out.println("Trials to go  " + trials_to_go );
		}
		System.out.println();
		return backed;
	}
	
	protected UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> update_trajectory(
			final ADDRNode states, final ADDRNode value_fn, final ADDRNode policy) {
		
		ADDRNode cur_value = value_fn;
		ADDRNode cur_policy = policy;
		
		final ADDRNode gen_states = _dtr.generalize(value_fn, states, _genRule);
		final ADDRNode next_states = _dtr.BDDImage(gen_states, _actionVars, DDQuantify.EXISTENTIAL);
		
		int iter = steps_dp;
		UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> one_backup  = null;
		do{
			one_backup
				= _dtr.backup(cur_value, cur_policy, next_states, gen_states, dp_type, 
				do_apricodd, apricodd_epsilon, apricodd_type, true, MB, CONSTRAIN_NAIVELY);
//			System.out.println( one_backup._o2._o2 );
			cur_value = one_backup._o1;
			cur_policy = one_backup._o2._o1;
			_manager.flushCaches();
		}while( iter --> 0 && one_backup._o2._o2 > EPSILON );
		return one_backup;
	}

	private UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> 
		update_trajectory_backwards(
				final Stack< NavigableMap<String, Boolean> > trajectory,
				final ADDRNode current_value,
				final ADDRNode current_policy ){
		ADDRNode value_fn_ret = current_value;
		ADDRNode policy_ret = current_policy;
		UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> backed = null;
//		System.out.println("Updating trajectory");
		
		while( !trajectory.isEmpty() ){
			final NavigableMap<String, Boolean>  state_assign = trajectory.pop();
//			System.out.println( state_assign );
			
			final ADDRNode abstract_state 
				= _dtr.generalize(value_fn_ret, state_assign, _genRule );
			final ADDRNode next_states 
				= _dtr.BDDImage(abstract_state, _actionVars, DDQuantify.EXISTENTIAL);
			_DPTimer.ResumeTimer();
			backed = _dtr.backup(value_fn_ret, policy_ret, next_states, abstract_state, 
					dp_type, do_apricodd, apricodd_epsilon, apricodd_type, 
					true, MB, CONSTRAIN_NAIVELY );
			_DPTimer.PauseTimer();
			value_fn_ret = backed._o1;
			policy_ret = backed._o2._o1;
		}
		return backed;
	}
	
	public static void main(String[] args) throws InterruptedException {

		final boolean use_disc = Boolean.parseBoolean( args[4] );
		final int nStates = Integer.parseInt( args[5] );
		final int nRounds = Integer.parseInt( args[6] );

		Runnable worker = new SymbolicRTDP(
				args[0], args[1],
				Double.parseDouble( args[2] ), 
				DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, 
				Long.parseLong( args[3] ), use_disc, 
				nStates, nRounds, Boolean.parseBoolean( args[7] ),
				Boolean.parseBoolean( args[8] ), 
				Boolean.parseBoolean(args[9] ), 
				Double.parseDouble( args[10] ), 
				APPROX_TYPE.valueOf( args[11] ),
				BACKUP_TYPE.valueOf( args[12] ), 
				Double.parseDouble( args[13] ),
				Integer.parseInt( args[14] ),
				Long.parseLong( args[15] ),
				INITIAL_STATE_CONF.valueOf( args[16] ),
				Double.valueOf( args[17] ),
				BACKUP_TYPE.valueOf( args[ 18 ] ),
				Integer.parseInt( args[ 19 ] ) , 
				GENERALIZE_PATH.valueOf( args[ 20 ] ),
				Integer.parseInt( args[ 21 ] ) , 
				Integer.parseInt( args[ 22 ] ) );

		Thread t = new Thread( worker );
		t.start();
		t.join();
	}

}