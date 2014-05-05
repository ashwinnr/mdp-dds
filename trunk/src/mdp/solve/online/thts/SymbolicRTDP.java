package mdp.solve.online.thts;

import java.util.Arrays;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;

import mdp.generalize.trajectory.EBLGeneralization;
import mdp.generalize.trajectory.Generalization;
import mdp.generalize.trajectory.GenericTransitionGeneralization.Consistency;
import mdp.generalize.trajectory.OptimalActionGeneralization;
import mdp.generalize.trajectory.ValueGeneralization;
import mdp.generalize.trajectory.parameters.EBLParams;
import mdp.generalize.trajectory.parameters.GeneralizationParameters;
import mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH;
import mdp.generalize.trajectory.parameters.OptimalActionParameters;
import mdp.generalize.trajectory.parameters.OptimalActionParameters.UTYPE;
import mdp.generalize.trajectory.parameters.RewardGeneralizationParameters;
import mdp.generalize.trajectory.parameters.ValueGeneralizationParameters;
import mdp.generalize.trajectory.type.GeneralizationType;
import mdp.solve.online.Exploration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import util.InstantiateArgs;
import util.UnorderedPair;
import add.ADDLeaf;
import add.ADDRNode;
import dd.DDManager.APPROX_TYPE;
import dd.DDManager.DDOper;
import dd.DDManager.DDQuantify;
import dtr.add.ADDDecisionTheoreticRegression.BACKUP_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;

public class SymbolicRTDP< T extends GeneralizationType, 
	P extends GeneralizationParameters<T> > extends SymbolicTHTS< T, P >  {


	private FactoredState[] trajectory_states;
	private FactoredAction[] trajectory_actions;
	
	public final static boolean  DISPLAY_TRAJECTORY = false;
//	public boolean BACK_CHAIN;
	private int _successful_update = 0;
	private int successful_policy_update = 0;
	private double backChainThreshold;
	private int good_updates;
	private int truncated_backup;
	private boolean _genStates;
	

	public SymbolicRTDP(
			String domain,
			String instance,
			double epsilon,
			DEBUG_LEVEL debug,
			ORDER order,
			long seed,
			boolean useDiscounting,
			int numStates,
			int numRounds,
			boolean FAR,
			boolean constrain_naively,
			boolean do_apricodd,
			double[] apricodd_epsilon,
			APPROX_TYPE apricodd_type,
			BACKUP_TYPE heuristic_type,
			double time_heuristic_mins,
			int steps_heuristic,
			long MB,
			INITIAL_STATE_CONF init_state_conf,
			double init_state_prob,
			BACKUP_TYPE dp_type,
			int nTrials,
			int steps_dp,
			int steps_lookahead,
			Generalization<RDDLFactoredStateSpace, RDDLFactoredActionSpace, T, P> generalizer,
			P generalize_parameters_wo_manager,
			boolean gen_fix_states,
			boolean gen_fix_actions,
			int gen_num_states,
			int gen_num_actions,
			GENERALIZE_PATH gen_rule,
			Exploration<RDDLFactoredStateSpace, RDDLFactoredActionSpace> exploration,
			Consistency[] cons, boolean truncateTrials, boolean enableLabelling ,
			final double backChainThreshold ) {
		super(domain, instance, epsilon, debug, order, seed, useDiscounting, numStates,
				numRounds, FAR, constrain_naively, do_apricodd, apricodd_epsilon,
				apricodd_type, heuristic_type, time_heuristic_mins, steps_heuristic,
				MB, init_state_conf, init_state_prob, dp_type, nTrials, steps_dp,
				steps_lookahead, generalizer, generalize_parameters_wo_manager,
				gen_fix_states, gen_fix_actions, gen_num_states, gen_num_actions,
				gen_rule, exploration, cons, truncateTrials, enableLabelling );
//		this.BACK_CHAIN = backChain;
		this.backChainThreshold = backChainThreshold; 
			
		trajectory_states = new FactoredState[ steps_lookahead ];
		trajectory_actions = new FactoredAction[ steps_lookahead - 1 ];
		for( int i = 0 ; i < steps_lookahead-1; ++i ){
		    trajectory_actions[i] = new FactoredAction( );
		    trajectory_states[i] = new FactoredState( );
		}
		trajectory_states[ steps_lookahead - 1 ] = new FactoredState();
		_genStates = !gen_fix_states;
		
	}

	@Override
	public FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> act(
			final FactoredState<RDDLFactoredStateSpace> state) {

		if( !_manager.evaluate(_solved[0], state.getFactoredState()).equals(_manager.DD_ONE) ){
			do_sRTDP( state );
			display(  );//_valueDD, _policyDD );
		}
		
		final ADDRNode action_dd 
			= _manager.restrict(_policyDD[0] ,  state.getFactoredState() );
		final NavigableMap<String, Boolean> action 
			= pick_successor_node(state, 0).getFactoredAction();//ADDManager.sampleOneLeaf(action_dd, _rand );
		
		_manager.flushCaches();
		
		cur_action.setFactoredAction(action);
		
		System.out.println( "#updates to value " + (double)_successful_update / nTrials );
		_successful_update = 0;
		
		System.out.println( "#updates to policy " + (double)successful_policy_update/ nTrials );
		successful_policy_update = 0;
		
		System.out.println("Heuristic sharing " + heuristic_sharing );
		heuristic_sharing = 0;
		
		System.out.println( "#Good updates " + (double)good_updates / ( steps_lookahead * (nTrials) ) );
		good_updates = 0;
		
		System.out.println( "#Truncated trajectories " + (double)truncated_backup / ( steps_lookahead * (nTrials) ) );
		truncated_backup = 0;
		
		System.out.println( "Value of init state " + 
				_manager.evaluate(_valueDD[0], state.getFactoredState() ).toString() );
			
		
		throwAwayEverything();
		
		return cur_action;
	}
	

	protected void do_sRTDP( final FactoredState<RDDLFactoredStateSpace> init_state ) {

		int trials_to_go = nTrials;
		boolean solved = false;
		
		while( trials_to_go --> 0 && !solved ){
			FactoredState<RDDLFactoredStateSpace> cur_state = init_state;
			int num_actions = 0;//steps_lookahead;
//			System.out.println("value of init state : " + _manager.evaluate(value_fn, init_state.getFactoredState() ).getNode().toString() );
			
			trajectory_states[ num_actions ].setFactoredState( cur_state.getFactoredState() );
			if( !is_node_visited(cur_state, num_actions) ){
				initilialize_node(cur_state, num_actions);
				visit_node(cur_state, num_actions);
				if( truncateTrials ){
					System.out.println("Truncating trial : " + num_actions );
					System.out.println("Truncating trial : " + cur_state );
					continue;
				}
			}
			
			double prob_traj = 1.0d;
			
			while( num_actions < steps_lookahead-1 ){
				if( DISPLAY_TRAJECTORY ){
					System.out.println( cur_state.toString() );	
				}
				
				//				System.out.println(_manager.evaluate(value_fn, cur_state.getFactoredState() ).getNode().toString() );
			    
//				System.out.println( cur_state.toString() );

				if( enableLabelling && is_node_solved(cur_state, num_actions) ){
					break;
				}

				trajectory_actions[ num_actions ].setFactoredAction( 
						pick_successor_node(cur_state, num_actions).getFactoredAction() );

				if( DISPLAY_TRAJECTORY ){
					System.out.println( cur_action.toString() );	
				}

				final FactoredState<RDDLFactoredStateSpace> next_state 
				= pick_successor_node(cur_state, cur_action, num_actions);
				prob_traj *= _dtr.get_prob_transition( cur_state, cur_action, next_state );
				cur_state = next_state;
				//				System.out.println( "Steps to go " + steps_to_go );
				++num_actions;
				trajectory_states[ num_actions ].setFactoredState( cur_state.getFactoredState() );
				if( !is_node_visited(cur_state, num_actions) ){
					initilialize_node(cur_state, num_actions);
					visit_node(cur_state, num_actions);
					if( truncateTrials ){
//						System.out.println("Truncating trial : " + num_actions );
//						System.out.println("Truncating trial : " + cur_state );
						break;
					}
				}
//				if( prob_traj < 0.01d ){
//					break;
//				}
				
//				System.out.print("-");
				
			}
			if( DISPLAY_TRAJECTORY ){
				System.out.println( cur_state.toString() );
			}
			
//			trajectory_states[ trajectory_states.length-1 ].setFactoredState( cur_state.getFactoredState() );
			
//			System.out.println("Updating : " + _manager.countPaths(trajectory_states) );
//			System.out.println("Prob. trajectory = " + prob_traj );
			
			final FactoredState<RDDLFactoredStateSpace>[] trajectory_states_non_null 
				=  Arrays.copyOfRange(trajectory_states, 0, num_actions+1);
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>[] 
					trajectory_actions_non_null =  Arrays.copyOfRange(trajectory_actions, 0, num_actions);
			
			final ADDRNode[] gen_trajectory = generalize_trajectory( trajectory_states_non_null,
					trajectory_actions_non_null );
			
//			System.out.println(gen_trajectory.length);
//			_DPTimer.ResumeTimer();			
			update_generalized_trajectory( trajectory_states_non_null, trajectory_actions_non_null, 
					gen_trajectory , num_actions+1 );
//			_DPTimer.PauseTimer();
			System.out.print("*");			
//			System.out.println("Trials to go  " + trials_to_go );
			
			solved = _manager.evaluate(_solved[0], init_state.getFactoredState()).equals(_manager.DD_ONE);
//			display();
			if( trials_to_go % 100 == 0 ){
				System.out.println( "#updates to value " + (double)_successful_update / nTrials );
				System.out.println( "#updates to policy " + (double)successful_policy_update/ nTrials );
				System.out.println( "Heuristic sharing " + this.heuristic_sharing );
				System.out.println( "#Good updates " + (double)good_updates / ( steps_lookahead * (nTrials-trials_to_go) ) );
				System.out.println( "#Truncated backups " + (double)truncated_backup / ( steps_lookahead * (nTrials-trials_to_go) ) );
				
				System.out.println( "Value of init state " + 
						_manager.evaluate(_valueDD[0], init_state.getFactoredState() ).toString() );
				System.out.println("DP time: " + _DPTimer.GetElapsedTimeInMinutes() );
//				dis
			}
		}
		System.out.println();
		
	}
	
	protected ADDRNode[] generalize_trajectory(final FactoredState<RDDLFactoredStateSpace>[] trajectory_states, 
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>[] trajectory_actions  ){

		_genaralizeParameters.set_valueDD(_valueDD);
		_genaralizeParameters.set_policyDD( _policyDD );
		_genaralizeParameters.set_visited(_visited);
		
		final P inner_params = _genaralizeParameters.getParameters();
		inner_params.set_valueDD(_valueDD);
		inner_params.set_policyDD(_policyDD);
		inner_params.set_visited(_visited);
		
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
		ADDRNode visit_save_j = null;// Arrays.copyOf(_visited, _visited.length);
			
		while( i >= 2 ){
		
//	    	System.out.println("Updating trajectories " + j );
	    	
	    	ADDRNode this_next_states, this_states, this_actions;
	    	ADDRNode source_val, target_val, target_policy;
	    	
	    	
	    	this_next_states = trajectory[i--];
			this_actions = trajectory[i--];
			this_states = trajectory[i];
		    
			target_val = _valueDD[ j-1 ];
			target_policy = _policyDD[ j-1 ];
			
			final ADDRNode next_states = _dtr.BDDImageAction(this_states, DDQuantify.EXISTENTIAL,
					this_actions, true, _actionVars );//WARNING : constant true
			//visited in next state already initialized and updated
			//others need to be initialized optimistically
			//so - initialize them with MAX pruning
			//if(x,y) visited but next_states has (x)
			//initialize (x,y') = (x,y) - but no longer admissible
			//the problem with BDD intersection is adds variables to the 
			//level of abstraction of visited
			//which will be regressed
			final ADDRNode image_not_visited = _manager.BDDIntersection(next_states, 
					_manager.BDDNegate( _visited[j] ) );//1 iff next state ^ not visited
//				_manager.constrain( 
//				_manager.BDDIntersection( next_states, 
//					_manager.BDDNegate( 
//						( BACK_CHAIN && visit_save_j  != null ) ? visit_save_j : _visited[j] ) );
			if( !image_not_visited.equals(_manager.DD_ZERO) ){
				source_val = initilialize_node_temp( _valueDD[j], image_not_visited, j);
			}else{
				source_val = _valueDD[ j ];
			}
			
			_DPTimer.ResumeTimer();
			UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> backup  
			= _dtr.backup( target_val, target_policy, source_val, next_states, this_states, dp_type, 
			do_apricodd, do_apricodd ? apricodd_epsilon[j-1] : 0 , apricodd_type, true, MB, CONSTRAIN_NAIVELY);
			_DPTimer.PauseTimer();
			
			if( enableLabelling  ){
				final ADDRNode diff = _manager.apply( target_val, backup._o1, DDOper.ARITH_MINUS );
				final ADDRNode threshed 
				= _manager.BDDIntersection( this_states, _manager.threshold( diff, EPSILON, false) );
				//remove from threshed those that were not updated
				
				//for state s, thresh = 1 ^ solved = 1 for image(s)
				final Set<NavigableMap<String, Boolean>> small_error_paths 
				= _manager.enumeratePaths(threshed, false, true, _manager.DD_ONE, false);
				if( small_error_paths.isEmpty() ){
					final NavigableMap<String, Boolean> state_path = trajectory_states[j-1].getFactoredState();
					final ADDRNode this_path 
					= _manager.getProductBDDFromAssignment(state_path);
					final ADDRNode this_path_image = _dtr.BDDImage(this_path, true, DDQuantify.EXISTENTIAL);
					final ADDRNode this_path_image_not = _manager.BDDNegate(this_path_image);
					if( _manager.BDDUnion( this_path_image_not, _solved[ j ] ).equals(_manager.DD_ONE) ){
						mark_node_solved(state_path, j-1);
					}
				}else{
					for( final NavigableMap<String, Boolean> path : small_error_paths ){
						final ADDRNode this_path = _manager.getProductBDDFromAssignment(path);
						final ADDRNode this_path_image = _dtr.BDDImage(this_path, true, DDQuantify.EXISTENTIAL);
						final ADDRNode this_path_image_not = _manager.BDDNegate(this_path_image);
						if( _manager.BDDUnion( this_path_image_not, _solved[ j ] ).equals(_manager.DD_ONE) ){
							mark_node_solved(path, j-1);
						}
					}	
				}
				
			}
			
			//even though the backup is assigned to v[j-1]
			//the backup for v[j-2] will not use these values if visited=0
			//changed - 5/1
			//i want to assign only those to V(j-1) that have different values than
			//that of the heuristic
			//these will also be marked as visited\
			//the error parameter controls the width of the tree
			//e.g. the difference in value must be atleast as large as the difference
			//in the on trajectory state
			
			setValuePolicyVisited( backup._o1, backup._o2._o1, this_states, j-1 );

			if( j-1 == 0 && ( ( _manager.evaluate(target_val, trajectory_states[j-1].getFactoredState() ).getMax()
					) - ( _manager.evaluate(_valueDD[j-1], trajectory_states[j-1].getFactoredState() ).getMax() ) ) != 0 ){
				_successful_update++;
			}
			if( j-1 == 0 &&
					!_policyDD[j-1].equals(target_policy) ){
				++successful_policy_update ;
			}
			
			//FIX 1 : visit all the nodes in generalized states
			//and remove at the end
			//changed : 5/1/14
			//SetValue already takes care of this
			//sets value and visited accordingly
//			_valueDD[ j ] = resetValue( _valueDD[j], j );
//			_policyDD[ j ] = resetPolicy( _policyDD[j] , j );
			//BACK_CHAIN always true
//			if( BACK_CHAIN ){
//			    visit_save_j = visit_node( this_states, j-1 );
//			}
			
			
//			System.out.println(j + " " + backup._o2._o2 );
			--j;
			
//			System.out.println(j + " " +
//			_manager.enumeratePaths(this_states, false, true,
//					(ADDLeaf)(_manager.DD_ONE.getNode()), false ).size() );
			
			
		}
//		_visited = visit_save;
//		display(_valueDD, _policyDD);
//		System.out.println();
		
	}

	private void setValuePolicyVisited( 
			final ADDRNode new_val,
			final ADDRNode new_policy,
			final ADDRNode update_states,
			final int depth ) {
		
		if( new_policy.equals(_manager.DD_ZERO ) ) {
			try{
				throw new Exception("new policy is zero");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
				
//		if( !_manager.apply( new_val, _manager.BDDNegate( update_states ), 
//				DDOper.ARITH_PROD ).equals( _manager.DD_ZERO) ){
//			System.out.println( "other states are updated"); 
//		}
		
		if( _genStates ){
			final ADDRNode heur = initilialize_node_temp(update_states, update_states, depth);
			final ADDRNode diff = _manager.apply( 
					heur,
					_manager.BDDIntersection(new_val, update_states),
					DDOper.ARITH_MINUS );//how much did the value change with respect to the heuristic
			//so diff will be zero for states outside of update_states
			//careful when threshold
			if( diff.getMin() < 0 ){
				try{
					throw new Exception("Some values have increased wrt heuristic value" );
				}catch(Exception e ){
					e.printStackTrace();
					final ADDRNode error = _manager.threshold( diff, diff.getMin(), false );
					System.out.println( _manager.enumeratePaths(error, true, true, 
							(ADDLeaf)_manager.DD_ONE.getNode(), false ) );
					
					System.out.println( _manager.enumeratePaths(
							_manager.BDDIntersection(heur, error), true, true, 
							(ADDLeaf)_manager.DD_ZERO.getNode(), true ) );
					
					System.out.println( _manager.enumeratePaths(
							_manager.BDDIntersection(new_val, error), true, true, 
							(ADDLeaf)_manager.DD_ZERO.getNode(), true ) );
					
					System.exit(1);
				}
			}
			//threshold
			final ADDRNode small_change_update_only =  
					_manager.BDDIntersection( _manager.threshold(diff, backChainThreshold, false ), 
							update_states );//un updated states --> 0
			
			//large change useful for masking value fn
			final ADDRNode large_change = _manager.BDDNegate( small_change_update_only );
			//un updated states --> 1
			final ADDRNode large_change_update_only = _manager.BDDIntersection(large_change, update_states ); 
			//un updated states --> 0
			
			good_updates += _manager.enumeratePaths( large_change_update_only
					, false, true, _manager.DD_ONE, false ).size();
			
			if( large_change_update_only.equals(_manager.DD_ONE ) ){
				System.out.println("Good updates all around");
			}
			
			if( large_change_update_only.equals(_manager.DD_ZERO ) ){
				//just truncating the trajectory here
				++truncated_backup;
				return;
	//			try{
	//				throw new Exception("For thresholld " + backChainThreshold + 
	//						" no update took place" + ".\nThe max update was = " + diff.getMax() );
	//			}catch(Exception e ){
	//				e.printStackTrace();
	//				System.exit(1);
	//			}
			}
			final ADDRNode new_visited = _manager.BDDUnion( _visited[depth], large_change_update_only);
			if( new_visited.equals( _manager.DD_ONE ) && !_visited[depth].equals(_manager.DD_ONE) ){
				System.out.println("visited is one. Depth " + depth );
			}
			_visited[ depth ] = new_visited;
			
			_valueDD[ depth ] =
					_manager.BDDIntersection( new_val, large_change );
			//must be OK here tio use intersection as diff already has the vars of new_val
			//and so good_update has the same vars
			
//					_manager.constrain( new_val, 
//					good_or_no_update_constraint//no good and updated -> 0
//							, _manager.DD_ZERO );//!(^) = good | no update -> 1
			_policyDD[ depth ] = new_policy;//, large_change_update_only, _manager.DD_ONE ); 
					//new_policy;
//					_manager.BDDIntersection( new_policy, good_or_no_update_constraint );
//					_manager.constrain( new_policy, 
//					good_or_no_update_constraint//no good and updated -> 0
//							, _manager.DD_ONE );
		}else{
			_valueDD[ depth ] = new_val;
//			, 
//					good_or_no_update_constraint//no good and updated -> 0
//							, _manager.DD_ZERO );//!(^) = good | no update -> 1
			_policyDD[ depth ] = new_policy;
//			_manager.constrain( new_policy, 
//					good_or_no_update_constraint//no good and updated -> 0
//							, _manager.DD_ONE );
		}
		
		//have set visited and v and pi
	}
//
//	private ADDRNode resetValue( final ADDRNode value, final int depth ) {
//		return _manager.constrain( value, _visited[ depth ], _manager.DD_ZERO );
//	}

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
	
//	private ADDRNode resetPolicy(final ADDRNode policy, 
//			final int depth ) {
//		final ADDRNode this_visited = _visited[ depth ] ;
//		//whenever visited zero
//		//set policy to _baseline
//		//without increasing size
//		return _manager.constrain(policy, this_visited, _manager.DD_ONE );
//	}

//	private ADDRNode visit_node(ADDRNode states, int depth) {
//	    return  _manager.BDDUnion(_visited[depth], states );
//	}

	private ADDRNode initilialize_node_temp( final ADDRNode value_fn, 
			final ADDRNode states, final int depth ) {
		//for each path in states that lead to 1
		//initialize
	    	if( states.equals(_manager.DD_ONE) ){
	    	    return _manager.getLeaf((steps_lookahead-depth)*_RMAX );
	    	}
		ADDRNode ret = value_fn;
		
		final Set<NavigableMap<String, Boolean>> paths_states
		= _manager.enumeratePaths(states, false, true, _manager.DD_ONE, false);
		ADDRNode weighted_states = _manager.DD_ZERO;
		
		final FactoredState<RDDLFactoredStateSpace> fs = new FactoredState< RDDLFactoredStateSpace >();
		
		for( final NavigableMap<String, Boolean> path_state : paths_states ){
			fs.setFactoredState(path_state);
			final double hval = get_heuristic_value(fs, depth);
			weighted_states = _manager.apply( weighted_states, 
					_manager.scalarMultiply( _manager.getProductBDDFromAssignment(path_state), hval ),
					DDOper.ARITH_PLUS );//paths will already be disjoint
//					_manager.assign( weighted_states , path_state, hval );//assign may increase size
		}
		
		ret = _manager.apply( _manager.apply( ret, 
				_manager.BDDNegate( states ), DDOper.ARITH_PROD ),
				weighted_states, DDOper.ARITH_PLUS );
		
		return ret; 
	}

	public static void main(String[] args) throws InterruptedException {

		System.out.println( Arrays.toString(args) );

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
		}else if( cmd.getOptionValue("generalization").equals("EBL") ){
			generalizer = new EBLGeneralization();
		}
//		else if( cmd.getOptionValue("generalization").equals("reward") ){
//			generalizer = new RewardGeneralization();
//		}
		
		final Random rand = new Random( Long.parseLong( cmd.getOptionValue("seed") ) );
		final boolean constrain_naively = !Boolean.parseBoolean( cmd.getOptionValue("constraintPruning") );
				
		GeneralizationParameters inner_params = null;
		if( cmd.getOptionValue("generalization").equals("value") ){
				inner_params = new ValueGeneralizationParameters( 
			null, GENERALIZE_PATH.valueOf( cmd.getOptionValue("generalizationRule") ), 
			new Random( rand.nextLong() ), constrain_naively  );
		}else if( cmd.getOptionValue("generalization").equals("action") ){
				inner_params = new OptimalActionParameters(
						null, GENERALIZE_PATH.valueOf( cmd.getOptionValue("generalizationRule") ), 
						new Random( rand.nextLong() ) , constrain_naively,
						Boolean.parseBoolean(cmd.getOptionValue("actionAllDepth") ),
						UTYPE.valueOf(cmd.getOptionValue("actionType") ) );
		}else if( cmd.getOptionValue("generalization").equals("EBL") ){
			inner_params = new EBLParams(null, null, new Random( rand.nextLong()), 
					null , constrain_naively,
					Boolean.parseBoolean(cmd.getOptionValue("EBLPolicy") ) );
		}else if( cmd.getOptionValue("generalization").equals("reward") ){
			inner_params = new RewardGeneralizationParameters(null, 
					GENERALIZE_PATH.valueOf( cmd.getOptionValue("generalizationRule") ), 
					new Random( rand.nextLong() ), null, constrain_naively,
				null );
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
				Double.parseDouble( cmd.getOptionValue("convergenceTest") ), 
				DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, 
				Long.parseLong( cmd.getOptionValue("seed") ), 
				Boolean.parseBoolean( cmd.getOptionValue( "discounting" ) ), 
				Integer.parseInt( cmd.getOptionValue("testStates") ), 
				Integer.parseInt( cmd.getOptionValue( "testRounds" ) ),
				Boolean.parseBoolean( cmd.getOptionValue( "actionVars" ) ), 
				constrain_naively,
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
				Boolean.parseBoolean( cmd.getOptionValue("truncateTrials") ),
				Boolean.parseBoolean( cmd.getOptionValue("enableLabelling") ),
				Double.parseDouble( cmd.getOptionValue("backChainThreshold") ) );
		
		}catch( Exception e ){
			HelpFormatter help = new HelpFormatter();
			help.printHelp("symbolicRTDP", options);
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

}