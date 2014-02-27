package mdp.solve.solver;

import java.util.NavigableMap;

import add.ADDRNode;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import util.UnorderedPair;
import dd.DDManager.APPROX_TYPE;
import dd.DDManager.DDOper;
import dtr.add.ADDDecisionTheoreticRegression.BACKUP_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.GENERALIZE_PATH;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;

//TODO : the action at level 1 should be fixed after depth 1 search ?
//TODO :  multiple ADDs each with more or less error ?
public class IDsRTDP extends SymbolicRTDP {

	public IDsRTDP(String domain, String instance, double epsilon,
			DEBUG_LEVEL debug, ORDER order, long seed, boolean useDiscounting,
			int numStates, int numRounds, boolean FAR,
			boolean constrain_naively, boolean do_apricodd,
			double apricodd_epsilon, APPROX_TYPE apricodd_type,
			BACKUP_TYPE heuristic_type, double time_heuristic_mins,
			int steps_heuristic, long MB, INITIAL_STATE_CONF init_state_conf,
			double init_state_prob, BACKUP_TYPE dp_type, int nTrials,
			GENERALIZE_PATH rule, int steps_dp, int steps_lookahead) {
		super(domain, instance, epsilon, debug, order, seed, useDiscounting, numStates,
				numRounds, FAR, constrain_naively, do_apricodd, apricodd_epsilon,
				apricodd_type, heuristic_type, time_heuristic_mins, steps_heuristic,
				MB, init_state_conf, init_state_prob, dp_type, nTrials, rule, steps_dp,
				steps_lookahead);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> do_sRTDP(
			final ADDRNode current_value_fn,
			final ADDRNode current_policy,
			final FactoredState<RDDLFactoredStateSpace> init_state ) {
		ADDRNode value_fn = current_value_fn;
		ADDRNode policy = current_policy;
		UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> backed = null;
		_DPTimer.ResetTimer();
		
		for( int depth = 1; depth <= steps_lookahead; ++depth ){
			System.out.println("Depth : " + depth );
			int trials_to_go = nTrials;
			System.out.println("action : " + 
					_manager.sampleOneLeaf( 
							_manager.restrict(policy, init_state.getFactoredState() ) , _rand ) );
			while( trials_to_go --> 0 ){
				FactoredState<RDDLFactoredStateSpace> cur_state = init_state;
				FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> cur_action
				= new FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>(null);
				System.out.println("value of init state : " + _manager.evaluate(value_fn, init_state.getFactoredState() ).getNode().toString() );
				ADDRNode trajectory_states = _manager.DD_NEG_INF;
				int steps_to_go = depth;
				while( steps_to_go --> 0 ){
					final NavigableMap<String, Boolean> state_assign = cur_state.getFactoredState();
//					System.out.println( cur_state.toString() );
					//				System.out.println(_manager.evaluate(value_fn, cur_state.getFactoredState() ).getNode().toString() );
					final ADDRNode this_state = _manager.getSumNegInfDDFromAssignment( state_assign );
					trajectory_states = _manager.apply( trajectory_states, this_state, DDOper.ARITH_MAX );
					
					final ADDRNode action_dd = _manager.restrict(policy, cur_state.getFactoredState() );
					final NavigableMap<String, Boolean> action = _manager.sampleOneLeaf(action_dd, _rand );
					cur_action.setFactoredAction( action );
					cur_state = _transition.sampleFactored(cur_state, cur_action);
//					System.out.println( "Steps to go " + steps_to_go );
//					System.out.println( cur_action.toString() );
					System.out.print("*");
				}
				_DPTimer.ResumeTimer();			
				backed = update_trajectory( trajectory_states, value_fn, policy );
				_DPTimer.PauseTimer();
				
				value_fn = backed._o1;
				policy = backed._o2._o1;
				
				System.out.println("Trials to go  " + trials_to_go );
			}
		}
		return backed;
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
