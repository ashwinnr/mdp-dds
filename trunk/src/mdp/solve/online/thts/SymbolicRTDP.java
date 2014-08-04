package mdp.solve.online.thts;

import java.util.Arrays;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;

import mdp.generalize.trajectory.EBLGeneralization;
import mdp.generalize.trajectory.Generalization;
import mdp.generalize.trajectory.GenericTransitionGeneralization.Consistency;
import mdp.generalize.trajectory.OptimalActionGeneralization;
import mdp.generalize.trajectory.RewardGeneralization;
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

import com.sun.org.apache.xml.internal.utils.UnImplNode;

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
//	private int good_updates;
	private int truncated_backup;
	private boolean _genStates;
//	private int generalization;
//	private int generalization_cons;
	private int _onPolicyDepth;
	
	//TODO : how important is base policy?
	//TODO : rollout parameter and exps
	//TODO : action sharing between levels for unvisited nodes

	public SymbolicRTDP(
			String domain,
			String instance,
			double epsilon,
			DEBUG_LEVEL debug,
			ORDER order,
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
			final Random topLevel,
			final int onPolicyDepth ) {
		super(domain, instance, epsilon, debug, order, useDiscounting, numStates,
				numRounds, FAR, constrain_naively, do_apricodd, apricodd_epsilon,
				apricodd_type, heuristic_type, time_heuristic_mins, steps_heuristic,
				MB, init_state_conf, init_state_prob, dp_type, nTrials, steps_dp,
				steps_lookahead, generalizer, generalize_parameters_wo_manager,
				gen_fix_states, gen_fix_actions, gen_num_states, gen_num_actions,
				gen_rule, exploration, cons, truncateTrials, enableLabelling,
				new Random( topLevel.nextLong() )  );
		
		_onPolicyDepth = onPolicyDepth;
		
//		this.BACK_CHAIN = backChain;
			
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

//		if( enableLabelling && !_manager.evaluate(_solved[0], state.getFactoredState()).equals(_manager.DD_ONE) ){
		do_sRTDP( state );
//			display(  );//_valueDD, _policyDD );
//		}
//		display();
		
//		final ADDRNode action_dd 
//			= _manager.restrict(_policyDD[0] ,  state.getFactoredState() );
		final NavigableMap<String, Boolean> action 
			= pick_successor_node(state, 0).getFactoredAction();//ADDManager.sampleOneLeaf(action_dd, _rand );
		
//		_manager.flushCaches();
		
		cur_action.setFactoredAction(action);
		
		//remember root node description of policy?
		
		throwAwayEverything();
		saveValuePolicy();
		
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

				if( enableLabelling && is_node_solved(cur_state, num_actions) ){
					break;
				}

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
				if( num_actions == steps_lookahead-1 ){
					if( _genStates ){
						//changed 5/27/014
						//find states that have the same reward
						//not possible to find all
						//only finding the BDD traversed by state in \sum_R
						final ADDRNode gen_leaf = generalize_leaf( cur_state, num_actions);
						
//						System.out.println("leaf node : depth "  + num_actions + " " 
//						+ _manager.enumeratePathsBDD(gen_leaf).toString() );
						
						initialize_leaf( cur_state, num_actions, gen_leaf );
//						visit_node( gen_leaf, num_actions );
					}else{
						initialize_leaf(cur_state, num_actions);
//						visit_node(cur_state, num_actions);
					}
				}
//					else if( truncateTrials && !is_node_visited(cur_state, num_actions) ){
//					initilialize_node(cur_state, num_actions);
//					visit_node( cur_state, num_actions );
////					System.out.println("Truncating trial : " + num_actions );
////					System.out.println("Truncating trial : " + cur_state );
//					break;
//				}
				
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
			
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> root_action = pick_successor_node(init_state, 0);
			
//			_DPTimer.PauseTimer();
			System.out.print("*");			
//			System.out.println("Trials to go  " + trials_to_go );
			
			solved = _manager.evaluate(_solved[0], init_state.getFactoredState()).equals(_manager.DD_ONE);
//			display();
			if( trials_to_go % 100 == 0 ){
				System.out.println( "#updates to value " + (double)_successful_update / nTrials );
				System.out.println( "#updates to policy " + (double)successful_policy_update/ nTrials );
//				System.out.println( "Heuristic sharing " + this.heuristic_sharing );
//				System.out.println( "#Good updates " + (double)good_updates / ( steps_lookahead * (nTrials-trials_to_go) ) );
				
				System.out.println( "Value of init state " + 
						_manager.evaluate(_valueDD[0], init_state.getFactoredState() ).toString() );
				System.out.println("DP time: " + _DPTimer.GetElapsedTimeInMinutes() );
				
				System.out.println("root node action : " + root_action.toString() );
				
				System.out.print("description of value of init state ");
				
				System.out.println(  
								(_manager.enumeratePathsBDD( 
										_manager.get_path(
												_valueDD[0], init_state.getFactoredState() )).iterator().next().size()-1.0d)/(1.0d*_mdp.getNumStateVars()) );
				
				
				System.out.println( "Value : " + 
						_manager.enumeratePathsBDD( _manager.get_path(_valueDD[0], init_state.getFactoredState() ) ) );
				
				System.out.print("description of policy of init state ");
				

				System.out.println( "Policy : " + 
						(_manager.enumeratePathsBDD( 
								_manager.get_path(
										_manager.restrict(_policyDD[0], root_action.getFactoredAction()), 
										init_state.getFactoredState() ) ).iterator().next().size()-1) / (1.0d*_mdp.getNumStateVars()) );
				
				System.out.println( "Policy : " + 
						_manager.enumeratePathsBDD( 
								_manager.get_path(
										_manager.restrict(_policyDD[0], root_action.getFactoredAction()), 
										init_state.getFactoredState() ) ) );
				
//				display(  );
			}
//			System.out.println( "Value of init state " + 
//					_manager.evaluate(_valueDD[0], init_state.getFactoredState() ).toString() );
		}
		System.out.println();
		
	}


	protected ADDRNode[] generalize_trajectory(final FactoredState<RDDLFactoredStateSpace>[] trajectory_states, 
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>[] trajectory_actions  ){

		saveValuePolicy();
		
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
//		System.out.println("num states " + num_states );
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
			
//			final ADDRNode next_states = _dtr.BDDImageAction( this_states, DDQuantify.EXISTENTIAL,
//					this_actions, true, _actionVars );//WARNING : constant true
			//visited in next state already initialized and updated
			//others need to be initialized optimistically
			//so - initialize them with MAX pruning
			//if(x,y) visited but next_states has (x)
			//initialize (x,y') = (x,y) - but no longer admissible
			//the problem with BDD intersection is adds variables to the 
			//level of abstraction of visited
			//which will be regressed
//			final ADDRNode image_not_visited = _manager.BDDIntersection(next_states, 
//					_manager.BDDNegate( _visited[j] ) );//1 iff next state ^ not visited
//				_manager.constrain( 
//				_manager.BDDIntersection( next_states, 
//					_manager.BDDNegate( 
//						( BACK_CHAIN && visit_save_j  != null ) ? visit_save_j : _visited[j] ) );
//			if( !image_not_visited.equals(_manager.DD_ZERO) ){
//				source_val = initilialize_node_temp( _valueDD[j], image_not_visited, j);
//			}else{
			
			source_val = _valueDD[ j ];
//			}
			
			_DPTimer.ResumeTimer();
			UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> backup = null;
			if( _genStates ){
//				final int index_cur_partition = _dtr.addStateConstraint( 
//						_manager.getRandomSubset( trajectory_states[ j-1 ].getFactoredState() ) ); 
//				
				if( j-1 < _onPolicyDepth ){
					backup = _dtr.backup( 
							target_val, target_policy, source_val, 
						this_states, dp_type, 
						do_apricodd, do_apricodd ? apricodd_epsilon[j-1] : 0 , 
								apricodd_type, true, MB , CONSTRAIN_NAIVELY, null  );
				}else{
					backup = _dtr.backup( target_val, target_policy, source_val, 
							this_states, dp_type, 
							do_apricodd, do_apricodd ? apricodd_epsilon[j-1] : 0 , 
									apricodd_type, true, MB , CONSTRAIN_NAIVELY, target_policy );
				}
//				if( !_dtr.removeStateConstraint(index_cur_partition) ){
//					try{
//						throw new Exception("state constraint could not be removed");
//					}catch( Exception e ){
//						e.printStackTrace();
//						System.exit(1);
//					}
//				}
				
				
			}else{
				backup = _dtr.backup( target_val, target_policy, source_val, this_states, dp_type, 
						do_apricodd, do_apricodd ? apricodd_epsilon[j-1] : 0 , apricodd_type, true, MB, 
								CONSTRAIN_NAIVELY , null  );
//				System.out.println( backup._o2._o2 );
			}
			_DPTimer.PauseTimer();
			
			setValuePolicyVisited( backup._o1, backup._o2._o1, this_states, j-1 , 
					trajectory_states[j-1], trajectory_states[j] );
			
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

	//purpose of this method is to keep _valueDD compact
	//ie remove some states that are already backed up
	//for the purpose of compactness
	//method 1 : change wrt heuristic - for newly visited - 2 => BDD
	//method 2 : change wrt cur_value - for already visited - meh
	//method 3 : restrict to the partition of the backup that - 1 => BDD
	//set visited to intersection
	//contains the cur state
	private void setValuePolicyVisited( 
			final ADDRNode new_val,
			final ADDRNode new_policy,
			final ADDRNode update_states,
			final int depth,
			final FactoredState<RDDLFactoredStateSpace> actual_state,
			final FactoredState<RDDLFactoredStateSpace> next_state ) {
		
		if( new_policy.equals(_manager.DD_ZERO ) ) {
			try{
				throw new Exception("new policy is zero");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		if( update_states.equals(_manager.DD_ZERO) ){
		    try{
			throw new Exception("update states is zero in set value");
		    }catch( Exception  e ){
			e.printStackTrace();
			System.exit(1);
		    }
		}
		
		if( _genStates ){
			//method 1 BDD
			// current partition subseteq update_states must contain actual_state
			// unless consistency is used! - in which case current_partition 
			// can be a superset
			ADDRNode current_parition = findNewGeneralizedPartition( new_val,
				new_policy, update_states, depth, actual_state, next_state );
			
//			final ADDRNode diff = _manager.BDDIntersection( update_states, 
//					_manager.BDDNegate(current_parition) );
			
			if( _manager.getVars(current_parition).get(0).contains("delta_y_1") ){
				try{
					throw new Exception("Problem");
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
			
//			System.out.println( "gen state " + depth + " " + _manager.enumeratePathsBDD(current_parition).toString() );
//			System.out.println( "updated state " + depth + " " + _manager.enumeratePathsBDD(update_states).toString() );
			current_parition = _manager.BDDIntersection( current_parition, update_states );

//			System.out.println( depth + " " + 
//					_manager.enumeratePathsBDD(current_parition).iterator().next().size()/(1.0d*_mdp.getNumStateVars()) );
//			
//			System.out.println( "gen state* " + depth + " " + _manager.enumeratePathsBDD(current_parition).toString() );
			//check to see wasteful work
			//update ^ ~current_part
			
//			final int terms_current_partition = _manager.countPathsBDD(current_parition);
//			generalization += terms_current_partition;
//
//			final int terms_current_partition_after_cons = _manager.countPathsBDD(current_parition);
//			generalization_cons += terms_current_partition_after_cons;
			
			if( !_manager.evaluate(current_parition, actual_state.getFactoredState() ).equals(_manager.DD_ONE) ){
				try{
					throw new Exception("current partition does not contain actual state" );
				}catch( Exception e ) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			
//			_visited[ depth ] = _manager.BDDUnion( _visited[depth], current_parition );
			
			//actual state must be visited
//			if( !is_node_visited(actual_state, depth) ){
//				try{
//					throw new Exception("actual state has not been visited");
//				}catch( Exception e ) {
//					e.printStackTrace();
//					System.exit(1);
//				}
//			}
			
			_valueDD[ depth ]  =
//					_manager.constrain( new_val, _visited[ depth ], _manager.DD_NEG_INF );
					_manager.apply( 
							_manager.BDDIntersection( new_val, current_parition ),
							_manager.BDDIntersection( _valueDD[depth], _manager.BDDNegate(current_parition)),
							DDOper.ARITH_PLUS );
							
//							
//							_manager.constrain(new_val, _visited[depth], _manager.DD_NEG_INF );
//			
			//updated state - value not = neg inf
//			if( _manager.BDDIntersection(_valueDD[ depth ], current_parition ).getMin() 
//					== _manager.getNegativeInfValue() ){
//			    try{
//			    	throw new Exception("Updated state has value -inf");
//			    }catch( Exception e ){
//			    	e.printStackTrace();
//					System.exit(1);
//			    }
//			}
			_policyDD[ depth ] =
//				_manager.constrain(new_policy, _visited[depth], _manager.DD_ONE );
					_manager.BDDUnion( _manager.BDDIntersection(new_policy, current_parition) ,
							_manager.BDDIntersection( _policyDD[depth], _manager.BDDNegate(current_parition) ) );
			
					//_manager.constrain(new_policy, _visited[depth], _manager.DD_ONE );
//			_policyDD[ depth ] = _dtr.applyMDPConstraints(_policyDD[depth], null, _manager.DD_ZERO, false, null);

			//actual state must have new values and policy
			if( get_value(actual_state, depth) != 
					_manager.evaluate(new_val, actual_state.getFactoredState() ).getMax() ){
				try{
					throw new Exception("actual state value not set properly " );
				}catch( Exception e ) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			
			//EXPENSIVE:
			//remove
			if( !_manager.restrict(_policyDD[depth], actual_state.getFactoredState() ).
					equals( _manager.restrict(new_policy, actual_state.getFactoredState() ) ) ){
				try{
					throw new Exception("actual state policy not set properly " );
				}catch( Exception e ) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			
		}
		
		//method 1 + method 2 BDD
//		final ADDRNode - lets just do this for mow
		
		
				
//		if( !_manager.apply( new_val, _manager.BDDNegate( update_states ), 
//				DDOper.ARITH_PROD ).equals( _manager.DD_ZERO) ){
//			System.out.println( "other states are updated"); 
//		}
		
		//SANITY : value not -inf should only decrease
//    	final ADDRNode diff_check = _manager.apply(
//    			_manager.BDDIntersection(_valueDD[depth], _visited[depth] ),
//    			_manager.BDDIntersection(new_val, _visited[depth] ),
//    			DDOper.ARITH_MINUS );
//		if( diff_check.getMin() < 0 ){
//    		try{
//    			throw new Exception ("value of state has increased wrt previous value " 
//    		+ diff_check.getMin() +"\nDepth = " + depth );
//    		}catch( Exception e ){
//    			e.printStackTrace();
//    			final ADDRNode error = _manager.threshold( diff_check, diff_check.getMin(), false );
//				System.out.println( _manager.enumeratePaths(error, true, true, 
//						(ADDLeaf)_manager.DD_ONE.getNode(), false ) );
//				
//				System.out.println( _manager.enumeratePaths(
//						_manager.BDDIntersection(_valueDD[depth], error), true, true, 
//						(ADDLeaf)_manager.DD_ZERO.getNode(), true ) );
//				
//				System.out.println( _manager.enumeratePaths(
//						_manager.BDDIntersection(new_val, error), true, true, 
//						(ADDLeaf)_manager.DD_ZERO.getNode(), true ) );
//				
//				System.out.println("Current Heuristic value : " + 
//				_manager.enumeratePaths( initilialize_node_temp(update_states, update_states, depth) ) );
////				System.exit(1);
//    		}
//    	}
		
//		if( _genStates ){
//			//restricting to partition containing cur state
//			//so can inc # partitions in V by at most 1
//			
//		}
//		
//		if( _genStates ){
//		    
//		    	if( _visited[depth].equals(_manager.DD_ONE) ){
//		    	    _valueDD[depth] = new_val;
//		    	    _policyDD[depth] = new_policy;
//		    	    return;
//		    	}
//		    	
//		    	
//		    	
//		    //changed : quality for generalization judged only for newly visited states
//		    	//states that are not visited and have been updated
//		    	//include actual state
//		    	//find updates for unvisited states that are small
//		    	//convert to constraint
//		    	final ADDRNode this_dd = _manager.getProductBDDFromAssignment( actual_state.getFactoredState() );
//		    	
//		    	//care = update ^ !visited
//		    	final ADDRNode care_states =  
//		    			_manager.BDDUnion( 
//		    					this_dd,
//		    					_manager.BDDIntersection(update_states, 
//		    							_manager.BDDNegate(_visited[depth]  ) ) );
//		    	
//		    	if( care_states.equals(_manager.DD_ZERO) ){
//		    	    //no new states to check
//		    	    _valueDD[depth] = new_val;
//		    	    _policyDD[depth] = new_policy;
//		    	    return;
//		    	}
//		    	
//			final ADDRNode heur = initilialize_node_temp(care_states, care_states, depth);
//			//!care => heur = 0
//			
//			ADDRNode diff = _manager.apply( 
//					heur, 
//					_manager.BDDIntersection(new_val, care_states),
//					DDOper.ARITH_MINUS  );//how much did the value change with respect to the heuristic
//			//so diff will be zero for states outside of update_states
//			//careful when threshold
//			//remove non updated states from diff
//			
////			final ADDRNode checker = _manager.BDDIntersection(diff, care_states );
//			if( diff.getMin() < 0  ){
//				try{
//					throw new Exception("Some values have increased wrt heuristic value " + " depth = " + depth  );
//				}catch(Exception e ){
//					e.printStackTrace();
//					final ADDRNode error = _manager.threshold( diff, diff.getMin(), false );
//					System.out.println( _manager.enumeratePaths(error, true, true, 
//							(ADDLeaf)_manager.DD_ONE.getNode(), false ) );
//					
//					System.out.println( _manager.enumeratePaths(
//							_manager.BDDIntersection(heur, error), true, true, 
//							(ADDLeaf)_manager.DD_ZERO.getNode(), true ) );
//					
//					System.out.println( _manager.enumeratePaths(
//							_manager.BDDIntersection(new_val, error), true, true, 
//							(ADDLeaf)_manager.DD_ZERO.getNode(), true ) );
//					
//					System.exit(1);
//				}
//			}
//			diff = _manager.constrain(diff, care_states, _manager.DD_NEG_INF);
//			//!care => diff = -inf
//			
//			//changed : 5/5 - removed backChainThresh
//			//instead use delta of actual state as thresh
//			//remember changes that were at least as large as actual state
//			//therefore strictly more work than rtdp
//			//WARNING : laziness
//			backChainThreshold = _manager.evaluate(diff, actual_state.getFactoredState() ).getMax(); 
////			System.out.println("State : " + actual_state.getFactoredState() );
////			System.out.println( "Threshold : " + backChainThreshold );
//			
//			//threshold
//			//small_change ^ care --> 1 
//			final ADDRNode small_change_care =   _manager.threshold(diff, backChainThreshold, true );
////			, 
////							update_states );//un updated states --> 0
//			
//			//large change useful for masking value fn
//			//small change ^ care --> 0
//			//!small OR !care --> 1
//			final ADDRNode throw_away_mask = _manager.BDDNegate( small_change_care );
//			//un updated states --> 1
//			//!small AND care --> 1
//			final ADDRNode keep_states_update_only = _manager.BDDIntersection(throw_away_mask, 
//				care_states ); 
//			//un updated states --> 0
//			
//			good_updates += _manager.enumeratePaths( keep_states_update_only
//					, false, true, _manager.DD_ONE, false ).size();
//			
////			if( throw_away_mask.equals(_manager.DD_ONE ) ){
////				System.out.println("Good updates all around");
////			}
//			
//			if( throw_away_mask.equals(_manager.DD_ZERO ) ){
//				//just truncating the trajectory here
//				++truncated_backup;
//				return;
////	//			try{
////	//				throw new Exception("For thresholld " + backChainThreshold + 
////	//						" no update took place" + ".\nThe max update was = " + diff.getMax() );
////	//			}catch(Exception e ){
////	//				e.printStackTrace();
////	//				System.exit(1);
////	//			}
//			}
//			//updates for visited, v and pi
//			final ADDRNode new_visited = _manager.BDDUnion( _visited[depth], keep_states_update_only );
//			//_visited => new_vis
//			// == !_vis OR new_vis
//			if( ! _manager.BDDUnion( _manager.BDDNegate(_visited[depth]) , new_visited ).equals(_manager.DD_ONE) ){
//			    try{
//				throw new Exception("visited turned from one to zero");
//			    }catch( Exception e ){
//				e.printStackTrace();
//				System.exit(1);
//			    }
//			}
//			
//			if( new_visited.equals( _manager.DD_ONE ) && !_visited[depth].equals(_manager.DD_ONE) ){
//				System.out.println("visited is one. Depth " + depth );
//			}
//			
//			//visited = 1 => value != -inf
//			if( _manager.BDDIntersection( _visited[depth], _valueDD[depth] ).getMin() == _manager.getNegativeInfValue() ){
//				try{
//				    throw new Exception("visited is one but value -inf");//THIS IS HAPPENING
//				}catch( Exception e ){
//				    e.printStackTrace();
//				    System.exit(1);
//				}
//			}
//			
//			_visited[ depth ] = new_visited;
//
////			_valueDD[ depth ] = 
////				new_val;
////					_manager.BDDIntersection( new_val, large_change );
//			//must be OK here tio use intersection as diff already has the vars of new_val
//			//and so good_update has the same vars
////			
////					_manager.BDDIntersection( new_val, large_change );
//			
//			//TODO :
//			//does it happen that a visited node has small change
//			//so put -inf 
//			//but does not reset visited
//			//and how does this happen the first time
//			// if this is the case, visited and value will not be coherent!
//			
////			_valueDD[ depth] = _manager.BDDIntersection( new_val, throw_away_mask );
//			_valueDD[ depth ] = _manager.constrain( new_val, _visited[ depth ], _manager.DD_NEG_INF);
//					//			( new_val, 
////						large_change//no good and updated -> 0
////							, _manager.DD_NEG_INF ); //getVMax( depth ) );//!(^) = good | no update -> 1
//			
//			if( _manager.evaluate( _visited[depth], actual_state.getFactoredState() ).getMax() == 
//				0.0d || _manager.evaluate( _valueDD[depth], actual_state.getFactoredState() ).getMax() != 
//				_manager.evaluate( new_val, actual_state.getFactoredState() ).getMax() ) {
//			    try{
//				throw new Exception( "value of actual state is not set");
//			    }catch (Exception e ){
//				e.printStackTrace();
//				System.exit(1);
//			    }
//			}
//
//			//visited = 1 => value != -inf
//			if( _manager.BDDIntersection( _visited[depth], _valueDD[depth] ).getMin() == _manager.getNegativeInfValue() ){
//				try{
//				    throw new Exception("visited is one but value -inf");//THIS IS HAPPENING
//				}catch( Exception e ){
//				    e.printStackTrace();
//				    System.exit(1);
//				}
//			}
//
//			//visited = 0 => value == -inf
//			
//			_policyDD[ depth ] =
//					_manager.constrain( new_policy, _visited[depth], _manager.DD_ONE ) ;
//			// new_policy; 
////				new_policy;//, large_change_update_only, _manager.DD_ONE ); 
//					//new_policy;
////					_manager.BDDIntersection( new_policy, large_change );
////					_manager.BDDUnion( 
////							_manager.BDDIntersection(_baseLinePolicy, _manager.BDDNegate( _visited[depth] ) ),
////							_manager.BDDIntersection( new_policy, 
////							_visited[ depth ] ) );//no good and updated -> 0
////							, _manager.DD_ONE );
//		}
		else{
//	    	_visited[ depth ] = _manager.BDDUnion( _visited[depth], update_states );
		    
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

	private ADDRNode findNewGeneralizedPartition(final ADDRNode new_val,
			final ADDRNode new_policy, 
			final ADDRNode update_states, 
			final int depth,
			final FactoredState<RDDLFactoredStateSpace> actual_state, 
			final FactoredState<RDDLFactoredStateSpace> next_state ) {
//		final ADDRNode action_dd = _manager.restrict(new_policy, actual_state.getFactoredState() );
//		cur_action.setFactoredAction( _manager.sampleOneLeaf(action_dd, _actionSelectionRand ) );
		
		final ADDRNode save_value = _valueDD[ depth ];
		final ADDRNode save_policy = _policyDD[ depth ];
		
		_valueDD[ depth ] = new_val;
		_policyDD[ depth ] = new_policy;
		
		saveValuePolicy();
		
		//this does not take into account consistency
		final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> 
			new_action = pick_successor_node(actual_state, depth);
		
		final ADDRNode new_generalized_state = 
				_generalizer.generalize_state(actual_state, new_action, next_state, 
						_genaralizeParameters, depth);
		
		_valueDD[ depth ] = save_value;
		_policyDD[ depth ] = save_policy;
		
		saveValuePolicy();
		
		return new_generalized_state;
	}

	private void saveValuePolicy() {
		_genaralizeParameters.set_valueDD(_valueDD);
		_genaralizeParameters.set_policyDD( _policyDD );
//		_genaralizeParameters.set_visited(_visited);
		
		final P inner_params = _genaralizeParameters.getParameters();
		inner_params.set_valueDD(_valueDD);
		inner_params.set_policyDD(_policyDD);
//		inner_params.set_visited(_visited);
	}
	
//	private ADDRNode initilialize_node_temp( final ADDRNode value_fn, 
//			final ADDRNode states, final int depth ) {
//		//for each path in states that lead to 1
//		//initialize
//    		if( states.equals(_manager.DD_ONE) ){
//    		    return _manager.getLeaf((steps_lookahead-depth)*_RMAX );
//    		}
//		ADDRNode ret = value_fn;
//		
//		final Set<NavigableMap<String, Boolean>> paths_states
//		= _manager.enumeratePaths(states, false, true, _manager.DD_ONE, false);
//		ADDRNode weighted_states = _manager.DD_ZERO;
//		
//		final FactoredState<RDDLFactoredStateSpace> fs = new FactoredState< RDDLFactoredStateSpace >();
//		
//		for( final NavigableMap<String, Boolean> path_state : paths_states ){
//			fs.setFactoredState(path_state);
//			final double hval = get_heuristic_value(fs, depth);
//			ADDRNode this_dd = _manager.getProductBDDFromAssignment(path_state);
////			_manager.showGraph( this_dd );
//			weighted_states = _manager.apply( weighted_states, 
//					_manager.scalarMultiply( this_dd, hval ),
//					DDOper.ARITH_PLUS );//paths will already be disjoint
////					_manager.assign( weighted_states , path_state, hval );//assign may increase size
//		}
//		
////		_manager.showGraph( weighted_states );
//		
//		ret = _manager.apply( _manager.apply( ret, 
//				_manager.BDDNegate( states ), DDOper.ARITH_PROD ),
//				weighted_states, DDOper.ARITH_PLUS );
//		
//		if( _manager.BDDIntersection(ret, states).getMin() == _manager.getNegativeInfValue() ){
//		    try{
//			throw new Exception("newly initialized state has neg inf value");
//		    }catch( Exception e  ){
//			e.printStackTrace();
//			System.exit(1);
//		    }
//		}
//		
//		return ret; 
//	}
	

//	private ADDRNode initilialize_node_temp( final ADDRNode value_fn, 
//			final ADDRNode states, final int depth ) {
//		//for each path in states that lead to 1
//		//initialize
//		if( states.equals(_manager.DD_ONE) ){
//		    return _manager.getLeaf((steps_lookahead-depth)*_RMAX );
//		}
//		
////		final Set<NavigableMap<String, Boolean>> paths_states
////		= _manager.enumeratePaths(states, false, true, _manager.DD_ONE, false);
////		ADDRNode weighted_states = _manager.DD_ZERO;
////		final ADDRNode neg_inf_states = _dtr.convertToNegInfDD(states)[0];
//	
//		final List<ADDRNode> rewards = _mdp.getRewards();
//		ADDRNode init_temp = _manager.DD_ZERO ; 
//		
//		for( final ADDRNode rew : rewards ){
//			init_temp = _manager.apply( init_temp, 
//					_manager.constrain(rew, states, _manager.DD_ZERO), 
//					DDOper.ARITH_PLUS );
//			init_temp = _manager.constrain(init_temp, states, _manager.DD_ZERO);
//		}
//		//state s in states => init_temp has value reward
//		//state s not in states => init temp has some value( maybe 0 or not)
//		//init_temp no larger than sum_rewards
//		
////		System.out.println( "depth " + depth + " init value " + hval  );
//		
////		final FactoredState<RDDLFactoredStateSpace> fs = new FactoredState< RDDLFactoredStateSpace >();
//////		
//////		double max_hval = Double.NEGATIVE_INFINITY;
//////		
////		for( final NavigableMap<String, Boolean> path_state : paths_states ){
////			fs.setFactoredState(path_state);
////			final double hval = get_heuristic_value(fs, depth);
//////			max_hval = Math.max( max_hval,  hval );
////			
////			ADDRNode this_dd = _manager.getProductBDDFromAssignment(path_state);
//////			_manager.showGraph( this_dd );
////			weighted_states = _manager.apply( weighted_states, 
////					_manager.scalarMultiply( this_dd, hval ),
////					DDOper.ARITH_PLUS );//paths will already be disjoint
//////					_manager.assign( weighted_states , path_state, hval );//assign may increase size
////		}
////		
////		_manager.showGraph( weighted_states );
//		
//		final ADDRNode ret = _manager.apply( _manager.BDDIntersection( value_fn, 
//				_manager.BDDNegate( states ) ),
////				weighted_states, 
//				_manager.BDDIntersection(states, 
//						_manager.apply( init_temp, _manager.getLeaf( (steps_lookahead-depth)*_RMAX ), DDOper.ARITH_PLUS ) ),
////				_manager.BDDIntersection(states, _manager.getLeaf( (steps_lookahead-depth)*_RMAX ) ), 
//				DDOper.ARITH_PLUS );
//		
////		if( _manager.BDDIntersection(ret, states).getMin() == _manager.getNegativeInfValue() ){
////		    try{
////			throw new Exception("newly initialized state has neg inf value");
////		    }catch( Exception e  ){
////			e.printStackTrace();
////			System.exit(1);
////		    }
////		}
//		
//		return ret; 
//	}

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
		else if( cmd.getOptionValue("generalization").equals("reward") ){
			generalizer = new RewardGeneralization();
		}
		
		
		final Random topLevel = new Random( Long.parseLong( cmd.getOptionValue("seed") ) );
		final boolean constrain_naively = !Boolean.parseBoolean( cmd.getOptionValue("constraintPruning") );
				
		GeneralizationParameters inner_params = null;
		if( cmd.getOptionValue("generalization").equals("value") ){
				inner_params = new ValueGeneralizationParameters( 
			null, GENERALIZE_PATH.valueOf( cmd.getOptionValue("generalizationRule") ), 
			 constrain_naively  );
		}else if( cmd.getOptionValue("generalization").equals("action") ){
				inner_params = new OptimalActionParameters(
						null, GENERALIZE_PATH.valueOf( cmd.getOptionValue("generalizationRule") ), 
						constrain_naively, null );
		}else if( cmd.getOptionValue("generalization").equals("EBL") ){
			inner_params = new EBLParams(null, null, 
					null , constrain_naively,
					Boolean.parseBoolean(cmd.getOptionValue("EBLPolicy") ) );
		}else if( cmd.getOptionValue("generalization").equals("reward") ){
			inner_params = new RewardGeneralizationParameters(null, 
					GENERALIZE_PATH.valueOf( cmd.getOptionValue("generalizationRule") ), 
					null, constrain_naively, null );
		}
		
		double[] epsilons = null;
		if( Boolean.parseBoolean( cmd.getOptionValue("doApricodd") ) ){
			epsilons = new double[ Integer.parseInt( cmd.getOptionValue("stepsLookahead") ) ];
			double epsilon = Double.parseDouble( cmd.getOptionValue("apricoddError") );
			for( int i = 0 ; i < epsilons.length; ++i ){
				epsilons[i] = epsilon;
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
				new Random( topLevel.nextLong() ),
				Integer.parseInt( cmd.getOptionValue("onPolicyDepth") ) );
		
		}catch( Exception e ){
			HelpFormatter help = new HelpFormatter();
			help.printHelp("symbolicRTDP", options);
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	@Override
	public boolean is_node_visited(FactoredState<RDDLFactoredStateSpace> state,
			int depth) {
		try{
			throw new UnsupportedOperationException();
		}catch( Exception e ){
			e.printStackTrace();
			System.exit(1);
		}
		return false;
	}

	@Override
	public void initilialize_node(FactoredState<RDDLFactoredStateSpace> state,
			int depth) {
		try{
			throw new UnsupportedOperationException();
		}catch( Exception e ){
			e.printStackTrace();
			System.exit(1);
		}		
	}

	@Override
	public void visit_node(FactoredState<RDDLFactoredStateSpace> state,
			int depth) {
		try{
			throw new UnsupportedOperationException();
		}catch( Exception e ){
			e.printStackTrace();
			System.exit(1);
		}		
	}

}