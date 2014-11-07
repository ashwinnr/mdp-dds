package mdp.solve.online.thts;

import java.util.Arrays;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import add.ADDRNode;

import rddl.RDDL.Bernoulli;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import util.InstantiateArgs;
import util.InternedArrayList;
import util.Timer;
import util.UnorderedPair;
import dd.DDManager.APPROX_TYPE;
import dd.DDManager.DDQuantify;
import dtr.add.ADDDecisionTheoreticRegression.BACKUP_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;
import mdp.generalize.trajectory.EBLGeneralization;
import mdp.generalize.trajectory.Generalization;
import mdp.generalize.trajectory.OptimalActionGeneralization;
import mdp.generalize.trajectory.RewardGeneralization;
import mdp.generalize.trajectory.ValueGeneralization;
import mdp.generalize.trajectory.GenericTransitionGeneralization.Consistency;
import mdp.generalize.trajectory.parameters.EBLParams;
import mdp.generalize.trajectory.parameters.GeneralizationParameters;
import mdp.generalize.trajectory.parameters.OptimalActionParameters;
import mdp.generalize.trajectory.parameters.RewardGeneralizationParameters;
import mdp.generalize.trajectory.parameters.ValueGeneralizationParameters;
import mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH;
import mdp.generalize.trajectory.type.GeneralizationType;
import mdp.solve.online.Exploration;
import mdp.solve.online.thts.IncrementalSymbolicRTDP.REMOVE_VAR_CONDITION;
import mdp.solve.online.thts.IncrementalSymbolicRTDP.START_STATE;
import mdp.solve.online.thts.IncrementalSymbolicRTDP.STOPPING_CONDITION;
import mdp.solve.online.thts.SymbolicRTDP.LearningMode;
import mdp.solve.online.thts.SymbolicRTDP.LearningRule;

//this class implements an iterative update scheme
//for updating the value of a path p(set of states)
//and with the invariance condition.
//it updates the path via a sequence of increasingly 
//general paths(between s to p) 
//and stops when invariance is violated.
//point : how to pick variables? any path that if updated could cause reduction
//simplest such path : 1-bit off at lowest bit
//valuepolicy path : vars ignored by both value and policy(only value/only polcy)
//optional : time limit can be imposed.
//the update tracks the amount of time spent per path
//normalized by path length and terminates before 
//timeout is expired
//optinal : labelling of invariant paths
public class IncrementalSymbolicRTDP< T extends GeneralizationType, 
P extends GeneralizationParameters<T> > extends SymbolicRTDP<T,P> {

	public static enum START_STATE{
		FROM_SPECIFIC,CONCRETE_STATE
	}
	private START_STATE _init_mode;
	public static enum STOPPING_CONDITION{
		INVARIANCE,TIMEOUT,INVARIANCE_AND_TIMEOUT
	}
	private STOPPING_CONDITION _stop_mode;
	public static enum REMOVE_VAR_CONDITION{
		RANDOM,FROM_LGG,LOWEST_ORDER
	}
	private REMOVE_VAR_CONDITION _remove_mode;
	private double _time_per_state;
	private Random _rand;
	private int _max_ignore_vars;
	private int succesful_generalization;

	public IncrementalSymbolicRTDP(
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
			int nTrials,
			double timeOutMins,
			int steps_lookahead,
			Random topLevel,
			int onPolicyDepth,
			mdp.solve.online.thts.SymbolicRTDP.LearningRule learningRule,
			int maxRulesToLearn,
			mdp.solve.online.thts.SymbolicRTDP.LearningMode learningMode, 
			START_STATE init_mode, STOPPING_CONDITION stop_mode, 
			REMOVE_VAR_CONDITION remove_mode,
			final int max_ignore_vars, final double time_per_state ) {
		super(domain, instance, epsilon, debug, order, useDiscounting, numStates,
				numRounds, constrain_naively, do_apricodd, apricodd_epsilon,
				apricodd_type, init_state_conf, init_state_prob, nTrials, timeOutMins,
				steps_lookahead, null, null,
				false, false, -1, -1, null, null, topLevel, 
				onPolicyDepth, learningRule, maxRulesToLearn,
				learningMode, true, false);
		_init_mode = init_mode;
		_stop_mode = stop_mode;
		_remove_mode = remove_mode;
		_rand = new Random( topLevel.nextLong() );
		_max_ignore_vars = max_ignore_vars;
		_time_per_state = time_per_state;
	}
	
	protected void update_generalized_trajectory(
			final FactoredState<RDDLFactoredStateSpace>[] trajectory_states, 
			final FactoredAction<RDDLFactoredStateSpace,RDDLFactoredActionSpace>[] trajectory_actions, 
			final ADDRNode[] trajectory, int num_states) {
		
		int i = (num_states-1)*2;//-1;
		int j = num_states-1;
//		System.out.println("num states " + num_states );
			
		while( i >= 2 ){
		
//	    	System.out.println("Updating trajectories " + j );
	    	
	    	ADDRNode this_next_states, this_states, this_actions;
	    	ADDRNode source_val, target_val, target_policy;

	    	this_next_states = trajectory[i--];
			this_actions = trajectory[i--];
			this_states = trajectory[i];
		    
			target_val = _stationary_vfn ? _valueDD[0] : _valueDD[ j-1 ];
			target_policy = _stationary_vfn ? _policyDD[0] : _policyDD[ j-1 ];
			
			source_val = _stationary_vfn ? _valueDD[0] : _valueDD[ j ];
			
			_DPTimer.ResumeTimer();
			UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> backup = null;
			if( _genStates ){
//				final int index_cur_partition = _dtr.addStateConstraint( 
//						_manager.getRandomSubset( trajectory_states[ j-1 ].getFactoredState() ) ); 
//				
				if( j-1 < _onPolicyDepth ){
					backup = updatePath( 
							target_val, target_policy, source_val, 
							BACKUP_TYPE.VI_FAR, 
							do_apricodd, 
							do_apricodd ? ( _stationary_vfn ? apricodd_epsilon[0] : apricodd_epsilon[j-1] ) : 0 , 
								apricodd_type, true, -1 , CONSTRAIN_NAIVELY, null, 
								trajectory_states[j-1], trajectory_actions[j-1], j );
				}else{
					backup = updatePath( target_val, target_policy, source_val, 
							BACKUP_TYPE.VI_FAR, 
							do_apricodd, 
							do_apricodd ? ( _stationary_vfn ? apricodd_epsilon[0] : apricodd_epsilon[j-1] ) : 0, 
									apricodd_type, true, -1 , CONSTRAIN_NAIVELY, target_policy,
									trajectory_states[j-1], trajectory_actions[j-1], j  );
				}
				
			}else{
				backup = _dtr.backup( target_val, target_policy, source_val, this_states, BACKUP_TYPE.VI_FAR, 
						do_apricodd, 
						do_apricodd ? ( _stationary_vfn ? apricodd_epsilon[0] : apricodd_epsilon[j-1] ) : 0, 
								apricodd_type, true, -1, 
								CONSTRAIN_NAIVELY , null  );
//				System.out.println( backup._o2._o2 );
			}
			_DPTimer.PauseTimer();
			_valueDD[j-1] = backup._o1;
			_policyDD[j-1] = backup._o2._o1;
			saveValuePolicy();
			--j;
		}
//		System.out.println(succesful_generalization);
//		succesful_generalization = 0;
	}

	private UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> updatePath(
			final ADDRNode target_val, 
			final ADDRNode target_policy, 
			final ADDRNode source_val,
			final BACKUP_TYPE bType, 
			final boolean do_apricodd,
			final double apricodd_epsilon, 
			final APPROX_TYPE apricodd_type, 
			final boolean make_policy, final long MB,
			final boolean constrain_naively, 
			final ADDRNode policy_constraint, 
			final FactoredState<RDDLFactoredStateSpace> actual_state, 
			FactoredAction<RDDLFactoredStateSpace,RDDLFactoredActionSpace> actual_action,
			final int depth ){

		ADDRNode current_value_path = getValueGenState( target_val, actual_state, actual_action );
		ADDRNode current_policy_path = getPolicyGenState( target_policy, actual_state, actual_action );
		ADDRNode current_lgg = getLeastGeneralGeneralization( current_value_path, current_policy_path );
		final ADDRNode save_lgg = current_lgg;
		
		//
		final ADDRNode initial_path = InitializeGenState( 
				actual_state,
				actual_action,
				depth,
				current_value_path, current_policy_path );
		ADDRNode best_seen_generalization = null;
		final Set<String> best_seen_generalization_vars = _manager.getVars(initial_path).get(0);
		//current generalized state being updated 
		//INVARIANT : current_path => ~updated_states
		//BDD to maintain updated states
		//INVARIANT : updated_states => this_states
		//INVARIANT : actual_state => updated_states
		//USE :  to avoid double updates
		ADDRNode updated_states = _manager.DD_ZERO;
		boolean done = false;
		double time_per_extension = 0.0d, elapsedTimeInMinutes = 0d;
		final Timer update_time = new Timer();
		int num_missing_vars = 0;
		ADDRNode updated_V = target_val, updated_pi = target_policy;
		String next_variable = null;
		ADDRNode next_generalization = initial_path;
		int num_tried_vars = 0;
		
		while ( !done ){

			update_time.ResumeTimer();
			ADDRNode next_gen_uniq = 
					_manager.BDDIntersection( next_generalization, 
							_manager.BDDNegate(updated_states) ) ;
//			System.out.println("updating " + _manager.enumeratePathsBDD(next_generalization).iterator().next().toString() );
			UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> backup 
				= _dtr.backup( updated_V, updated_pi, source_val, 
						next_gen_uniq, BACKUP_TYPE.VI_FAR, 
						do_apricodd, 
						do_apricodd ? apricodd_epsilon  : 0, 
						apricodd_type, true, -1, 
						CONSTRAIN_NAIVELY , null  );
			update_time.PauseTimer();

			final ADDRNode new_value_path = getValueGenState(backup._o1, actual_state, actual_action);
			final ADDRNode new_policy_path = getPolicyGenState(backup._o2._o1, actual_state, actual_action);
			ADDRNode new_lgg = getLeastGeneralGeneralization( new_value_path, 
					new_policy_path );
			
			final boolean is_invariant = 
					next_variable == null ? true : 
					checkInvariance( new_lgg, current_lgg ) ;
				
			if( !is_invariant && (next_variable != null ) ){
				//path did not change
				//no generalization
//				System.out.println("no change - reverting " + next_variable );
				backup._o1 = null;
				backup._o2 = null;
				backup = null;
				new_lgg = null;
				next_generalization = best_seen_generalization;
				best_seen_generalization_vars.remove(next_variable);
				
				if( _stop_mode.equals(STOPPING_CONDITION.INVARIANCE) || 
						_stop_mode.equals(STOPPING_CONDITION.INVARIANCE_AND_TIMEOUT) ){
					done = true;
//					System.out.println("failed invariance test - stopping LDS");
					break;
				}
				//fallout of if to pick nexxt variable

			}else if( is_invariant || ( next_variable == null ) ){
				//take shorter path
				//can become longer
				//best path => new path
				updated_V = backup._o1;
				updated_pi = backup._o2._o1;
				updated_states = _manager.BDDUnion(updated_states, next_generalization );

				//during the first backup
				//the new paths will be the concrete state
				best_seen_generalization = next_generalization;
				current_value_path = new_value_path;
				current_policy_path = new_policy_path;
				current_lgg = new_lgg;
				
				if( next_variable != null ){
//					System.out.println("invariance holds for ignoring " + next_variable );
					best_seen_generalization_vars.remove(next_variable);
					++num_missing_vars;
				}
			}
				
			//next generalization
			if( ( _max_ignore_vars==-1 && num_tried_vars == _mdp.getNumStateVars() ) ||
					( _max_ignore_vars!=-1 && num_tried_vars == _max_ignore_vars ) ){
				done = true;
//				System.out.println("tried all variables to ignore (or) max depth reached");
				break;
			}
			next_variable = pickNextVariable(best_seen_generalization_vars, 
					actual_state, actual_action, save_lgg);
//			System.out.println("ignore " + next_variable );
			
			if( next_variable == null ){
//				System.out.println("no variable to be ignored");
				done = true;
				break;
			}
			
			++num_tried_vars;
			next_generalization = 
					_manager.quantify(best_seen_generalization, next_variable, 
							DDQuantify.EXISTENTIAL );

			final double new_elapsed_time = update_time.GetElapsedTimeInMinutes();
			time_per_extension = updateTimeEstimate( time_per_extension, elapsedTimeInMinutes, 
					new_elapsed_time , (int)Math.pow(2,num_missing_vars+1-1) );
			
//			if( !isThereEnoughTime( time_per_extension, (int)Math.pow(2d, num_missing_vars+1-1),
//					_time_per_state-elapsedTimeInMinutes ) ){
//				done = true;
//				System.out.println("timeout predicted");
//				break;
//			}
			elapsedTimeInMinutes = new_elapsed_time;
			if( best_seen_generalization_vars.isEmpty() ) {
				done = true;
			}
			
				
			if ( _time_per_state != -1 && elapsedTimeInMinutes >= _time_per_state ){
					done = true;
//					System.out.println("timeout happened");
					break;
			}
			
		}
		
		if( num_missing_vars > 0 ){
//			System.out.println("ignored total " + num_missing_vars );
			++succesful_generalization;
		}
		
		return new UnorderedPair<>(updated_V,new UnorderedPair<>(updated_pi,0d));

	}

	private ADDRNode getPolicyGenState(
			final ADDRNode policy_bdd,
			final FactoredState<RDDLFactoredStateSpace> actual_state,
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> actual_action) {
		final ADDRNode state_dd = _manager.restrict(policy_bdd, actual_action.getFactoredAction() );
		return _manager.get_path( state_dd, actual_state.getFactoredState() );
	}

	private ADDRNode getValueGenState(
			final ADDRNode value_add,
			FactoredState<RDDLFactoredStateSpace> actual_state,
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> actual_action) {
		return _manager.get_path(value_add, actual_state.getFactoredState() );
	}

	private boolean isThereEnoughTime(
			final double time_per_extension, 
			final int num_next_extensions, final double remaining_time ) {
		return ( time_per_extension * num_next_extensions < remaining_time );
	}

	private double updateTimeEstimate(
			final double time_per_extension,
			final double elapsedTimeInMinutes, 
			final double new_elapsed_time,
			final int num_max_updated_extensions ) {
		final double this_avg = (new_elapsed_time-elapsedTimeInMinutes)/num_max_updated_extensions;
		//old avg = ( some time )/ n
		//new avg = ( some time + new time ) / 2n = old_avg/2 + new avg/2
		return 0.5d*( time_per_extension + this_avg );
	}

	private boolean checkInvariance(ADDRNode new_lgg, ADDRNode old_lgg) {
		if( new_lgg.equals(old_lgg) ){
			return true;
		}
		return _manager.BDDImplication(old_lgg, new_lgg).equals(_manager.DD_ONE);
	}

	private ADDRNode getLeastGeneralGeneralization(final ADDRNode gen_1,
			final ADDRNode gen_2) {
		//assuming inputs are path as of now
		final NavigableMap<String, Boolean> path_1 = _manager.enumeratePathsBDD(gen_1).iterator().next();
		final NavigableMap<String, Boolean> path_2 = _manager.enumeratePathsBDD(gen_2).iterator().next();
		final NavigableMap<String, Boolean> lgg_path = Maps.newTreeMap( );
		for( final String s : path_1.keySet() ){
			final Boolean path_1_value = path_1.get(s);
			final Boolean path_2_value = path_2.get(s);
			if( path_2_value != null && path_2_value.equals( path_1_value ) ){
				if( !s.equals("1.0") ){
					lgg_path.put(s, path_1_value );	
				}
			}
		}
		return _manager.getProductBDDFromAssignment(lgg_path);
	}

//	private boolean checkInvariance(
//			final UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> backup,
//			final FactoredState<RDDLFactoredStateSpace> actual_state,
//			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> actual_action) {
//		//check invariance wrt more general abstraction
//		//1. since the search was started with the more 
//		//specific abstraction
//		//2. variable are only ignored
//		//3. 
//		final ADDRNode new_path = getMoreGeneralAbstractState( backup._o1, backup._o2._o1, 
//				actual_state, actual_action );
//		
//	}

	private ADDRNode InitializeGenState(
			final FactoredState<RDDLFactoredStateSpace> actual_state,
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> actual_action,
			final int depth,
			final ADDRNode value_path_gen , 
			final ADDRNode policy_path_gen ) {
		saveValuePolicy();
		ADDRNode ret = null;
		
		switch( _init_mode ){
		case FROM_SPECIFIC : 
			ret = getMoreSpecificAbstraction( value_path_gen, policy_path_gen );
			break;
		case CONCRETE_STATE : 
			ret = _manager.getProductBDDFromAssignment( actual_state.getFactoredState() );
			break;
		}
		return ret;
	}
	
	

	private ADDRNode getMoreSpecificAbstraction(final ADDRNode value_path_gen,
			final ADDRNode policy_path_gen) {
		//sanity
		if( _manager.BDDIntersection(value_path_gen, policy_path_gen).equals(_manager.DD_ZERO) ){
			try{
				throw new Exception("gen states are contradictory");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		//x => y means x is more specific
		if( _manager.BDDImplication(value_path_gen, 
				policy_path_gen).equals(_manager.DD_ONE)){
			return policy_path_gen;
		}else if( _manager.BDDImplication(policy_path_gen,
				value_path_gen ).equals( _manager.DD_ONE ) ){
			return value_path_gen;
		}
		return null;
	}

//	private ADDRNode getPolicyGenState(
//			final FactoredState<RDDLFactoredStateSpace> actual_state,
//			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> actual_action,
//			final int depth) {
//		final ADDRNode state_dd = _manager.restrict(_policyDD[depth], actual_action.getFactoredAction() );
//		return _manager.get_path(state_dd, actual_state.getFactoredState() );
//	}
//
//	private ADDRNode getValueGenState(
//			final FactoredState<RDDLFactoredStateSpace> actual_state,
//			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> actual_action,
//			final int depth) {
//		saveValuePolicy();
//		return _manager.get_path(_valueDD[depth], actual_state.getFactoredState() );
//	}

	private String pickNextVariable(
			final Set<String> current_generalization_vars,
			final FactoredState<RDDLFactoredStateSpace> actual_state,
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> actual_action, 
			final ADDRNode lgg ) {
		
		if( current_generalization_vars.isEmpty() ){
			return null;
		}
		
		String ret = null;
		InternedArrayList<String> list_ordering;
		switch( _remove_mode ){
		case LOWEST_ORDER : 
			final InternedArrayList<String> ord = _manager.getOrdering();
			for( int i = ord.size()-1; i >= 0 ; --i ){
				final String this_state_var = ord.get(i);
				if( current_generalization_vars.contains( this_state_var ) ){
					ret = this_state_var;
					break;
				}
			}
			break;
		case RANDOM :
			ret = getRandomElementFromSet( current_generalization_vars, _rand );
			break;
		case FROM_LGG : 
			final Set<String> lgg_vars = _manager.getVars( lgg ).iterator().next();
			list_ordering = _manager.getOrdering();
			for( int i =  list_ordering.size()-1 ; i >= 0 ; --i ){
				final String s = list_ordering.get(i);
				if( current_generalization_vars.contains(s) && 
						_mdp.isStateVariable( s ) && !lgg_vars.contains(s) ){
					ret = s;
					break;
				}
			}
			break;
		}
//		System.out.println("ignore " + ret );
		return ret;
	}

	public static <T> T getRandomElementFromSet(
			final Set<T> some_set, 
			final Random rand ) {
		final int size = some_set.size();
		T ret = null;
		final int index = rand.nextInt( size );
		int i = 0;
		
		for( final T thing : some_set ){
			if( i == index ){
				ret = thing;
				break;
			}
		}
		
		return ret;
	}
	
	protected ADDRNode[] generalize_trajectory(
			final FactoredState<RDDLFactoredStateSpace>[] trajectory_states, 
			final FactoredAction<RDDLFactoredStateSpace,RDDLFactoredActionSpace>[] trajectory_actions) {
		final ADDRNode[] ret = new ADDRNode[ trajectory_states.length + trajectory_actions.length ];
		int i =0;
		for( final FactoredState<RDDLFactoredStateSpace> fs : trajectory_states ){
			ret[i++] = _manager.getProductBDDFromAssignment(fs.getFactoredState());
			if( i != ret.length ){
				ret[i++] = _manager.DD_ONE;
			}
		}
		return ret;
	}
	
	public static IncrementalSymbolicRTDP instantiateMe(String[] args) {
		
		final Options options = InstantiateArgs.createOptions();
		
		try{
			final CommandLine cmd = InstantiateArgs.parseOptions(args, options);
		final Random topLevel = new Random( Long.parseLong( cmd.getOptionValue("seed") ) );
		final boolean constrain_naively = !Boolean.parseBoolean( cmd.getOptionValue("constraintPruning") );
				
		double[] epsilons = null;
		if( Boolean.parseBoolean( cmd.getOptionValue("doApricodd") ) ){
			epsilons = new double[ Integer.parseInt( cmd.getOptionValue("stepsLookahead") ) ];
			double epsilon = Double.parseDouble( cmd.getOptionValue("apricoddError") );
			for( int i = 0 ; i < epsilons.length; ++i ){
				epsilons[i] = epsilon;
			}
		}
		 
		final double timeOut = Double.parseDouble( cmd.getOptionValue("timeOutMins") );
		System.out.println("Timeout " + timeOut );
		
		return new IncrementalSymbolicRTDP( 
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
				new Random( topLevel.nextLong() ),
				Integer.parseInt( cmd.getOptionValue("onPolicyDepth") ),
				LearningRule.valueOf( cmd.getOptionValue("learningRule") ),
				Integer.valueOf( cmd.getOptionValue("maxRules") ),
				LearningMode.valueOf( cmd.getOptionValue("learningMode")  ) ,
				START_STATE.valueOf( cmd.getOptionValue("initGen") ),
				STOPPING_CONDITION.valueOf(cmd.getOptionValue("stopGen") ),
				REMOVE_VAR_CONDITION.valueOf(cmd.getOptionValue("nextGen") ) ,
				Integer.parseInt( cmd.getOptionValue("maxIgnoreDepth") ),
				Double.parseDouble( cmd.getOptionValue("timePerState") ) );
		
		}catch( Exception e ){
			HelpFormatter help = new HelpFormatter();
			help.printHelp("symbolicRTDP", options);
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	public static void main(String[] args) {
		
		System.out.println( Arrays.toString(args) );

		IncrementalSymbolicRTDP incrsrtdp = IncrementalSymbolicRTDP.instantiateMe(args);
		
		Thread t = new Thread( incrsrtdp );
		t.start();
		try {
			t.join();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
