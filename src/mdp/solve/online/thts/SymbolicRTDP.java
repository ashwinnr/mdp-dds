package mdp.solve.online.thts;

import java.util.Arrays;
import java.util.NavigableMap;
import java.util.Random;

import mdp.generalize.trajectory.EBLGeneralization;
import mdp.generalize.trajectory.Generalization;
import mdp.generalize.trajectory.GenericTransitionGeneralization.Consistency;
import mdp.generalize.trajectory.OptimalActionGeneralization;
import mdp.generalize.trajectory.ValueGeneralization;
import mdp.generalize.trajectory.parameters.EBLParams;
import mdp.generalize.trajectory.parameters.GeneralizationParameters;
import mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH;
import mdp.generalize.trajectory.parameters.OptimalActionParameters;
import mdp.generalize.trajectory.parameters.RewardGeneralizationParameters;
import mdp.generalize.trajectory.parameters.ValueGeneralizationParameters;
import mdp.generalize.trajectory.type.GeneralizationType;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import util.InstantiateArgs;
import util.Timer;
import util.UnorderedPair;
import add.ADDRNode;

import com.google.common.collect.Maps;

import dd.DDManager.APPROX_TYPE;
import dd.DDManager.DDOper;
import dd.DDManager.DDQuantify;
import dtr.add.ADDDecisionTheoreticRegression.BACKUP_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;

public class SymbolicRTDP< T extends GeneralizationType, 
	P extends GeneralizationParameters<T> > extends SymbolicTHTS< T, P >  {


	public static boolean  DISPLAY_TRAJECTORY = false;

	protected int _onPolicyDepth;

	public enum LearningRule{
		NONE, DECISION_LIST
	}
	private LearningRule _learningRule;
	public enum LearningMode{
		BATCH, ONLINE
	}
	private LearningMode _learningMode;
	private ADDRNode _learnedPolicy = null;
	private int _numRules = 0;
	private int _maxRules;
	
	private boolean do_Xion = true;
	protected boolean _stationary_vfn = false;

	protected double _avgDPTime = 0;
	protected int _numUpdates = 0;
	
	public SymbolicRTDP(
			String domain,
			String instance,
			double epsilon,
			DEBUG_LEVEL debug,
			ORDER order,
			boolean useDiscounting,
			int numStates,
			int numRounds,
			boolean constrain_naively,
			boolean do_apricodd,
			double[] apricodd_epsilon,
			APPROX_TYPE apricodd_type,
			INITIAL_STATE_CONF init_state_conf,
			double init_state_prob,
			final int nTrials,
			final double timeOutMins, 
			int steps_lookahead,
			Generalization<RDDLFactoredStateSpace, RDDLFactoredActionSpace, T, P> generalizer,
			P generalize_parameters_wo_manager,
			boolean gen_fix_states,
			boolean gen_fix_actions,
			int gen_num_states,
			int gen_num_actions,
			GENERALIZE_PATH gen_rule,
			Consistency[] cons, 
			final Random topLevel,
			final int onPolicyDepth,
			final LearningRule learningRule,
			final int maxRulesToLearn,
			final LearningMode learningMode ,
			final boolean do_Xion ,
			final boolean stat_vfn,
			final GLOBAL_INITIALIZATION global_init, 
			final LOCAL_INITIALIZATION local_init,
			final boolean truncateTrials,
			final boolean mark_visited,
			final boolean mark_solved,
			final REMEMBER_MODE remember_mode,
			final boolean reward_init ) {
		super( domain, instance, epsilon, debug, order, useDiscounting, numStates,
				numRounds, true, constrain_naively, do_apricodd, apricodd_epsilon,
				apricodd_type,
				BACKUP_TYPE.VI_FAR, 10, 1, -1, 
				init_state_conf, init_state_prob, BACKUP_TYPE.VI_FAR, nTrials, 
				timeOutMins, steps_lookahead, generalizer, generalize_parameters_wo_manager,
				gen_fix_states, gen_fix_actions, gen_num_states, gen_num_actions,
				gen_rule, cons, truncateTrials, false , 
				new Random( topLevel.nextLong() ) , global_init, local_init,
				mark_visited, mark_solved, remember_mode, reward_init );
		
		_learningMode = learningMode;
		_maxRules = maxRulesToLearn;
		_learningRule = learningRule;
		_onPolicyDepth = onPolicyDepth;
		
		this.do_Xion = do_Xion;
//		this.BACK_CHAIN = backChain;
			
		_genStates = !gen_fix_states;
		
		_stationary_vfn = stat_vfn;
		_generalizer.setStationary( _stationary_vfn );
		
	}

	@Override
	public FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> act(
			final FactoredState<RDDLFactoredStateSpace> state) {

		if( _learningRule.equals( LearningRule.DECISION_LIST )  && 
				( ( _learningMode.equals(LearningMode.ONLINE ) ) || 
						( _learningMode.equals(LearningMode.BATCH) && _numRules == _maxRules ) ) ){
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>
				lookup = lookupRule( state );
			if( lookup != null ){
				System.out.println("Reacting");
				return lookup;
			}else{
				//TODO : 
				//need to compress rules
				//plan again
				//add a rule
				//compression should guarantee reduction in number
			}
		}
		
//		if( enableLabelling && !_manager.evaluate(_solved[0], state.getFactoredState()).equals(_manager.DD_ONE) ){
		if( _learningRule.equals(LearningRule.NONE) || 
				(!_learningRule.equals(LearningRule.NONE) && _numRules < _maxRules ) ){
			do_sRTDP( state );	
			final NavigableMap<String, Boolean> action 
			= pick_successor_node(state, 0).getFactoredAction();//ADDManager.sampleOneLeaf(action_dd, _rand );
			cur_action.setFactoredAction(action);
			if( !_learningRule.equals(LearningRule.NONE) ){
				addTrainingSample( state );
				System.out.println("#Rules : " + _numRules );
			}
		}else{
			System.out.println("Reacting due to max rules reached");
			if( _learningRule.equals( LearningRule.DECISION_LIST )  ){
				final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>
					lookup = lookupRule( state );
				if( lookup != null ){
					cur_action.setFactoredAction( lookup.getFactoredAction() );
				}else{
					//random action
					System.out.println("Base policy");
					cur_action.setFactoredAction( pick_successor_node(state, 0).getFactoredAction() );
				}
			}
		}
		
//			display(  );//_valueDD, _policyDD );
//		}
//		display();
//		final ADDRNode action_dd 
//			= _manager.restrict(_policyDD[0] ,  state.getFactoredState() );
//		_manager.flushCaches();
		//remember root node description of policy?
		//options : testing on/off
		//testingRule : decision list of actions at root node/
		//value of root node, value of all nodes/ linear FA option(+TD)
		//testingTraj ratio : between training and testing samples from new state
		//0 for only act - 1 for training again
		//put back rule - 0/1 after testing - add back to d'list? - same rule as testingRule
//		System.out.println("testing hypothesis" );
//		final NavigableMap<String, Boolean> state_descr = Maps.unmodifiableNavigableMap( _manager.enumeratePathsBDD( 
//				_manager.get_path(_valueDD[0], state.getFactoredState() ) ).iterator().next() );
//		
//		final Random _flipRandom = new Random();
//		int count = 0;
//		
//		for( int test = 0 ; test < 10; ++test ){
//			
//			final NavigableMap<String, Boolean> descr = 
//					Maps.newTreeMap( );
//		
//			for( final String svar : _mdp.get_stateVars() ){
//				if( state_descr.get( svar ) == null ){
//					descr.put( svar, _flipRandom.nextBoolean() );
//				}else{
//					descr.put( svar, state_descr.get( svar ) );
//				}
//			}
//			
//			System.out.println( "test #" + test + " = " + descr.toString() );
//			
//			throwAwayEverything();
//			saveValuePolicy();
//			
//			final FactoredState<RDDLFactoredStateSpace> this_state 
//				= new FactoredState<RDDLFactoredStateSpace>().setFactoredState(descr);
//			do_sRTDP( this_state );
//			
//			final NavigableMap<String, Boolean> new_descr = _manager.enumeratePathsBDD( 
//					_manager.get_path(_valueDD[0], this_state.getFactoredState() ) ).iterator().next();
//			System.out.println( "Result " + new_descr );
//			final int this_count = state_descr.equals(new_descr) ? 1 : 0;
//			System.out.println("This count " +  this_count );
//			count += this_count;
//		}
//		
//		System.out.println("Count positive tests = " + count );
//		
		throwAwayEverything();
		saveValuePolicy();
		
		return cur_action;
	}
	

	private void addTrainingSample(final FactoredState<RDDLFactoredStateSpace> state) {
		if( _learnedPolicy == null ){
			_learnedPolicy = _manager.DD_ZERO;
		}
		if( _learningRule.equals(LearningRule.DECISION_LIST) ){
		
			final NavigableMap<String, Boolean> state_assign = state.getFactoredState();
			final ADDRNode action_dd = _manager.restrict(_policyDD[0], state_assign);
			final NavigableMap<String, Boolean> root_action 
				= _manager.sampleOneLeaf(action_dd, _actionSelectionRandom);
			ADDRNode policy_path = _manager.get_path( _manager.restrict(_policyDD[0], root_action ), 
									state_assign );
			final ADDRNode new_rule = 
					_manager.BDDIntersection(policy_path, _manager.getProductBDDFromAssignment(root_action) );
			System.out.println("Adding rule : " + 
					_manager.enumeratePathsBDD(policy_path).toString() ); 
			System.out.println( "Action " + root_action.toString() );
			
			_numRules++;
			
			_learnedPolicy = _manager.BDDUnion(_learnedPolicy, new_rule );
			
		}
		
	}

	private FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> lookupRule(
			final FactoredState<RDDLFactoredStateSpace> state) {
		
		//TODO : What if the rules are not disjoint 
		//e.g. ordered like a dlist
		
		if( _learnedPolicy == null ){
			return null;
		}

		if( _learningRule.equals(LearningRule.DECISION_LIST) ){
			final ADDRNode action_dd = _manager.restrict(
					_learnedPolicy, state.getFactoredState() );
			if( action_dd.equals(_manager.DD_ZERO ) ){
				return null;
			}
			
			final NavigableMap<String, Boolean> partial_path = 
					Maps.newTreeMap( _manager.sampleOneLeaf( action_dd , _actionSelectionRandom ) );
			System.out.println("Found rule");
			System.out.println(_manager.get_path( _learnedPolicy, 
					state.getFactoredState(), partial_path ) );
			
	//		final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> partial_action 
	//		= cur_action.setFactoredAction( path );
			for( final String actvar : _mdp.get_actionVars() ){
				if( !partial_path.containsKey(actvar) ){
					partial_path.put( actvar, _mdp.getDefaultValue(actvar) );
				}
			}
			if( partial_path.size() != _mdp.get_actionVars().size() ){
				try{
					throw new Exception("partial action simulated");
				}catch( Exception e ){
					e.printStackTrace();
					System.exit(1);
				}
			}
			return cur_action.setFactoredAction(partial_path);
		}
		return null;
	}

	protected void do_sRTDP( final FactoredState<RDDLFactoredStateSpace> init_state ) {

		int trials_to_go = nTrials;
		boolean timeOut = false;
		final Timer act_time = new Timer();
		int numSamples = 0;
		
		final int numStateVars = _mdp.getNumStateVars();
		final NavigableMap<String, Boolean> initStateAssign = init_state.getFactoredState();
		while( trials_to_go --> 0 && ( (timeOutMins == -1) || !timeOut ) 
				&& ( !_enableLabelling || !is_node_solved(init_state, 0) ) ){
			FactoredState<RDDLFactoredStateSpace> cur_state = init_state;
			int num_actions = 0;//steps_lookahead;
//			System.out.println("value of init state : " + _manager.evaluate(value_fn, init_state.getFactoredState() ).getNode().toString() );
			
			trajectory_states[ num_actions ].setFactoredState( cur_state.getFactoredState() );
			if( !is_node_visited(cur_state, num_actions)   ){
				initialize_node(cur_state, num_actions);
//				visit_node(cur_state, num_actions);
//				System.out.println("Truncating trial : " + num_actions );
//				System.out.println("Truncating trial : " + cur_state );
				if( _truncateTrials ){
					continue;
				}
			}
			
			while( num_actions < steps_lookahead-1 ){
				if( DISPLAY_TRAJECTORY ){
					System.out.println( cur_state.toString() );	
				}
				
				//				System.out.println(_manager.evaluate(value_fn, cur_state.getFactoredState() ).getNode().toString() );
//				System.out.println( cur_state.toString() );
//				if( enableLabelling && is_node_solved(cur_state, num_actions) ){
//					break;
//				}

				final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>
					greedy_action = pick_successor_node(cur_state, num_actions);
				final NavigableMap<String, Boolean> greedy_action_assign = greedy_action.getFactoredAction();
				trajectory_actions[ num_actions ].setFactoredAction( greedy_action_assign );
				
				if( DISPLAY_TRAJECTORY ){
					System.out.println( num_actions + " " + cur_action.toString() );	
				}

				//				System.out.println( "Steps to go " + steps_to_go );
				final FactoredState<RDDLFactoredStateSpace> next_state 
				= pick_successor_node(cur_state, trajectory_actions[ num_actions ], num_actions);
				cur_state = next_state;
				++num_actions;
				trajectory_states[ num_actions ].setFactoredState( cur_state.getFactoredState() );
				if( _enableLabelling && num_actions < steps_lookahead-1 && 
						is_node_solved(cur_state, num_actions) ){
					if( DISPLAY_TRAJECTORY ){
						System.out.println(cur_state.toString() + " state marked as solved " + num_actions );
					}
					break;
				}else if( !is_node_visited(cur_state, num_actions) ){
//					System.out.println("initializing " + cur_state );
					initialize_node(cur_state, num_actions);
//					visit_node( cur_state, num_actions );
//					System.out.println("Truncating trial : " + num_actions );
//					System.out.println("Truncating trial : " + cur_state );
					if( _truncateTrials ){
						break;
					}
				}
			}
			if( DISPLAY_TRAJECTORY ){
				System.out.println( num_actions + " " + cur_state.toString() );
			}
//			trajectory_states[ trajectory_states.length-1 ].setFactoredState( cur_state.getFactoredState() );
//			System.out.println("Updating : " + _manager.countPaths(trajectory_states) );
//			System.out.println("Prob. trajectory = " + prob_traj );
			final FactoredState<RDDLFactoredStateSpace>[] trajectory_states_non_null 
				=  Arrays.copyOfRange(trajectory_states, 0, num_actions+1);
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>[] 
					trajectory_actions_non_null =  Arrays.copyOfRange(trajectory_actions, 0, num_actions);
			
			ADDRNode[] gen_trajectory = null;
			if( _genStates ){
				gen_trajectory = generalize_trajectory( trajectory_states_non_null,
					trajectory_actions_non_null );
			}
//			System.out.println(gen_trajectory.length);
//			_DPTimer.ResumeTimer();			
			update_generalized_trajectory( trajectory_states_non_null, trajectory_actions_non_null, 
					gen_trajectory , num_actions+1 );
			
//			_DPTimer.PauseTimer();
			System.out.print("*");	
			++numSamples;
//			System.out.println("Trials to go  " + trials_to_go );
			
//			solved = _manager.evaluate(_solved[0], init_state.getFactoredState()).equals(_manager.DD_ONE);
//			display();
			
			act_time.PauseTimer();
//			System.out.println( act_time.GetElapsedTimeInMinutes() + " " + timeOutMins );
			if( act_time.GetElapsedTimeInMinutes() >= timeOutMins ){
				timeOut = true;
			}
			
			if( trials_to_go % 50 == 0 ){
//				System.out.println( "#updates to value " + (double)_successful_update / nTrials );
//				System.out.println( "#updates to policy " + (double)successful_policy_update/ nTrials );
//				System.out.println( "Heuristic sharing " + this.heuristic_sharing );
//				System.out.println( "#Good updates " + (double)good_updates / ( steps_lookahead * (nTrials-trials_to_go) ) );
				
				FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> root_action = pick_successor_node(init_state, 0);
				
				System.out.println( "\nValue of init state " + 
						_manager.evaluate(_valueDD[0], initStateAssign ).getMax() );
				System.out.println("average DP time: " + _avgDPTime );
				
				System.out.println("root node action : " + root_action.toString() );
				
				System.out.print("description of value of init state ");
				
				final ADDRNode path_aroo = _manager.get_path(_valueDD[0], initStateAssign );
				
				System.out.println(  
								(_manager.enumeratePathsBDD( 
										path_aroo).iterator().next().size()-1.0d)/(1.0d*numStateVars) );
				
				
				System.out.println( "Value : " + 
						_manager.enumeratePathsBDD( path_aroo ) );
				
				System.out.print("description of policy of init state ");
				

				final ADDRNode policy_roo = _manager.get_path(
						_manager.restrict(_policyDD[0], root_action.getFactoredAction()), 
						initStateAssign ) ;
				
				System.out.println( "Policy : " + 
						(_manager.enumeratePathsBDD( 
								policy_roo ).iterator().next().size()-1) / (1.0d*numStateVars) );
				
				System.out.println( "Policy : " + 
						_manager.enumeratePathsBDD( 
								policy_roo ) );
				
//				display(  );
			}
			
			if( !timeOut ){
				act_time.ResumeTimer();
			}
			
//			System.out.println( "Value of init state " + 
//					_manager.evaluate(_valueDD[0], init_state.getFactoredState() ).toString() );
			if( _enableLabelling && is_node_solved(init_state, 0) ){
				System.out.println("initial state marked as solved");
				break;
			}
		}
		System.out.println();
		System.out.println("num samples : " + numSamples );
//		System.out.println( "#updates to value " + (double)_successful_update / nTrials );
//		System.out.println( "#updates to policy " + (double)successful_policy_update/ nTrials );
////		System.out.println( "Heuristic sharing " + this.heuristic_sharing );
//		System.out.println( "#Good updates " + (double)good_updates / ( steps_lookahead * (nTrials-trials_to_go) ) );
		
		FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> root_action = pick_successor_node(init_state, 0);
		
		System.out.println( "Value of init state " + 
				_manager.evaluate(_valueDD[0], initStateAssign ).toString() );
		System.out.println("avg DP time: " + _avgDPTime );
		
		System.out.println("root node action : " + root_action.toString() );
		
		System.out.print("description of value of init state ");
		
		System.out.println(  
						(_manager.enumeratePathsBDD( 
								_manager.get_path(
										_valueDD[0], initStateAssign )).iterator().next().size()-1.0d)/(1.0d*numStateVars) );
		
		
		System.out.println( "Value : " + 
				_manager.enumeratePathsBDD( _manager.get_path(_valueDD[0], initStateAssign ) ) );
		
		System.out.print("description of policy of init state ");
		

		final NavigableMap<String, Boolean> rootNodeFactoredAction = root_action.getFactoredAction();
		System.out.println( "Policy : " + 
				(_manager.enumeratePathsBDD( 
						_manager.get_path(
								_manager.restrict(_policyDD[0], rootNodeFactoredAction), 
								initStateAssign ) ).iterator().next().size()-1) / (1.0d*numStateVars) );
		
		System.out.println( "Policy : " + 
				_manager.enumeratePathsBDD( 
						_manager.get_path(
								_manager.restrict(_policyDD[0], rootNodeFactoredAction), 
								initStateAssign ) ) );
		
	}


	protected ADDRNode[] generalize_trajectory(final FactoredState<RDDLFactoredStateSpace>[] trajectory_states, 
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>[] trajectory_actions  ){

		saveValuePolicy();
		
//		System.out.println( "Generalizing " );
		
		return _generalizer.generalize_trajectory(trajectory_states, trajectory_actions,
				_genaralizeParameters );
	}
	
	protected void update_generalized_trajectory( 
			final FactoredState<RDDLFactoredStateSpace>[] trajectory_states, 
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>[] trajectory_actions, 
			final ADDRNode[] trajectory,
			final int num_states ){
		
		int i = (num_states-1)*2;//-1;
		int j = num_states-1;
//		System.out.println("num states " + num_states );
			
		while( j > 0 ){
		
//	    	System.out.println("Updating trajectories " + j );
			ADDRNode source_val, target_val, target_policy;
			target_val = _stationary_vfn ? _valueDD[0] : _valueDD[ j-1 ];
			target_policy = _stationary_vfn ? _policyDD[0] : _policyDD[ j-1 ];
			source_val = _stationary_vfn ? _valueDD[0] : _valueDD[ j ];

			UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> backup = null;
			Timer dp_time = new Timer();
			if( _genStates ){
				ADDRNode this_next_states, this_states, this_actions;
				this_next_states = trajectory[i--];
				this_actions = trajectory[i--];
				this_states = trajectory[i];
		    
				if( j-1 < _onPolicyDepth ){
					backup = _dtr.backup( 
							target_val, target_policy, source_val, 
						this_states, BACKUP_TYPE.VI_FAR, 
						do_apricodd, 
						do_apricodd ? ( _stationary_vfn ? apricodd_epsilon[0] : apricodd_epsilon[j-1] ) : 0 , 
								apricodd_type, true, -1 , CONSTRAIN_NAIVELY, null  );
				}else{
					backup = _dtr.backup( target_val, target_policy, source_val, 
							this_states, BACKUP_TYPE.VI_FAR, 
							do_apricodd, 
							do_apricodd ? ( _stationary_vfn ? apricodd_epsilon[0] : apricodd_epsilon[j-1] ) : 0, 
									apricodd_type, true, -1 , CONSTRAIN_NAIVELY, target_policy );
				}
				setValuePolicyVisited( backup._o1, backup._o2._o1, this_states, 
						_stationary_vfn ? 0 : j-1 , 
						trajectory_states[j-1], trajectory_states[j] );
			}else{
				final ADDRNode flat_state_bdd = _manager.getProductBDDFromAssignment(trajectory_states[j-1].getFactoredState());
				backup = _dtr.backup( target_val, target_policy, source_val, 
							flat_state_bdd, 
							BACKUP_TYPE.VI_FAR, 
						do_apricodd, 
						do_apricodd ? ( _stationary_vfn ? apricodd_epsilon[0] : apricodd_epsilon[j-1] ) : 0, 
								apricodd_type, true, -1, 
								CONSTRAIN_NAIVELY , null  );
				_valueDD[ j-1 ] = backup._o1;
				_policyDD[ j-1 ] = backup._o2._o1;
				//_manager.BDDUnion(
					//					_manager.BDDIntersection( greedy_action, flat_state_bdd ),
						//				_manager.BDDIntersection( _policyDD[ j-1 ] , _manager.BDDNegate(flat_state_bdd) ) );
				
				if( _markVisited ){
					visit_node(flat_state_bdd, j-1 );
				}
				if( _enableLabelling ){
					ADDRNode new_solved = getSolvedStates(j-1, flat_state_bdd);
					mark_node_solved(new_solved, j-1);
				}
				saveValuePolicy();
			}

			dp_time.StopTimer();
			final double this_time = dp_time.GetElapsedTimeInMinutes();
			++_numUpdates;
			_avgDPTime = ( ( _numUpdates-1 )*_avgDPTime + this_time ) / _numUpdates;
			--j;
			
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
//			}
//			if( _genStates ){
//				final int index_cur_partition = _dtr.addStateConstraint( 
//						_manager.getRandomSubset( trajectory_states[ j-1 ].getFactoredState() ) ); 
//				
//				if( !_dtr.removeStateConstraint(index_cur_partition) ){
//					try{
//						throw new Exception("state constraint could not be removed");
//					}catch( Exception e ){
//						e.printStackTrace();
//						System.exit(1);
//					}
//				}
//			}else{
//				System.out.println( backup._o2._o2 );
//			}
			//even though the backup is assigned to v[j-1]
			//the backup for v[j-2] will not use these values if visited=0
			//changed - 5/1
			//i want to assign only those to V(j-1) that have different values than
			//that of the heuristic
			//these will also be marked as visited\
			//the error parameter controls the width of the tree
			//e.g. the difference in value must be atleast as large as the difference
			//in the on trajectory state
//			if( j-1 == 0 && ( ( _manager.evaluate(target_val, trajectory_states[j-1].getFactoredState() ).getMax()
//					) - ( _manager.evaluate(_valueDD[j-1], trajectory_states[j-1].getFactoredState() ).getMax() ) ) != 0 ){
//				_successful_update++;
//			}
//			if( j-1 == 0 &&
//					!_policyDD[j-1].equals(target_policy) ){
//				++successful_policy_update ;
//			}
//			
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

			//take all solved states too?
			ADDRNode new_solved  = null;
			ADDRNode current_partition = null;
			if( do_Xion ){
				current_partition = findNewGeneralizedPartition( new_val,
						new_policy, update_states, depth, actual_state, next_state );
//				System.out.println( depth + " " + 
//						_manager.enumeratePathsBDD(update_states) + " " + 
//						_manager.enumeratePathsBDD(current_partition) );
				current_partition = _manager.BDDIntersection( current_partition, update_states );
				
				
//				if( _enableLabelling ){
//					current_partition = _manager.BDDUnion(current_partition, new_solved );
//				}
			}else{
				current_partition = update_states;
			}
			
			if( _enableLabelling ){
				new_solved = getSolvedStates( depth, current_partition );
			}

			if( !_manager.evaluate(current_partition, actual_state.getFactoredState() ).equals(_manager.DD_ONE) ){
				try{
					throw new Exception("current partition does not contain actual state" );
				}catch( Exception e ) {
					e.printStackTrace();
					System.exit(1);
				}
			}

			final ADDRNode newly_updated_values = _manager.BDDIntersection( new_val, current_partition );
			_valueDD[ depth ]  =
					_manager.apply( 
							newly_updated_values,
							_manager.BDDIntersection( _valueDD[depth], _manager.BDDNegate(current_partition)),
							DDOper.ARITH_PLUS );

			ADDRNode newly_updated_policy = _manager.BDDIntersection(new_policy, current_partition);
			_policyDD[ depth ] =
					_manager.BDDUnion( newly_updated_policy ,
							_manager.BDDIntersection( _policyDD[depth], _manager.BDDNegate(current_partition) ) );

			if( _markVisited ){
				visit_node(current_partition, depth);
			}
			if( _enableLabelling ){
				mark_node_solved( new_solved, depth);
			}
			
			saveValuePolicy();
//			System.out.println( "gen state " + depth + " " + _manager.enumeratePathsBDD(current_parition).toString() );
//			System.out.println( "updated state " + depth + " " + _manager.enumeratePathsBDD(update_states).toString() );
//			System.out.println( depth + " " + 
//					_manager.enumeratePathsBDD(current_parition).iterator().next().size()/(1.0d*_mdp.getNumStateVars()) );
//			
		}
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

	protected ADDRNode getSolvedStates( final int depth, final ADDRNode update_states ) {
		//solved states in next layer
		//S(s) = \forall_s',a [ T(s,a,s') => S(s') ]
		//S = \forall_{X',A} \or ~T_i \or S' 
		//push X' inside
		//S_X = \forall_{X',A} [ T \or ~X => S' \and X ] 
		//= \forall_{X',A} [ ( ~T \and X ) \or ( S' \and X ) ]
		//= \forall_{X',A}[ ([\or ~T_i] \and X) \or ( S' \and X ) ]
		//= \forall_{X',A}[ [\or (~T_i \and X)] \or (S' \and X) ]
		//X=0 => S_X = 0
		//push X_i' inside till T_i
		
		final NavigableMap<String, ADDRNode> transition_relation = _mdp.getTransitionRelationFAR();
		ADDRNode new_solved = _manager.remapVars( _solved[depth+1]  , _mdp.getPrimeRemap() );
		new_solved = _manager.BDDIntersection( new_solved, update_states );
		for( final String next_var : _mdp.getSumOrder() ){
			ADDRNode this_trans = transition_relation.get( next_var );
			ADDRNode this_trans_neg = _manager.BDDNegate(this_trans);
			ADDRNode this_trans_neg_and = _manager.BDDIntersection( this_trans_neg, update_states );
			new_solved = _manager.BDDUnion( new_solved, this_trans_neg_and );
			new_solved = _manager.quantify(new_solved, next_var, DDQuantify.UNIVERSAL);
		}
		//marginalize actions
		new_solved = _manager.quantify(new_solved, _mdp.get_actionVars(), DDQuantify.UNIVERSAL);
		return new_solved;
	}

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

	protected void saveValuePolicy() {
		_genaralizeParameters.set_valueDD(_valueDD);
		_genaralizeParameters.set_policyDD( _policyDD );
//		_genaralizeParameters.set_visited(_visited);
		
		final P inner_params = _genaralizeParameters.getParameters();
		if( inner_params != null ){
			inner_params.set_valueDD(_valueDD);
			inner_params.set_policyDD(_policyDD);
		}
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
//		else if( cmd.getOptionValue("generalization").equals("reward") ){
//			generalizer = new RewardGeneralization();
//		}
		
		
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
		
		final String[] const_options = ( cmd.getOptionValues("consistencyRule") );
		final Consistency[] consistency = new Consistency[ const_options.length ];
		for( int i = 0 ; i < const_options.length; ++i ){
		    consistency[i] = Consistency.valueOf(const_options[i]);
		}
		 
		final double timeOut = Double.parseDouble( cmd.getOptionValue("timeOutMins") );
		System.out.println("Timeout " + timeOut );
		
		return new SymbolicRTDP(
				cmd.getOptionValue("domain"), cmd.getOptionValue("instance"),
				Double.parseDouble( cmd.getOptionValue("convergenceTest") ), 
				DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, 
				Boolean.parseBoolean( cmd.getOptionValue( "discounting" ) ), 
				Integer.parseInt( cmd.getOptionValue("testStates") ), 
				Integer.parseInt( cmd.getOptionValue( "testRounds" ) ),
				constrain_naively,
				Boolean.parseBoolean( cmd.getOptionValue("doApricodd") ), 
				epsilons,
				APPROX_TYPE.valueOf( cmd.getOptionValue("apricoddType") ), 
				INITIAL_STATE_CONF.valueOf( cmd.getOptionValue("initialStateConf") ),
				Double.parseDouble( cmd.getOptionValue("initialStateProb") ),
				Integer.parseInt( cmd.getOptionValue("numTrajectories") ),
				timeOut,
				Integer.parseInt( cmd.getOptionValue("stepsLookahead") ),
				generalizer, 
				inner_params, 
				!Boolean.parseBoolean( cmd.getOptionValue("generalizeStates") ),
				!Boolean.parseBoolean( cmd.getOptionValue("generalizeActions") ),
				Integer.parseInt( cmd.getOptionValue("limitGeneralizedStates") ),
				Integer.parseInt( cmd.getOptionValue("limitGeneralizedActions") ),
				GENERALIZE_PATH.valueOf( cmd.getOptionValue("generalizationRule") ), 
				consistency,
				new Random( topLevel.nextLong() ),
				Integer.parseInt( cmd.getOptionValue("onPolicyDepth") ),
				LearningRule.valueOf( cmd.getOptionValue("learningRule") ),
				Integer.valueOf( cmd.getOptionValue("maxRules") ),
				LearningMode.valueOf( cmd.getOptionValue("learningMode")  ),
				Boolean.parseBoolean( cmd.getOptionValue("do_Xion") ),
				Boolean.parseBoolean( cmd.getOptionValue("stat_vfn") ),
				GLOBAL_INITIALIZATION.valueOf( cmd.getOptionValue("global_init") ),
				LOCAL_INITIALIZATION.valueOf( cmd.getOptionValue("local_init") ) ,
				Boolean.valueOf( cmd.getOptionValue("truncate_trials") ),
				Boolean.valueOf( cmd.getOptionValue("mark_visited") ),
				Boolean.valueOf( cmd.getOptionValue("mark_solved") ),
				REMEMBER_MODE.valueOf( cmd.getOptionValue("remember_mode") ),
				Boolean.valueOf( cmd.getOptionValue("init_reward") ) );
		
		}catch( Exception e ){
			HelpFormatter help = new HelpFormatter();
			help.printHelp("symbolicRTDP", options);
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

//	@Override
//	public boolean is_node_solved(FactoredState<RDDLFactoredStateSpace> state,
//			int depth) {
//		try{
//			throw new UnsupportedOperationException();
//		}catch( Exception e ){
//			e.printStackTrace();
//			System.exit(1);
//		}
//		return false;
//	}

}