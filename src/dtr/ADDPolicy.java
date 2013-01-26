package dtr;

import java.util.Collection;
import java.util.NavigableMap;
import java.util.Random;

import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredReward;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.mdp.RDDLFactoredTransition;

import mdp.define.Action;
import mdp.define.PolicyStatistics;
import mdp.define.State;

import add.ADDINode;
import add.ADDLeaf;
import add.ADDManager;
import add.ADDNode;
import add.ADDRNode;

import dd.DDINode;
import dd.DDLeaf;
import dd.DDManager;
import dd.DDManager.DDOper;
import dd.DDNode;
import dd.DDRNode;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredPolicy;
import factored.mdp.define.FactoredRandomStateGenerator;
import factored.mdp.define.FactoredReward;
import factored.mdp.define.FactoredState;
import factored.mdp.define.FactoredStateSpace;
import factored.mdp.define.FactoredTransition;

public class ADDPolicy extends
	SymbolicPolicy<ADDNode, ADDRNode, ADDINode, ADDLeaf, RDDLFactoredStateSpace,
		RDDLFactoredActionSpace>{

	private static final boolean DISPLAY = false;
	protected ADDManager _manager;
	private FactoredTransition<RDDLFactoredStateSpace, RDDLFactoredActionSpace> _transition;
	private FactoredReward<RDDLFactoredStateSpace, RDDLFactoredActionSpace> _reward;
	private RDDLFactoredStateSpace _stateSpace;
	private Random _rand;
	
	public ADDPolicy(ADDManager man, 
			RDDLFactoredStateSpace stateSpace,
			RDDLFactoredTransition transition, 
			RDDLFactoredReward reward,
			long seed ) {
		_manager = man;
		_transition = transition;
		_reward = reward;
		_stateSpace = stateSpace;
		_rand = new Random( seed );
	}
	
	@Override
	public <T extends State<RDDLFactoredStateSpace>, U extends 
		Action<RDDLFactoredStateSpace, RDDLFactoredActionSpace>> U getAction(T state) {
		return (U)(getFactoredAction((FactoredState<RDDLFactoredStateSpace>)state));
	}

	@Override
	protected void updateADDPolicy( final ADDRNode v_func, final ADDRNode this_q, 
			final NavigableMap<String, Boolean> this_action ){
		
		int act_id;
		try{
			act_id = actToIntMap.get(this_action);
		}catch( NullPointerException e ){
			act_id = this_action.values().hashCode();
			actToIntMap.put( this_action, act_id );
			leafMapping.put( act_id, this_action );
		}
		
		if( _addPolicy == null ){
			_addPolicy = _manager.scalarMultiply( ((ADDManager)_manager).DD_ONE, 
					act_id );
		}else{
			ADDRNode diff = _manager.apply( v_func, this_q, DDOper.ARITH_MINUS );
			ADDRNode bdd_diff = _manager.threshold(diff, 0, false);//1 in places where 
			//this_action is the maximizing action
			ADDRNode this_action_policy = _manager.scalarMultiply( bdd_diff, act_id );
			ADDRNode old_policy = _manager.apply( _addPolicy, 
									_manager.apply(_manager.DD_ONE, bdd_diff, DDOper.ARITH_MINUS),
									DDOper.ARITH_PROD );
			_addPolicy = _manager.apply(this_action_policy, old_policy, DDOper.ARITH_PLUS);
		}
		
	}

	@Override
	protected void updateBDDPolicy(final ADDRNode v_func, final ADDRNode jointQFunc) {
		ADDRNode diff = _manager.apply(v_func, jointQFunc, DDOper.ARITH_MINUS );
		_bddPolicy = _manager.threshold(diff, 0, false);
//		_manager.showGraph( v_func, jointQFunc, diff, _bddPolicy );
	}
	
	@Override
	public <T extends FactoredState<RDDLFactoredStateSpace>, 
		U extends FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>> U getFactoredAction(
			T state) {
		if( _bddPolicy == null ){
			ADDRNode ret = _manager.evaluate(_addPolicy, state.getFactoredState());
			if( ret.getNode() instanceof ADDINode ){
				try{
					throw new Exception("Policy did not evaluate to leaf");
				}catch( Exception e ){
					e.printStackTrace();
				}
			}else{
				int act = (((ADDLeaf)(ret.getNode())).getLeafValues()._o1).intValue();
				return (U) new FactoredAction<RDDLFactoredStateSpace,
						RDDLFactoredActionSpace>( leafMapping.get(act) );
			}
		}else{
			ADDRNode ret = _manager.restrict(_bddPolicy, state.getFactoredState());
			NavigableMap<String, Boolean> act = _manager.findFirstOneLeafAction( ret );
			return (U) new FactoredAction<RDDLFactoredStateSpace,RDDLFactoredActionSpace>(act);
		}
		return null;
	}
	
	public PolicyStatistics executePolicy( final int numRounds, final int numStates, 
			final boolean useDiscounting, final int horizon,
			final double discount ){
		PolicyStatistics stats = new PolicyStatistics(numStates, numRounds);
		FactoredRandomStateGenerator<RDDLFactoredStateSpace> randomGen
			= new FactoredRandomStateGenerator<RDDLFactoredStateSpace>(_stateSpace, _rand.nextLong() );
		
		for( int i = 0 ; i < numStates; ++i ){
			State<RDDLFactoredStateSpace> init_state = randomGen.randomState();
			if( DISPLAY ){
				System.out.println("Random initial state : ");
				System.out.println( init_state );
			}
			for( int j = 0 ; j < numRounds; ++j ){
				State<RDDLFactoredStateSpace> current_state = init_state;
				double round_reward = 0.0d;
				double cur_discount = ( useDiscounting ? discount : 0 );
				for( int k = 0 ; k < horizon; ++k ){
					if( DISPLAY ){
						System.out.println("*State : " + current_state );
					}
					Action<RDDLFactoredStateSpace, RDDLFactoredActionSpace> act 
						= this.getAction(current_state);
					State<RDDLFactoredStateSpace> new_state 
						= _transition.sample(current_state, act);
					if( DISPLAY ){
						System.out.println("***State : " + current_state );
					}
					double reward = _reward.sample(current_state, act);
					round_reward += ( useDiscounting ? cur_discount*reward : reward );
					cur_discount = ( useDiscounting ? cur_discount*discount : 0 );
					if( DISPLAY ){
						System.out.println("State : " + current_state );
						System.out.println("Action : " + act );
						System.out.println("Reward : " + reward );
						System.out.println("Next state : " + new_state );
					}
					current_state = null;
					current_state = new_state;
					if( DISPLAY ){
						System.out.println("**State : " + current_state );
					}
				}
				stats.addRoundStats(round_reward);
			}
		}
		
		return stats;
	}

}
