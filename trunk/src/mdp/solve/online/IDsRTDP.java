package mdp.solve.online;

import java.util.Arrays;
import java.util.List;
import java.util.NavigableMap;
import java.util.Stack;


import add.ADDManager;
import add.ADDRNode;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import util.Timer;
import util.UnorderedPair;
import dd.DDManager.APPROX_TYPE;
import dd.DDManager.DDOper;
import dd.DDManager.DDQuantify;
import dtr.add.ADDDecisionTheoreticRegression;
import dtr.add.ADDDecisionTheoreticRegression.BACKUP_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.GENERALIZE_PATH;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;
//This class : reverse iterative deepening RTDP
//in order to act( s, k )
//
// act(s, 1) => V1
//NEW : initialize V_i+1 to V_i for all states before act(s, i) - still admissible
//repeat till i = steps_lookahead
public class IDsRTDP extends RDDLOnlineActor {
	
	protected ADDRNode[] value_fns;
	protected ADDRNode[] policy;
	private double EPSILON;
	private int steps_dp;
	private int steps_lookahead;
	private GENERALIZE_PATH _genRule;
	private int nTrials;
	private ADDManager _manager;
	private boolean CONSTRAIN_NAIVELY;
	private boolean do_apricodd;
	private double apricodd_epsilon;
	private double time_heuristic_mins;
	private APPROX_TYPE apricodd_type;
	private BACKUP_TYPE heuristic_type;
	private int steps_heuristic;
	private long MB;
	private BACKUP_TYPE dp_type;
	private Timer _DPTimer;
	private ADDRNode Rmax;
	private ADDRNode _noOpPolicy;
	private UnorderedPair<ADDRNode, ADDRNode> heuristic;

	public IDsRTDP(String domain, String instance, double epsilon,
			DEBUG_LEVEL debug, ORDER order, long seed, boolean useDiscounting,
			int numStates, int numRounds, boolean FAR,
			boolean constrain_naively, boolean do_apricodd,
			double apricodd_epsilon, APPROX_TYPE apricodd_type,
			BACKUP_TYPE heuristic_type, double time_heuristic_mins,
			int steps_heuristic, long MB, INITIAL_STATE_CONF init_state_conf,
			double init_state_prob, BACKUP_TYPE dp_type, int nTrials,
			GENERALIZE_PATH rule, int steps_dp, int steps_lookahead) {
		super(domain, instance, FAR, debug, order, seed, useDiscounting, numStates,
				numRounds, init_state_conf, init_state_prob );
		value_fns = new ADDRNode[ steps_lookahead+1 ];//V0 = R. 
		policy = new ADDRNode[ steps_lookahead+1 ];
		EPSILON = epsilon;
		this.steps_dp = steps_dp;
		this.steps_lookahead = steps_lookahead;
		_genRule = rule;
		this.nTrials = nTrials;
		_manager = _mdp.getManager();
		CONSTRAIN_NAIVELY = constrain_naively;
		this.do_apricodd = do_apricodd;
		this.apricodd_epsilon = apricodd_epsilon;
		this.apricodd_type = apricodd_type;
		this.heuristic_type = heuristic_type;
		this.time_heuristic_mins = time_heuristic_mins;
		this.steps_heuristic = steps_heuristic;
		this.MB = MB;
		this.dp_type = dp_type;
		_DPTimer = new Timer();
		_DPTimer.PauseTimer();
		//initialize V0 to R
		final UnorderedPair<ADDRNode, ADDRNode> init = _dtr.computeLAOHeuristic(steps_heuristic, heuristic_type, 
				constrain_naively, do_apricodd, apricodd_epsilon, apricodd_type, MB, time_heuristic_mins);
		value_fns[0] = init._o1;
		policy[0] = init._o2;
		heuristic = init;
		this.Rmax = _mdp.getRMax();
	}
	
	@Override
	public FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> act(
			FactoredState<RDDLFactoredStateSpace> state) {
		int steps_to_go = 1;
		while( steps_to_go <= steps_lookahead ){
			System.out.println("depth iter : " + steps_to_go );
			if( value_fns[ steps_to_go ] == null ){
				//INITIALIZE TO previous + Rmax ? - admissible
				value_fns[ steps_to_go ] =
						_manager.apply( this.Rmax ,
						_manager.scalarMultiply( value_fns[ steps_to_go-1 ] , DISCOUNT ),
						DDOper.ARITH_PLUS );
				policy[ steps_to_go ] = policy[ steps_to_go-1 ];
			}
//			System.out.println("value of init state : " + 
//					_manager.evaluate(value_fns[steps_to_go], state.getFactoredState() ).getNode().toString() );
			
			do_sRTDP( state, steps_to_go );//update V(1:steps)
			
//			System.out.println("value of init state : " + 
//					_manager.evaluate(value_fns[steps_to_go], state.getFactoredState() ).getNode().toString() );
			
			
			steps_to_go = steps_to_go + 1 ;
		}
		final ADDRNode action_dd 
			= _manager.restrict(policy[steps_lookahead],  state.getFactoredState() );
		final NavigableMap<String, Boolean> action 
			= ADDManager.sampleOneLeaf(action_dd, _rand );
		_manager.flushCaches();
		
		display();
//		value_fns = new ADDRNode[HORIZON];
//		policy = new ADDRNode[HORIZON];
//		value_fns[0] = heuristic._o1;
//		policy[0] = heuristic._o2;
		
		return new FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>(action);
	}
	
	public void display(){
		System.out.println("Value function leaves ");
		System.out.println( Arrays.toString( _manager.countLeaves(value_fns) ) );
		System.out.println("Policy leaves ");
		System.out.println( Arrays.toString( _manager.countLeaves(policy) ) );
	
		System.out.println("Value function nodes ");
		System.out.println( _manager.countNodes(value_fns) );
		System.out.println("Policy nodes ");
		System.out.println( _manager.countNodes(policy) ) ;
	}

	protected void do_sRTDP(
			FactoredState<RDDLFactoredStateSpace> state,
			final int effective_horizon //update V(1:steps)
			) {
		_DPTimer.ResetTimer();
		FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> cur_action
		= new FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>(null);
		
		int trials_to_go = nTrials;
		while( trials_to_go --> 0 ){
			FactoredState<RDDLFactoredStateSpace> cur_state = state;
			
			Stack<NavigableMap<String, Boolean>> trajectory_states 
				= new Stack<NavigableMap<String, Boolean>>();
			
			int steps_to_go = effective_horizon;
			while( steps_to_go > 0 ){
				final NavigableMap<String, Boolean> state_assign = cur_state.getFactoredState();
				trajectory_states.push( state_assign );
//				System.out.println( cur_state.toString() );
					//				System.out.println(_manager.evaluate(value_fn, cur_state.getFactoredState() ).getNode().toString() );
//				final ADDRNode this_state = _manager.getSumNegInfDDFromAssignment( state_assign );
//				trajectory_states = _manager.apply( trajectory_states, this_state, DDOper.ARITH_MAX );
					
				final ADDRNode action_dd = _manager.restrict(
						policy[ steps_to_go ] , state_assign );
				final NavigableMap<String, Boolean> action = ADDManager.sampleOneLeaf(action_dd, _rand );
				cur_action.setFactoredAction( action );
				cur_state = _transition.sampleFactored(cur_state, cur_action);
//					System.out.println( "Steps to go " + steps_to_go );
//				System.out.println( cur_action.toString() );
//				System.out.print("*");
				steps_to_go = steps_to_go - 1;
			}
			_DPTimer.ResumeTimer();			
			update_trajectory( trajectory_states, effective_horizon );
			_DPTimer.PauseTimer();
//			System.out.print("*");
//			System.out.println("Trials to go  " + trials_to_go );
		}
//		System.out.println();
	}
	
	private void update_trajectory(
			final Stack<NavigableMap<String, Boolean>> trajectory_states,
			final int effective_horizon) {
//		System.out.println("updating " +  effective_horizon );
		for( int index = 1 ; index <= effective_horizon ; ++index ){
			final NavigableMap<String, Boolean> this_state = trajectory_states.pop();
			
			final ADDRNode this_vfn_src = value_fns[ index-1 ] ;
			final ADDRNode this_policy_src = policy[ index-1 ];
			
			final double this_error =  apricodd_epsilon/Math.pow(DISCOUNT, steps_lookahead-index);
			
			final ADDRNode this_gen_state = _dtr.generalize( value_fns[ index ], this_state, _genRule);
			final ADDRNode this_gen_state_neg = _manager.BDDNegate(this_gen_state);
			
			final ADDRNode next_states = _dtr.BDDImage(this_gen_state, _actionVars, DDQuantify.EXISTENTIAL);
			final UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> one_backup 
				= _dtr.backup(this_vfn, this_policy, next_states, this_gen_state, dp_type, 
					do_apricodd, apricodd_epsilon/Math.pow(EPSILON, steps_lookahead-index), apricodd_type, true, MB,
					CONSTRAIN_NAIVELY );
//			System.out.println( one_backup._o2._o2 );
			value_fns[ index ] = one_backup._o1;
			policy[ index ] = one_backup._o2._o1;
			_manager.flushCaches();
//			System.out.println( index );
		}
	}

	public static void main(String[] args) throws InterruptedException {

		final boolean use_disc = Boolean.parseBoolean( args[4] );
		final int nStates = Integer.parseInt( args[5] );
		final int nRounds = Integer.parseInt( args[6] );

		Runnable worker = new IDsRTDP(
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
