package dtr.add;

import java.util.Collection;
import java.util.NavigableMap;
import java.util.Random;

import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredReward;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.mdp.RDDLFactoredTransition;
import rddl.viz.StateViz;

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
import dtr.SymbolicPolicy;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredPolicy;
import factored.mdp.define.FactoredReward;
import factored.mdp.define.FactoredState;
import factored.mdp.define.FactoredStateSpace;
import factored.mdp.define.FactoredTransition;

public class ADDPolicy extends
	SymbolicPolicy<ADDNode, ADDRNode, ADDINode, ADDLeaf, RDDLFactoredStateSpace,
		RDDLFactoredActionSpace> implements RDDLPolicy{

	private static final boolean DISPLAY = false;
	protected ADDManager _manager;
	private RDDLFactoredTransition _transition;
	private FactoredReward<RDDLFactoredStateSpace, RDDLFactoredActionSpace> _reward;
	private RDDLFactoredStateSpace _stateSpace;
	
	public ADDPolicy(ADDManager man, 
			RDDLFactoredStateSpace stateSpace,
			RDDLFactoredTransition transition, 
			RDDLFactoredReward reward  ) {
		_manager = man;
		_transition = transition;
		_reward = reward;
		_stateSpace = stateSpace;
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
//		_manager.showGraph( v_func, jointQFunc, diff );
		_bddPolicy = _manager.threshold(diff, 0, false);
		
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
				int act = ((ADDLeaf)(ret.getNode())).getLeafValues().intValue();
				return (U) new FactoredAction<RDDLFactoredStateSpace,
						RDDLFactoredActionSpace>( ).setFactoredAction( leafMapping.get(act) );
			}
		}else{
			ADDRNode ret = _manager.restrict(_bddPolicy, state.getFactoredState());
			NavigableMap<String, Boolean> act = _manager.findFirstOneLeafAction( ret );
			return (U) new FactoredAction<RDDLFactoredStateSpace,RDDLFactoredActionSpace>(  ).setFactoredAction( act );
		}
		return null;
	}
	
	public PolicyStatistics executePolicy( final int numRounds, final int numStates, 
			final boolean useDiscounting, final int horizon,
			final double discount, final StateViz visualizer ,
			final ADDRNode initial_state_dist,
			final Random initialStateRandom,
			final Random stateTransitionRandom,
			final Random rewardSampleRandom ){
		if( initial_state_dist == null ){
			return executePolicy(numRounds, numStates, useDiscounting, horizon, 
					discount, visualizer, initialStateRandom,stateTransitionRandom, 
					rewardSampleRandom );
		}
		
		PolicyStatistics stats = new PolicyStatistics(numStates, numRounds);
		
		for( int i = 0 ; i < numStates; ++i ){
			FactoredState<RDDLFactoredStateSpace> init_state 
				= _transition.sampleState( initial_state_dist , initialStateRandom, 
						_manager ) ;
			
			
			System.out.println("Initial state #" + i );
			System.out.println( init_state );
			runRounds( init_state, numRounds ,
					useDiscounting, horizon, discount, visualizer, stats,
					stateTransitionRandom, rewardSampleRandom );
		}
		
		return stats;
	}
	
	public PolicyStatistics executePolicy( final int numRounds, final int numStates, 
			final boolean useDiscounting, final int horizon,
			final double discount, final StateViz visualizer,
			final Random initialStateRandom, Random stateTransitionRandom, 
			Random rewardTransitionRandom ){
		PolicyStatistics stats = new PolicyStatistics(numStates, numRounds);
		
		for( int i = 0 ; i < numStates; ++i ){
			FactoredState<RDDLFactoredStateSpace> init_state 
				= _transition.randomState( initialStateRandom  );
			System.out.println("Initial state #" + i );
			System.out.println( init_state );
			runRounds( init_state, numRounds ,
					useDiscounting, horizon, discount, visualizer, stats, 
					stateTransitionRandom, rewardTransitionRandom );
		}
		
		return stats;
	}
	
	//WARNING : side effects - adds entry to stats
	public void  runRounds(
			final FactoredState<RDDLFactoredStateSpace> init_state,
			final int numRounds,
			final boolean useDiscounting ,
			final int horizon, 
			final double discount ,
			final StateViz visualizer,
			final PolicyStatistics stats, final Random stateTransitionRandom,
			final Random rewardSampleRandom ) {
		for( int j = 0 ; j < numRounds; ++j ){
			FactoredState<RDDLFactoredStateSpace> current_state = init_state;
			double round_reward = 0.0d;
			double cur_discount = ( useDiscounting ? discount : 0 );
			for( int k = 0 ; k < horizon; ++k ){
				if( DISPLAY ){
					System.out.println("*State : " + current_state );
				}
				
				FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> act 
					= this.getAction(current_state);

				if( visualizer != null ){
					_transition.displayState( visualizer,
							current_state, act, k );
				}
				
				FactoredState<RDDLFactoredStateSpace> new_state = null;
				try{
					new_state
						= _transition.sample(current_state, act, stateTransitionRandom );
				}catch( Exception e ){
					e.printStackTrace();
					System.exit(1);
				}
					
				if( DISPLAY ){
					System.out.println("***State : " + current_state );
				}
				double reward = _reward.sample(current_state, act, rewardSampleRandom );
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
			System.out.println( " Round reward : " + round_reward );
			stats.addRoundStats(round_reward);
		}

	}

	@Override
	public PolicyStatistics executePolicy(int numRounds, int numStates,
			boolean useDiscounting, int horizon, double discount,
			final Random initial_state_random, 
			final Random state_transition_random, 
			final Random reward_sample_random ) {
		return executePolicy(numRounds, numStates, useDiscounting, horizon, discount, null,
				initial_state_random, 
				state_transition_random, 
				reward_sample_random );
	}

}
