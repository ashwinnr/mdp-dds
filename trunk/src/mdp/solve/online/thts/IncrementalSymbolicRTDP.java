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
import mdp.solve.online.thts.IncrementalSymbolicRTDP.START_STATE;
import mdp.solve.online.thts.SymbolicRTDP.LearningMode;
import mdp.solve.online.thts.SymbolicRTDP.LearningRule;
import mdp.solve.online.thts.SymbolicTHTS.GLOBAL_INITIALIZATION;
import mdp.solve.online.thts.SymbolicTHTS.LOCAL_INITIALIZATION;
import mdp.solve.online.thts.SymbolicTHTS.REMEMBER_MODE;

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

//	public static enum START_STATE{
//		FROM_SPECIFIC,CONCRETE_STATE
//	}
//	private START_STATE _firstState;
//	
//	public static enum SECOND_STATE{
//		LGG, MORE_GENERAL
//	}
//	private SECOND_STATE _secondState;
//	
//	protected boolean _ignoreIncrementally;
//	
//	public enum ON_TIMEOUT{
//		QUIT, MAX
//	}
	
//	public static enum STOPPING_CONDITION{
//		INVARIANCE,TIMEOUT,INVARIANCE_AND_TIMEOUT
//	}
//	private STOPPING_CONDITION _stop_mode;
//	
//	public static enum REMOVE_VAR_CONDITION{
//		RANDOM, LOWEST_FROM_LGG, LOWEST_FROM_ORDER
//	}
//	private REMOVE_VAR_CONDITION _remove_mode;

	private Random _rand;
//	private int _max_ignore_vars;
//	private int succesful_generalization;

	public IncrementalSymbolicRTDP(
			final String domain,
			final String instance,
			final double epsilon,
			final DEBUG_LEVEL debug,
			final ORDER order,
			final boolean useDiscounting,
			final int numStates,
			final int numRounds,
			final boolean constrain_naively,
			final boolean do_apricodd,
			final double[] apricodd_epsilon,
			final APPROX_TYPE apricodd_type,
			final INITIAL_STATE_CONF init_state_conf,
			final double init_state_prob,
			final int nTrials,
			final double timeOutMins,
			final int steps_lookahead,
			final Random topLevel,
			final int onPolicyDepth,
			final mdp.solve.online.thts.SymbolicRTDP.LearningRule learningRule,
			final int maxRulesToLearn,
			final mdp.solve.online.thts.SymbolicRTDP.LearningMode learningMode, 
			final GLOBAL_INITIALIZATION global_init, 
			final LOCAL_INITIALIZATION local_init, 
			final boolean truncate_trials, final boolean mark_visited, 
			final boolean mark_solved, final REMEMBER_MODE remember_mode, 
			final boolean reward_init ){
		super( domain, instance, epsilon, debug, order, useDiscounting, numStates,
				numRounds, constrain_naively, do_apricodd, apricodd_epsilon,
				apricodd_type, init_state_conf, init_state_prob, nTrials, timeOutMins,
				steps_lookahead, null, null,
				false, false, -1, -1, null, null, topLevel, 
				onPolicyDepth, learningRule, maxRulesToLearn,
				learningMode, true, false, global_init, local_init, truncate_trials, mark_visited, mark_solved, 
				remember_mode, reward_init );
		_rand = new Random( topLevel.nextLong() );
	}

	@Override
	protected void update_generalized_trajectory(
			final FactoredState<RDDLFactoredStateSpace>[] trajectory_states, 
			final FactoredAction<RDDLFactoredStateSpace,RDDLFactoredActionSpace>[] trajectory_actions, 
			final ADDRNode[] trajectory, int num_states) {
		
		if( !_genStates ){
			try{
				throw new Exception("dont run this class for no generalization");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
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
			
			UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> backup = null;
			if( j-1 < _onPolicyDepth ){
				backup = updatePath( 
						target_val, target_policy, source_val, 
						BACKUP_TYPE.VI_FAR,  do_apricodd, 
						do_apricodd ? ( _stationary_vfn ? apricodd_epsilon[0] : apricodd_epsilon[j-1] ) : 0 , 
						apricodd_type, true, -1 , CONSTRAIN_NAIVELY, null, 
						trajectory_states[j-1], trajectory_actions[j-1], j  );
			}else{
				backup = updatePath( target_val, target_policy, source_val, 
					BACKUP_TYPE.VI_FAR, do_apricodd, 
					do_apricodd ? ( _stationary_vfn ? apricodd_epsilon[0] : apricodd_epsilon[j-1] ) : 0, 
					apricodd_type, true, -1 , CONSTRAIN_NAIVELY, target_policy,
					trajectory_states[j-1], trajectory_actions[j-1], j );
			}
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
			final FactoredAction<RDDLFactoredStateSpace,RDDLFactoredActionSpace> actual_action,
			final int depth  ){

		ADDRNode current_value_path = getValueGenState( target_val, actual_state, actual_action );
		ADDRNode current_policy_path = getPolicyGenState( target_policy, actual_state, actual_action );
		ADDRNode current_lgg = getLeastGeneralGeneralization( current_value_path, current_policy_path );

		
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
		//a generalized update is good if it 
		//strictly reduces the size of the lgg
		if( new_lgg.equals(old_lgg) ){
			return false;
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
			final Set<String> ignore_vars ) {
		
		if( current_generalization_vars.isEmpty() ){
			return null;
		}
		if( ignore_vars.isEmpty() ){
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
			list_ordering = _manager.getOrdering();
			for( int i =  list_ordering.size()-1 ; i >= 0 ; --i ){
				final String s = list_ordering.get(i);
				if( current_generalization_vars.contains(s) && 
						_mdp.isStateVariable( s ) && ignore_vars.contains(s) ){
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
				Integer.parseInt( cmd.getOptionValue("maxIgnoreDepth") ) );
		
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
