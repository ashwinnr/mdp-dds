package mdp.solve.solver;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.Random;

import mdp.define.PolicyStatistics;
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
import dd.DDManager.DDQuantify;
import dtr.add.ADDDecisionTheoreticRegression;
import dtr.add.ADDDecisionTheoreticRegression.BACKUP_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.GENERALIZE_PATH;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import dtr.add.ADDPolicy;
import dtr.add.ADDValueFunction;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;

public class SymbolicRTDP implements Runnable {

	private boolean CONSTRAIN_NAIVELY = false;
	private static double	EPSILON	= 0;
	private ADDDecisionTheoreticRegression _dtr;
	private ADDManager _manager;
	private Timer _cptTimer;
	private Timer _solutionTimer;
	private Timer _DPTimer = null;
	private int HORIZON;
	private double DISCOUNT;
	private int _nStates;
	private int _nRounds;
	private boolean _useDiscounting;
	private boolean _FAR;
	private APPROX_TYPE apricodd_type;
	private double apricodd_epsilon;
	private boolean do_apricodd;
	private BACKUP_TYPE heuristic_type;
	private double time_heuristic_mins;
	private int steps_heuristic;
	private long MB;
	private RDDL2ADD _mdp;
	private INITIAL_STATE_CONF init_state_conf;
	private double init_state_prob;
	private BACKUP_TYPE dp_type;
	private long seed;
	private ADDRNode __initial_state_add;
	private int nTrials;
	private RDDLFactoredStateSpace _stateSpace;
	private RDDLFactoredTransition _transition;
	private RDDLFactoredReward _reward;
	private GENERALIZE_PATH _genRule;
	private Random _rand;

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
			final GENERALIZE_PATH rule ) {
		_rand = new Random( seed );
		_genRule = rule;
		this.nTrials = nTrials;
		_FAR = FAR;
		EPSILON = epsilon;
		_cptTimer = new Timer();
		_mdp = new RDDL2ADD(domain, instance, _FAR, debug, order, true, seed);
		_cptTimer.StopTimer();
		_nStates = numStates;
		_nRounds = numRounds;
		_useDiscounting = useDiscounting;
		_dtr = new ADDDecisionTheoreticRegression(_mdp, seed);
		_manager = _mdp.getManager();
		DISCOUNT = _mdp.getDiscount();
		HORIZON = _mdp.getHorizon();
		CONSTRAIN_NAIVELY = constrain_naively;
		this.do_apricodd = do_apricodd;
		this.apricodd_epsilon = apricodd_epsilon;
		this.apricodd_type = apricodd_type;
		this.heuristic_type = heuristic_type;
		this.time_heuristic_mins = time_heuristic_mins;
		this.steps_heuristic = steps_heuristic;
		this.MB = MB;
		this.init_state_conf = init_state_conf;
		this.init_state_prob = init_state_prob;
		this.dp_type = dp_type;
		this.seed = seed;
	}

	public void run() {
		System.out.println("CPT time: " + _cptTimer.GetTimeSoFarAndResetInMinutes() );
		
		final ADDRNode heuristic = _dtr.computeLAOHeuristic( steps_heuristic, heuristic_type, CONSTRAIN_NAIVELY,
				do_apricodd, apricodd_epsilon, apricodd_type, MB, time_heuristic_mins );

		__initial_state_add = _dtr.getIIDInitialStates( init_state_conf ,
				init_state_prob );
		_stateSpace = _mdp.getFactoredStateSpace(); 
		_transition = _mdp.getFactoredTransition();
		_reward = _mdp.getFactoredReward();
		int states_to_go  = _nStates;
		final PolicyStatistics stats = new PolicyStatistics(_nStates, _nRounds);
		ADDRNode value_fn = heuristic;
		ADDRNode policy = _dtr.getNoOpPolicy( _mdp.get_actionVars(), _manager );
		_solutionTimer = new Timer();
		_DPTimer = new Timer();
		
		while( states_to_go --> 0 ){
			final FactoredState<RDDLFactoredStateSpace> init_state 
			= _transition.sampleState(__initial_state_add);
			int rounds_to_go = _nRounds;
			
			while( rounds_to_go --> 0 ){
				int horizon_to_go = HORIZON;
				FactoredState<RDDLFactoredStateSpace> cur_state = init_state;
				FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>
				cur_action = new FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>( null );
				double round_reward = 0;
				double cur_disc = 1;
				Timer roundTimer = new Timer();
				roundTimer.ResetTimer();
				
				while( horizon_to_go --> 0 ){
					System.out.println( "State : " + cur_state.toString() ); 
					_solutionTimer.ResetTimer();
					
					UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> result 
					= do_sRTDP( value_fn, policy, cur_state );
					value_fn = result._o1;
					policy = result._o2._o1;
					_solutionTimer.StopTimer();
					display( result );

					final ADDRNode action_dd 
					= _manager.restrict(result._o2._o1, cur_state.getFactoredState() );
					final NavigableMap<String, Boolean> action 
					= _manager.sampleOneLeaf(action_dd, _rand );
					cur_action.setFactoredAction( action );
					System.out.println("Action " + cur_action.toString() );

					final FactoredState<RDDLFactoredStateSpace> next_state 
						= _transition.sampleFactored(cur_state, cur_action);
					round_reward += cur_disc * _reward.sample( cur_state, cur_action );
					cur_state = next_state;
					System.out.println( "Horizon to go " + horizon_to_go );
					cur_disc = ( _useDiscounting ) ? cur_disc * DISCOUNT : cur_disc;
				}
				System.out.println("Rounds to go " + rounds_to_go );
				System.out.println("Round time : " + roundTimer.GetTimeSoFarAndResetInMinutes() );
				System.out.println("Round reward : " + round_reward );
				stats.addRoundStats(round_reward);
			}
			System.out.println("States to go " + states_to_go );
		}
		stats.printStats();
	}

	//	private void executePolicy( final UnorderedPair<ADDRNode, 
	//			ADDRNode > result , 
	//			final FactoredState<RDDLFactoredStateSpace> init_state ,
	//			final PolicyStatistics stats ){
	//		final ADDPolicy policy = new ADDPolicy(_manager, _stateSpace, _transition, _reward, _rand.nextLong());
	//		policy._addPolicy = _FAR ? null : result._o2;
	//		policy._bddPolicy = _FAR ? result._o2 : null;
	//		policy.runRounds(init_state, _nRounds, _useDiscounting, HORIZON, DISCOUNT, null, stats);
	//	}

	private void display(
			final UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> result ) {
		ADDRNode vfn = result._o1;
		ADDRNode plcy = result._o2._o1;
		System.out.println("Size of Value fn. " + _manager.countNodes(vfn) );
		System.out.println("Size of policy " + _manager.countNodes( plcy ) );
		System.out.println("Action time: " + _solutionTimer.GetElapsedTimeInMinutes() );
		System.out.println("DP time: " + _DPTimer.GetElapsedTimeInMinutes() );
	}

	private UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> do_sRTDP(
			final ADDRNode current_value_fn,
			final ADDRNode current_policy,
			final FactoredState<RDDLFactoredStateSpace> init_state ) {
		ADDRNode value_fn = current_value_fn;
		ADDRNode policy = current_policy;
		UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> backed = null;
		int trials_to_go = nTrials;
		_DPTimer.ResetTimer();
		
		while( trials_to_go --> 0 ){
			FactoredState<RDDLFactoredStateSpace> cur_state = init_state;
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> cur_action
			= new FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>(null);
			int steps_to_go = HORIZON;
			System.out.println("value of init state : " + _manager.evaluate(value_fn, init_state.getFactoredState() ).getNode().toString() );
			while( steps_to_go --> 0 ){
//				System.out.println( cur_state.toString() );
				//				System.out.println(_manager.evaluate(value_fn, cur_state.getFactoredState() ).getNode().toString() );

				final ADDRNode abstract_state 
				= _dtr.generalize(value_fn, cur_state.getFactoredState(), _genRule );
				final ADDRNode next_states 
				= _dtr.BDDImage(abstract_state, _FAR, DDQuantify.EXISTENTIAL);
				System.out.println("Updating " + _manager.countNodes(abstract_state, next_states) );
				_DPTimer.ResumeTimer();
				backed = _dtr.backup(value_fn, policy, next_states, abstract_state, 
						dp_type, do_apricodd, apricodd_epsilon, apricodd_type, 
						true, MB );
				_DPTimer.PauseTimer();
				value_fn = backed._o1;
				policy = backed._o2._o1;
				//				System.out.println("Residual " + backed._o2._o2 );

				final ADDRNode action_dd = _manager.restrict(policy, cur_state.getFactoredState() );
				final NavigableMap<String, Boolean> action = _manager.sampleOneLeaf(action_dd, _rand );
				cur_action.setFactoredAction( action );
				cur_state = _transition.sampleFactored(cur_state, cur_action);
				System.out.println( "Steps to go " + steps_to_go );
//				System.out.println( cur_action.toString() );
			}
			System.out.print( "Trials to go  " + trials_to_go );
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
				GENERALIZE_PATH.valueOf( args[ 20 ] ) );

		Thread t = new Thread( worker );
		t.start();
		t.join();
	}
}