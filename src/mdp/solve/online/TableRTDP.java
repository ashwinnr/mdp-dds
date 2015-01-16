package mdp.solve.online;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import add.ADDRNode;
import dd.DDManager.APPROX_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;
import mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH;
import mdp.solve.online.thts.SymbolicRTDP;
import mdp.solve.online.thts.THTS;
import mdp.solve.solver.HandCodedPolicies;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.viz.StateViz;
import util.InstantiateArgs;
import util.Timer;

public class TableRTDP extends RDDLOnlineActor 
implements THTS< RDDLFactoredStateSpace, RDDLFactoredActionSpace >{
	
	private FactoredState[] trajectory_states;
	private FactoredAction[] trajectory_actions;
	public final static boolean  DISPLAY_TRAJECTORY = false;
	
	Map<NavigableMap<String, Boolean>, Double>[] value_fns;
	Map<NavigableMap<String, Boolean>, NavigableMap<String, Boolean>>[]  policies;
	
	private int steps_lookahead;
	protected int nTrials;
	protected double timeOutMins;
	
	private Random _stateSelectionRand;
	private Random _actionSelectionRandom;
	private ADDRNode _baseLinePolicy;
	private FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> cur_action
		= new FactoredAction<>();
	private List<NavigableMap<String, Boolean>> _all_actions;
	private Random _rewardRandom;
	
	public TableRTDP(
			String domain,
			String instance,
			DEBUG_LEVEL debug,
			ORDER order,
			boolean useDiscounting,
			int numStates,
			int numRounds,
			INITIAL_STATE_CONF init_state_conf,
			double init_state_prob,
			final int nTrials,
			final double timeOutMins, 
			int steps_lookahead,
			final Random topLevel, StateViz viz  ) {
		super(domain, instance, true, debug, order, new Random( topLevel.nextLong() ).nextLong(),
				useDiscounting, numStates, numRounds, 
				init_state_conf, init_state_prob, viz );
		this.steps_lookahead = steps_lookahead;
		this.nTrials = nTrials;
		this.timeOutMins = timeOutMins;
		
		throwAwayEverything();

		_actionSelectionRandom = new Random( topLevel.nextLong() );
		_stateSelectionRand = new Random( topLevel.nextLong() );
		_rewardRandom = new Random( topLevel.nextLong() );
		
		_baseLinePolicy = HandCodedPolicies.get(domain, _dtr, _manager, _mdp.get_actionVars() );

		_all_actions = _mdp.getFullRegressionOrder();
		
	}


	@Override
	public boolean is_node_visited(FactoredState<RDDLFactoredStateSpace> state,
			int depth) {
		final NavigableMap<String, Boolean> state_assign = state.getFactoredState();
		final Map<NavigableMap<String, Boolean>, Double> table = value_fns[depth];
		return table.containsKey(state_assign);
	}


	@Override
	public boolean is_node_solved(FactoredState<RDDLFactoredStateSpace> state,
			int depth) {
		try{
			throw new UnsupportedOperationException("not yet implemented labelling.");
		}catch( Exception e ){
			e.printStackTrace();
			System.exit(1);
		}
		return false;
	}


	@Override
	public FactoredState<RDDLFactoredStateSpace> pick_successor_node(
			FactoredState<RDDLFactoredStateSpace> state,
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
			int depth) {
		return _transition.sampleFactored(state, action, _stateSelectionRand );
	}


	@Override
	public FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> pick_successor_node(
			FactoredState<RDDLFactoredStateSpace> state, int depth) {
		NavigableMap<String, Boolean> state_assign = state.getFactoredState();
		Map<NavigableMap<String, Boolean>, NavigableMap<String, Boolean>> table = policies[depth];
		NavigableMap<String, Boolean> act = table.get( state_assign );
		if( act == null ){
			cur_action.setFactoredAction( _manager.sampleOneLeaf(_baseLinePolicy, _actionSelectionRandom) );
		}else{
			cur_action.setFactoredAction( act );
		}
		return cur_action;
	}


	@Override
	public void visit_node(FactoredState<RDDLFactoredStateSpace> state,
			int depth) {
		try{
			throw new UnsupportedOperationException("does not make sense for flat states.");
		}catch( Exception e ){
			e.printStackTrace();
			System.exit(1);
		}		
	}


	@Override
	public FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> act(
			FactoredState<RDDLFactoredStateSpace> init_state) {
		
		int trials_to_go = nTrials;
		boolean timeOut = false;
		final Timer act_time = new Timer();
		int numSamples = 0;
		
		while( trials_to_go --> 0 && ( (timeOutMins == -1) || !timeOut ) ){
			FactoredState<RDDLFactoredStateSpace> cur_state = init_state;
			int num_actions = 0;
			
			trajectory_states[ num_actions ].setFactoredState( cur_state.getFactoredState() );
//			if( truncateTrials && !is_node_visited(cur_state, num_actions)   ){
////				initilialize_node(cur_state, num_actions);
////				visit_node(cur_state, num_actions);
////				System.out.println("Truncating trial : " + num_actions );
////				System.out.println("Truncating trial : " + cur_state );
//				continue;
//			}
			
			
			while( num_actions < steps_lookahead-1 ){
				if( DISPLAY_TRAJECTORY ){
					System.out.println( cur_state.toString() );	
				}
				
				//				System.out.println(_manager.evaluate(value_fn, cur_state.getFactoredState() ).getNode().toString() );
			    
//				System.out.println( cur_state.toString() );

//				if( enableLabelling && is_node_solved(cur_state, num_actions) ){
//					break;
//				}

				trajectory_actions[ num_actions ].setFactoredAction( 
						pick_successor_node(cur_state, num_actions).getFactoredAction() );

				if( DISPLAY_TRAJECTORY ){
					System.out.println( cur_action.toString() );	
				}

				final FactoredState<RDDLFactoredStateSpace> next_state 
				= pick_successor_node(cur_state, trajectory_actions[ num_actions ], num_actions);
				cur_state = next_state;
				//				System.out.println( "Steps to go " + steps_to_go );
				++num_actions;
				trajectory_states[ num_actions ].setFactoredState( cur_state.getFactoredState() );
				
			}
			if( DISPLAY_TRAJECTORY ){
				System.out.println( cur_state.toString() );
			}
			
			final FactoredState<RDDLFactoredStateSpace>[] trajectory_states_non_null 
				=  Arrays.copyOfRange(trajectory_states, 0, num_actions+1);
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>[] 
					trajectory_actions_non_null =  Arrays.copyOfRange(trajectory_actions, 0, num_actions);
			
			update_trajectory( trajectory_states_non_null, trajectory_actions_non_null  );
			
			System.out.print("*");		
			++numSamples;

			act_time.PauseTimer();
			if( act_time.GetElapsedTimeInMinutes() >= timeOutMins ){
				timeOut = true;
			}
			
			if( trials_to_go % 100 == 0 ){
				
				FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> 
					root_action = pick_successor_node(init_state, 0);
				
				System.out.println( "\nValue of init state " + 
						value_fns[0].get( init_state.getFactoredState() ).toString() );
				
				System.out.println("root node action : " + root_action.toString() );
				
				System.out.print("description of value of init state ");
				
			}
			
			if( !timeOut ){
				act_time.ResumeTimer();
			}
			
		}
		System.out.println();
		System.out.println("num samples : " + numSamples );
		
		final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> 
		root_action = pick_successor_node(init_state, 0);
		
		System.out.println( "Value of init state " + 
				value_fns[0].get( init_state.getFactoredState() ).toString() );
		
		System.out.println("root node action : " + root_action.toString() );
		
		System.out.print("description of value of init state ");
	
		throwAwayEverything();
		
		return root_action;

	}


	private void throwAwayEverything() {
		value_fns = (Map<NavigableMap<String, Boolean>, Double>[]) Array.newInstance( HashMap.class, steps_lookahead );
		policies = (Map<NavigableMap<String, Boolean>, NavigableMap<String, Boolean> >[]) Array.newInstance( HashMap.class, steps_lookahead );
		for( int i = 0 ; i < steps_lookahead; ++i ){
			value_fns[i] = null;
			value_fns[i] = new HashMap< >();
			policies[i] = null; 
			policies[i] = new HashMap< >();
		}
		
		trajectory_states = new FactoredState[ steps_lookahead ];
		trajectory_actions = new FactoredAction[ steps_lookahead - 1 ];
		for( int i = 0 ; i < steps_lookahead-1; ++i ){
		    trajectory_actions[i] = null;
		    trajectory_actions[i] = new FactoredAction( );
		    trajectory_states[i] = null;
		    trajectory_states[i] = new FactoredState( );
		}
		trajectory_states[ steps_lookahead - 1 ] = null;
		trajectory_states[ steps_lookahead - 1 ] = new FactoredState();
	}


	private void update_trajectory(
			final FactoredState<RDDLFactoredStateSpace>[] trajectory_states_non_null,
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>[] trajectory_actions_non_null ) {

		//update backwards
		// unlike the symbolic case, there are too many states for the last level
		//let us backup this way : 
		//iterate states in next level
		//keep track of total prob mass
		//if you run out of states, take Rmax for missing mass
		for( int  i = trajectory_states_non_null.length-1 ; i >= 0 ; --i ){
			
			FactoredState<RDDLFactoredStateSpace> this_state = trajectory_states_non_null[i];
			NavigableMap<String, Boolean> this_state_assign = this_state.getFactoredState();
					
			if( i == trajectory_states_non_null.length-1 ){
				Map<NavigableMap<String, Boolean>, Double> table = value_fns[i];
				if( !table.containsKey(this_state_assign ) ){
					table.put(this_state_assign, _mdp.getReward(this_state_assign) );
				}
			}else{
				double new_val = Double.NEGATIVE_INFINITY;
				NavigableMap<String, Boolean> argmax = null;
				
				for( NavigableMap<String, Boolean> action : _all_actions ){
					double action_val = 0;
					double seen_prob = 0d;
					Map<NavigableMap<String, Boolean>, Double> next_table = value_fns[i+1];
					for( NavigableMap<String, Boolean> possible_next_state : next_table.keySet() ){
						double this_prob = _transition.getTransitionProb( 
								this_state_assign, action, possible_next_state );
						if( this_prob > 0d ){
							seen_prob += this_prob;
							action_val += this_prob*next_table.get( possible_next_state );
						}
						if( seen_prob == 1.0d ){
							break;
						}
					}
					action_val += (1.0d-seen_prob)*( _mdp.getVMax(i, steps_lookahead).getMax() );
					action_val *= _mdp.getDiscount();
					action_val += _reward.sample(this_state_assign, action, _rewardRandom );
				
					if( new_val < action_val ){
						argmax = action;
					}
					new_val = Math.max( new_val, action_val );
				}
				Map<NavigableMap<String, Boolean>, Double> this_table = value_fns[i];
				this_table.put( this_state_assign, new_val );
				
				Map<NavigableMap<String, Boolean>, NavigableMap<String, Boolean>> 
					policy_table = policies[i];
				policy_table.put( this_state_assign, argmax );
				
			}
		}
		
	}
	
	public static void main(String[] args) throws InterruptedException {

		System.out.println( Arrays.toString(args) );

		TableRTDP rtdp = TableRTDP.instantiateMe(args);
		
		Thread t = new Thread( rtdp );
		t.start();
		t.join();
	}


	private static TableRTDP instantiateMe(String[] args) {
		
		final Options options = InstantiateArgs.createOptions();
		
		try{
		
		final CommandLine cmd = InstantiateArgs.parseOptions(args, options);
		final Random topLevel = new Random( Long.parseLong( cmd.getOptionValue("seed") ) );
		
		final double timeOut = Double.parseDouble( cmd.getOptionValue("timeOutMins") );
		System.out.println("Timeout " + timeOut );
		
		return new TableRTDP(
				cmd.getOptionValue("domain"), cmd.getOptionValue("instance"),
				DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, 
				Boolean.parseBoolean( cmd.getOptionValue( "discounting" ) ), 
				Integer.parseInt( cmd.getOptionValue("testStates") ), 
				Integer.parseInt( cmd.getOptionValue( "testRounds" ) ),
				INITIAL_STATE_CONF.valueOf( cmd.getOptionValue("initialStateConf") ),
				Double.parseDouble( cmd.getOptionValue("initialStateProb") ),
				Integer.parseInt( cmd.getOptionValue("numTrajectories") ),
				timeOut,
				Integer.parseInt( cmd.getOptionValue("stepsLookahead") ),
				new Random( topLevel.nextLong() ) , null );
		
		}catch( Exception e ){
			HelpFormatter help = new HelpFormatter();
			help.printHelp("symbolicRTDP", options);
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}


	@Override
	public void initialize_node(FactoredState<RDDLFactoredStateSpace> state,
			int depth) {
		return;
	}


}
