package dtr;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
//import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import java_cup.production;

import javax.print.attribute.standard.Sides;

import rddl.EvalException;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_NAME;
import rddl.State;
import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import util.Pair;
import util.Timer;
import util.UnorderedPair;
import add.ADDINode;
import add.ADDLeaf;
import add.ADDManager;
import add.ADDNode;
import add.ADDRNode;
import dd.DDManager.APPROX_TYPE;
import dd.DDManager.DDMarginalize;
import dd.DDManager.DDOper;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredReward;
import factored.mdp.define.FactoredStateSpace;
import factored.mdp.define.FactoredTransition;

public class ADDDecisionTheoreticRegression implements
		SymbolicRegression<ADDNode, ADDRNode, ADDINode, ADDLeaf, RDDLFactoredStateSpace, 
		RDDLFactoredActionSpace> {

	private ADDManager _manager;
	private RDDL2ADD _mdp;
	private DEBUG_LEVEL _dbg;
//	private Set<ADDRNode> _constraints;
	private Random _rand;
	private List<String> __order_action_vars_appear;
	private ArrayList< Integer > __nextVar_to_actionVar_range;
	private ADDRNode __action_precondition = null;
	private Set<ADDRNode> __policy_constraints = new HashSet<ADDRNode>();
	private final ADDRNode __action_constraints;
	private final ADDRNode __state_constraints;

	public ADDDecisionTheoreticRegression(RDDL2ADD mdp, final long seed){
		_manager = mdp.getManager();
		_mdp = mdp;
		_dbg = mdp.__debug_level;
		final ADDRNode action_precon = _manager.productDD(_mdp.getActionPreconditions() );
		__action_constraints = _manager.productDD( _mdp.getActionConstraints() );
		__state_constraints = _manager.productDD( mdp.getStateConstraints() );
		__action_precondition = _manager.productDD( action_precon ) ;
		_rand = new Random( seed );
		setupMBFAR();
	}
	
	private void setupMBFAR() {
		try{
			__order_action_vars_appear = new ArrayList<String>();
			final ArrayList<String> expectation_order = _mdp.getSumOrder();
			__nextVar_to_actionVar_range = new ArrayList<Integer>();		
			for( int i = 0 ; i < expectation_order.size(); ++i ){
				final String xp = expectation_order.get( i );
				
				List<String> src =  _mdp.getAffectingActionVariables( xp );
				src = src == null ? Collections.EMPTY_LIST : src;
				
				final List<String> this_affected_by = new ArrayList<String>( src );
				this_affected_by.removeAll( __order_action_vars_appear );
				__order_action_vars_appear.addAll( this_affected_by );
				__nextVar_to_actionVar_range.add( __order_action_vars_appear.size() );
			}
		}catch( NullPointerException e ){
		}
	}

//	@Override
//	public UnorderedPair< ADDValueFunction, ADDPolicy > regress( final ADDRNode input, 
//			final boolean withActionVars,
//			final boolean keepQ, final boolean makePolicy,
//			final boolean constrain_naively,
//			final List<Long> size_change ) {
////		final ADDRNode withoutNegInf = _manager.apply(input, _manager.DD_ZERO,
////				DDOper.ARITH_MAX );
//		
//	}
	
	public ADDRNode applyMDPConstraintsNaively( final ADDRNode input,
			final NavigableMap<String, Boolean> action,
			final ADDRNode violate,
			final List<Long> size_chage ){
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
			System.out.println("applying constraint by multiplication");
		}
		
		if( size_chage != null ){
			size_chage.addAll( _manager.countNodes( input ) ) ;
		}
		ADDRNode all_constraints = _manager.productDD( 
				__action_precondition, __state_constraints,
				_manager.productDD( __policy_constraints, 
				__action_constraints ) );
		
		if( action != null ){
			all_constraints = _manager.restrict(all_constraints, action);
		}
		all_constraints = convertToNegInfDD( all_constraints );
		ADDRNode ret = _manager.apply(input, all_constraints, DDOper.ARITH_PLUS );
		if( size_chage != null ){
			size_chage.addAll( _manager.countNodes( ret ) ) ;
		}
			
//		;
//		for( ADDRNode constraint : _constraints ){
//			ADDRNode action_constraint;
//			if( action != null ){
//				action_constraint = _manager.restrict(constraint, action);
//			}else{
//				action_constraint = constraint;
//			}
//			action_constraint = _manager.remapLeaf( action_constraint, _manager.DD_ZERO, violate );
//			final ADDRNode constrained = _manager.apply(ret, action_constraint, DDOper.ARITH_PROD );
//			
//			if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
//				_manager.showGraph( ret, constraint, constrained );
//				try {
//					System.in.read();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//			ret = constrained;
//		}
		return ret;
	}
	
	@Override
	public ADDRNode applyMDPConstraints(final ADDRNode input,
			final NavigableMap<String, Boolean> action,
			final ADDRNode violate,
			final boolean constrain_naively,
			final List<Long> size_change ) {
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
			System.out.println("applying constraint");
			System.out.println("Size of input " + _manager.countNodes(input) );
		}
		//WARNING: stupidity
		if( constrain_naively ){
			return applyMDPConstraintsNaively(input, action, violate, size_change );
		}
		
		if( size_change != null ){
			size_change.addAll( _manager.countNodes( input ) ) ;
		}
		
		ADDRNode all_constraints = _manager.productDD( 
				__action_precondition, __state_constraints,
				_manager.productDD( __policy_constraints, 
				__action_constraints ) );
		
		if( action != null ){
			all_constraints = _manager.restrict(all_constraints, action);
		}
		
		ADDRNode ret = input;
		ret = _manager.constrain(ret, all_constraints, violate);
		if( size_change != null ){
			size_change.addAll( _manager.countNodes( ret ) ) ;
		}
		
//		for( ADDRNode constraint : _constraints ){
//			ADDRNode action_constraint;
//			
//			ADDRNode constrained = _manager.constrain(ret, action_constraint, violate);
//			
//			if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
//				_manager.showGraph( ret, constraint, constrained );
//				try {
//					System.in.read();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//			ret = constrained;
//			if( size_change != null ){
//				size_change.addAll( _manager.countNodes( constrained ) ) ;
//			}
//		}
		return ret;
	}
	
	//WARNING : this method dumps regression size traces for each action consecutively
	private UnorderedPair<ADDValueFunction, ADDPolicy> regressSPUDD(final ADDRNode primed, 
			final boolean keepQ, final boolean makePolicy,
			final boolean constrain_naively,
			final List<Long> size_change, 
			final boolean do_apricodd, 
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ){
		
		NavigableMap< FactoredAction<RDDLFactoredStateSpace , 
			RDDLFactoredActionSpace>, ADDRNode > q_map = null;
		ADDPolicy policy = null;
		ADDRNode v_func = _manager.DD_NEG_INF;
		
		///sum size of Q-functions
		//mulitply by Q^A
		//is max tree size
		long sum_q = 0;
		
		List<NavigableMap<String, Boolean>> actions = _mdp.getFullRegressionOrder();
		for( NavigableMap<String, Boolean> action : actions ){
			ADDRNode this_q = regressAction( primed, action, constrain_naively, 
					size_change , do_apricodd, apricodd_epsilon, apricodd_type );
			sum_q += _manager.countNodes( this_q ).get( 0 );
			if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.println("Regressing action : " + action );
			}
			
			//not noting down the size of V
			v_func = _manager.apply( v_func, this_q, DDOper.ARITH_MAX );
			_manager.flushCaches( );
			
			if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
				System.out.println("showing diagrams: " + action );
				_manager.showGraph(this_q, v_func);
				try {
					System.in.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if( makePolicy ){
				if( policy == null ){
					policy = new ADDPolicy(
							_manager, _mdp.getFactoredStateSpace(),
							_mdp.getFactoredTransition(), _mdp.getFactoredReward(), _rand.nextLong() );
				}
				policy.updateADDPolicy(v_func, this_q, action);
			}
			
			if( keepQ ){
				if( q_map == null ){
					 q_map = new TreeMap< FactoredAction<RDDLFactoredStateSpace , 
							 RDDLFactoredActionSpace>, ADDRNode >();
				}
				FactoredAction<RDDLFactoredStateSpace , 
				 RDDLFactoredActionSpace> thisAction 
				 	= new FactoredAction<RDDLFactoredStateSpace , 
							 RDDLFactoredActionSpace>(action);
				q_map.put( thisAction, this_q );
			}
			_manager.flushCaches( );
		}
		
		size_change.clear();
		size_change.add( sum_q / actions.size() );
//		sum_q *= actions.size();
				
		return new UnorderedPair<ADDValueFunction, ADDPolicy>( 
				new ADDValueFunction( v_func, q_map, null, _manager )
				, policy);
	}
	
	private UnorderedPair<ADDValueFunction, ADDPolicy> regressAllActions(final ADDRNode primed, 
			final boolean keepQ, final boolean makePolicy, 
			final boolean constrain_naively, final List<Long> size_change ,
			final boolean do_apricodd, 
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ) {

		final ArrayList<String> sum_order = _mdp.getSumOrder();
		System.out.print("Regressing all actions " );
		ADDRNode ret = primed;
		for( String str : sum_order ){
//			if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.print( "*" );// + str );
//			}
			ret = computeExpectation( ret, str, null, constrain_naively, size_change,
					do_apricodd, 
					apricodd_epsilon, apricodd_type );
		}
		System.out.println();
		
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
				&& _manager.hasSuffixVars(ret, "'") ){
			try{
				throw new Exception("has primed var after expectations");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
//		ADDRNode q_func = ret;
//		if( _mdp._bRewardActionDependent ){
		ADDRNode reward_added = discountAndAddReward( ret, null, constrain_naively, size_change,
				do_apricodd, 
				apricodd_epsilon, apricodd_type );
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
			System.out.println( "reward added " );
		}
		if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
			System.out.println( "showing diagrams");
			_manager.showGraph( ret, reward_added);
		}
//		ret = reward_added;
		final ADDRNode improper_q_func = reward_added;
		ADDRNode q_func = improper_q_func;
		if( !constrain_naively ){
			q_func = multiplyActionPreconditions( q_func, null );
			q_func = multiplyStateConstraints( q_func );
			if( makePolicy ){
				q_func = multiplyActionConstraints( q_func, null );
			}
		}
		
		System.out.println("Size of |Q| = " + _manager.countNodes(q_func) );
		
		ret = maxActionVariables(q_func, _mdp.getElimOrder(), size_change,
				do_apricodd, apricodd_epsilon, apricodd_type );
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
				&& _manager.hasVars( ret, _mdp.getFactoredActionSpace().getActionVariables() ) ){
			try{
				throw new Exception("Has primed vars after max");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}

//		if( !_mdp._bRewardActionDependent ){
//			ADDRNode reward_added = discountAndAddReward( ret, null, constrain_naively, size_change );
//			if( makePolicy ){
//				q_func = discountAndAddReward( q_func, null, constrain_naively, 
//						size_change ); 
//			}
//
//			if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
//				System.out.println( "reward added " );
//			}
//			if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
//				System.out.println( "showing diagrams");
//				_manager.showGraph( ret, q_func, reward_added);
//			}
//			ret = reward_added;
//		}
//		ret = maxActionVariables(ret, _mdp.getElimOrder(), null );
		
		if(_dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
				&& _manager.hasVars( ret, _mdp.getFactoredActionSpace().getActionVariables() ) ){
			try{
				throw new Exception("Has action vars after max");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		ADDRNode v_func = ret;
//		//APRICODD
//		if( apricodd_epsilon > 0.0d ){
//			q_func = _manager.approximate(q_func, apricodd_epsilon, apricodd_type);
//			v_func = maxActionVariables(q_func, _mdp.getElimOrder(), size_change);
//			v_func = _manager.approximate(v_func, apricodd_epsilon, apricodd_type);
//		}
		
		ADDPolicy policy = null;
		if( makePolicy ){
			if( policy == null ){
				policy = new ADDPolicy(
						_manager, _mdp.getFactoredStateSpace(),
						_mdp.getFactoredTransition(), _mdp.getFactoredReward(), _rand.nextLong() );
			}
			policy.updateBDDPolicy( v_func, q_func );
		}else{ 
			policy = null;
		}
		
		if( !keepQ ){
			q_func = null;
		}
		
		return new UnorderedPair<ADDValueFunction, ADDPolicy>(
				new ADDValueFunction(v_func, null, q_func, _manager),
				policy);
		
	}

	private ADDRNode multiplyActionPreconditions( final ADDRNode input, 
			final NavigableMap<String, Boolean> action ) {
		ADDRNode constraint = convertToNegInfDD( __action_precondition );
		if( action != null ){
			constraint =_manager.restrict( constraint, action ); 
		}
		final ADDRNode ret = _manager.apply( input, constraint, DDOper.ARITH_PLUS );
		return ret;
	}

	private ADDRNode convertToNegInfDD(final ADDRNode input) {
		ADDRNode ret = _manager.remapLeaf(input, _manager.DD_ZERO, _manager.DD_NEG_INF );
		ret = _manager.remapLeaf(ret, _manager.DD_ONE, _manager.DD_ZERO);
		return ret;
	}

	private ADDRNode maxActionVariables( final ADDRNode input, 
			final ArrayList<String> action_vars,
			final List<Long> size_change,
			final boolean do_apricodd,
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ) {
		ADDRNode ret = input;
		if( size_change != null ){
			size_change.addAll( _manager.countNodes(input) );
		}
		
		for( final String str : action_vars ){
			if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.println("Maxing " + str );
				System.out.println("Size of input " + _manager.countNodes( ret ) );
			}
			final ADDRNode maxd = _manager.marginalize(ret, str, DDMarginalize.MARGINALIZE_MAX);
			final ADDRNode maxd_approx = _manager.doApricodd(maxd, do_apricodd,
					apricodd_epsilon, apricodd_type);
			if( size_change != null ){
				size_change.addAll( _manager.countNodes( maxd, maxd_approx ) );
			}
			if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
				System.out.println("showing diagrams");
				_manager.showGraph(ret, maxd, maxd_approx );
				try {
					System.in.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			ret = maxd_approx;
			_manager.flushCaches( );
		}
		return ret;
	}

	public UnorderedPair< ADDValueFunction, ADDPolicy > regressMBFAR( final ADDRNode input, 
			final boolean makePolicy,
			final boolean constrain_naively,
			final long BIGDD,
			final List<Long> size_change,
			final boolean do_apricodd, 
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ){
		int total_leaves = 0;
		//primed
		final ADDRNode primed = _manager.remapVars(input, _mdp.getPrimeRemap() );
//		final ADDRNode constrained_zero = _manager.remapLeaf( primed , 
//				_manager.DD_NEG_INF, _manager.DD_ZERO );
		
		final ArrayList<String> sum_order = _mdp.getSumOrder();
		ADDRNode value_func = _manager.DD_NEG_INF;
		ADDRNode policy = _manager.DD_ZERO;
		final Deque< UnorderedPair< NavigableMap< String, Boolean >, 
				UnorderedPair<Integer, ADDRNode > > > 
					evaluations = new ArrayDeque<UnorderedPair<NavigableMap<String, Boolean>, UnorderedPair<Integer, ADDRNode>>>();
		evaluations.addFirst( new UnorderedPair< NavigableMap< String, Boolean >, 
				UnorderedPair<Integer, ADDRNode > >( new TreeMap<String, Boolean>(), 
				new UnorderedPair<Integer, ADDRNode>( 0, primed ) ) );
		final ArrayList<String> action_variables = _mdp.getElimOrder();
		final int num_action_vars = action_variables.size();
		boolean recurse = false;
		final List<String> maxOrder = _mdp.getElimOrder();
		
		while( !evaluations.isEmpty() ){
			recurse = false;
			final UnorderedPair<NavigableMap<String, Boolean>, UnorderedPair<Integer, ADDRNode>>
				this_one = evaluations.pollFirst();
			final UnorderedPair<Integer, ADDRNode> source = this_one._o2;
			final NavigableMap<String, Boolean> assign = this_one._o1;
			final ADDRNode source_dd = source._o2;
			final int start_from = source._o1;
			if( _dbg.compareTo( DEBUG_LEVEL.SOLUTION_INFO ) >= 0 ){
				System.out.println( "Regressing on " + assign );
			}
			ADDRNode ret = source_dd;
			for( int i = start_from; i < sum_order.size() && !recurse ; ++i ){
				final ADDRNode after_exp = computeExpectation(ret, 
						sum_order.get(i), assign, constrain_naively, size_change,
						do_apricodd, 
						apricodd_epsilon, apricodd_type );
				ret = after_exp;
				final int size = _manager.getSize( after_exp , false );
				if( size > BIGDD && assign.size() != num_action_vars ){
					//pick one
					String conditioned_on = null;
					do{
						final int upper = __nextVar_to_actionVar_range.get( i );
						if( upper == assign.size() ){
							break;//all action vars that have been introduced 
							//are already enumerated
						}
						final int randInt = _rand.nextInt( upper );
						conditioned_on = __order_action_vars_appear.get( randInt );
					}while( assign.get(conditioned_on) != null );
					
					if( _dbg.compareTo( DEBUG_LEVEL.SOLUTION_INFO ) >= 0 ){
						System.out.println( "Conditioning on " + conditioned_on );
					}
					
					if( conditioned_on != null ){
						//set true
						final NavigableMap<String, Boolean> true_assign 
							= new TreeMap< String, Boolean >( assign );
						true_assign.put( conditioned_on, true );
						final ADDRNode true_restrict 
							= _manager.restrict( after_exp, conditioned_on, true );
						
						//set false
						final NavigableMap<String, Boolean> false_assign
							= new TreeMap<String, Boolean>( assign );
						false_assign.put( conditioned_on, false );
						final ADDRNode false_restrict
							= _manager.restrict( after_exp, conditioned_on, false );
						
						evaluations.addFirst( new UnorderedPair<NavigableMap<String, Boolean>, UnorderedPair<Integer, ADDRNode>>( false_assign, 
								new UnorderedPair<Integer, ADDRNode>( i , false_restrict ) ) );
						evaluations.addFirst( new UnorderedPair<NavigableMap<String, Boolean>, UnorderedPair<Integer, ADDRNode>>( true_assign, 
								new UnorderedPair<Integer, ADDRNode>( i , true_restrict ) ) );
						recurse = true;
					}
				}
			}
			if( recurse ){
				continue;
			}
		
			if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
					&& _manager.hasSuffixVars(ret, "'") ){
				try{
					throw new Exception("has primed var after expectations");
				}catch( Exception e ){
					e.printStackTrace();
					System.exit(1);
				}
			}
			
			ret = discountAndAddReward( ret, assign, constrain_naively, size_change,
					do_apricodd,
					apricodd_epsilon, apricodd_type );
			//fix for incorrectness of partial Q
			if( !constrain_naively ){
				ret = multiplyActionPreconditions( ret, assign );
				if( makePolicy ){
					ret = multiplyActionConstraints( ret, assign );
				}
				ret = multiplyStateConstraints( ret );
			}
			
			//max action vars
			final ArrayList<String> all_action_vars = new ArrayList<String>( maxOrder );
			all_action_vars.removeAll( assign.keySet() );
			final ADDRNode this_z = maxActionVariables( ret, all_action_vars,
					size_change, do_apricodd, apricodd_epsilon, apricodd_type );
			
			//max with value func
			value_func = _manager.apply( value_func, this_z, DDOper.ARITH_MAX );
			
			++total_leaves;
			if( _dbg.compareTo( DEBUG_LEVEL.SOLUTION_INFO ) >= 0 ){
				System.out.println( "Z-value function " + assign );
			}
			
			//update policy
			if( makePolicy ){
				//for unconditioned action vars - Z-V => 0/1
				//then update global policy - (1-pi_z)*old + pi_z*1(assign)
				ADDRNode action_indicator = _manager.DD_ONE;
				for( final String act : assign.keySet() ){
					final boolean value = assign.get(act);
					final ADDRNode this_act = _manager.getIndicatorDiagram(act, value );
					action_indicator = _manager.apply( action_indicator, this_act, DDOper.ARITH_PROD );
				}
				
				final ADDRNode global_opt_states_diff 
					= _manager.apply( value_func, this_z, DDOper.ARITH_MINUS );
				final ADDRNode global_opt_one_states 
					= _manager.threshold(global_opt_states_diff, 0.0d, false );
				
				final ADDRNode this_z_diff = _manager.apply( this_z, ret, DDOper.ARITH_MINUS );
				ADDRNode pi_z = _manager.threshold( this_z_diff, 0.0d, false );
				pi_z = _manager.apply( pi_z, action_indicator, DDOper.ARITH_PROD );
				pi_z = _manager.apply( pi_z, global_opt_one_states, DDOper.ARITH_PROD );
				
				final ADDRNode global_opt_zero_states 
					= _manager.apply( _manager.DD_ONE, global_opt_one_states, DDOper.ARITH_MINUS );
				final ADDRNode policy_zero_out 
					= _manager.apply( policy, global_opt_zero_states, DDOper.ARITH_PROD );
				policy = _manager.apply( policy_zero_out, pi_z, DDOper.ARITH_PLUS );
				
//				final ADDRNode this_q = _manager.apply( ret, action_indicator, DDOper.ARITH_PROD );
				//making pi_z - should prolly make an argmax method... TODO
				//all positive 
				//except 0 for opt action
				//neg inf for illegal
//				final ADDRNode opt_one = _manager.threshold( diff, 0.0d, false );
				//sets neginf to zero
				//and zero to one
				//positive nos. to zero 
				//optimal action is 1
//				final ADDRNode opt_one = _manager.apply( _manager.DD_ONE, opt_zero, DDOper.ARITH_MINUS );
//				final ADDRNode opt_zero = _manager.apply( _manager.DD_ONE, 
//						opt_one, DDOper.ARITH_MINUS);
//				final ADDRNode global_zero_out = _manager.apply( policy,
//						opt_zero, DDOper.ARITH_PROD );
////				final ADDRNode opt_one_ties = _manager.breakTiesInBD0D(input, tiesIn, default_value)
////				final ADDRNode zero_out_old = _manager.apply( policy, opt_zero_full, DDOper.ARITH_PROD );
//				policy = _manager.apply( global_zero_out, opt_one, DDOper.ARITH_PLUS );
//				policy = applyMDPConstraints( policy, assign, _manager.DD_ZERO,
//						constrain_naively);
			}
		}
				
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
				&& _manager.hasSuffixVars(value_func, "'") ){
			try{
				throw new Exception("has primed var after expectations");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
				&& _manager.hasVars( value_func, _mdp.getFactoredActionSpace().getActionVariables() ) ){
			try{
				throw new Exception("Has action vars after max");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		final ADDPolicy add_policy = new ADDPolicy(_manager,
				_mdp.getFactoredStateSpace(), _mdp.getFactoredTransition(),
				_mdp.getFactoredReward(), _rand.nextLong() );
		add_policy._bddPolicy = policy;
		
		System.out.println("Regressed MBFAR, total leaves = " + total_leaves );
		
		return new UnorderedPair<ADDValueFunction, ADDPolicy>(
				new ADDValueFunction(value_func, null, null, _manager),
				add_policy );
	}
	
	private ADDRNode multiplyActionConstraints(final ADDRNode input,
			NavigableMap<String, Boolean> action ) {
		ADDRNode constraint = convertToNegInfDD( __action_constraints );
		if( action != null ){
			constraint = _manager.restrict( constraint, action );
		}
		final ADDRNode ret = _manager.apply(input, constraint, DDOper.ARITH_PLUS);
		return ret;
	}

	private ADDRNode multiplyStateConstraints( final ADDRNode input ) {
		final ADDRNode constraint = convertToNegInfDD( __state_constraints );
		final ADDRNode ret = _manager.apply( input, constraint, DDOper.ARITH_PLUS );
		return ret;
	}

	private ADDRNode computeExpectation(
			final ADDRNode input, 
			final String str, 
			final NavigableMap<String, Boolean> action,
			final boolean constrain_naively,
			final List<Long> size_change,
			final boolean do_apricodd, 
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ){
		
		ADDRNode this_cpt = null;
		if( action == null ){
			this_cpt = _mdp.getCpts().get( str );	
		}else{
			//this throws a null ptr exception 
			//if action is a superset of a legal/illegal action
			try{
				this_cpt = _mdp.getActionCpts().get( action ).get( str );	
			}catch( NullPointerException e ){
				final ADDRNode full_cpt = _mdp.getCpts().get( str );
				this_cpt = _manager.restrict(full_cpt, action);
//				e.printStackTrace();
//				System.exit(1);
			}
		}
		
		ADDRNode ret = input;
		final ADDRNode mult = _manager.apply(ret, this_cpt, DDOper.ARITH_PROD);
//		final ADDRNode mult_approx = _manager.doApricodd( mult, do_apricodd, apricodd_epsilon, apricodd_type );
		_manager.flushCaches(   );
//		ADDRNode mult_constrained = applyMDPConstraints(mult, action, _manager.DD_ZERO,
//				constrain_naively, size_change );
//		_manager.flushCaches(   );
		
		final ADDRNode summed = _manager.marginalize(mult, str, DDMarginalize.MARGINALIZE_SUM);
		final ADDRNode summed_approx = _manager.doApricodd( summed, do_apricodd , apricodd_epsilon, apricodd_type );
		_manager.flushCaches(  );
		ADDRNode summed_constrained = applyMDPConstraints(summed_approx, action, _manager.DD_NEG_INF,
				constrain_naively, size_change);
		_manager.flushCaches(   );
		
		if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
			System.out.println("showing diagrams");
			_manager.showGraph(ret, this_cpt, mult, summed, summed_constrained);
			try {
				System.in.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		ret = summed_constrained;
		
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
				&& _manager.hasSuffixVars(ret, str) ){
			try{
				throw new Exception("has " + str + " after expectation");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}

//		if( _dbg.compareTo( DEBUG_LEVEL.SOLUTION_INFO ) >= 0 ){
		if( size_change != null ){
			size_change.addAll( _manager.countNodes( input, mult, 
					summed, summed_constrained) );
		}
		return ret;
	}

	@Override
	public ADDRNode regressAction(final ADDRNode primed,
			final NavigableMap<String, Boolean> action,
			final boolean constrain_naively,
			final List<Long> size_change , 
			final boolean do_apricodd, final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ) {
		ArrayList<String> order = _mdp.getSumOrder();
		ADDRNode ret = primed;
		
		for( String xp : order ){
			ret = computeExpectation(ret, xp, action, constrain_naively, size_change,
					do_apricodd,
					apricodd_epsilon, apricodd_type );
		}
		
		//check if no primed vars
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
				&& _manager.hasSuffixVars( ret, "'" ) ){
			try{
				throw new Exception("Has primed vars after expectations");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		if( size_change != null ){
			size_change.addAll( _manager.countNodes( ret ) ) ;
		}
		ADDRNode reward_added = discountAndAddReward( ret , action, 
				constrain_naively, size_change , do_apricodd,
				apricodd_epsilon, apricodd_type );
		
		if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
			System.out.println("showing diagrams after reward addition");
			_manager.showGraph(ret, reward_added);
			try {
				System.in.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		ret = reward_added;
//		ret = applyMDPConstraintsNaively( ret, action, _manager.DD_NEG_INF, size_change );
		
		if( _manager.hasVars(ret, _mdp.getFactoredActionSpace().getActionVariables() ) ){
			try{
				throw new Exception("has action variables after regress action");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		if( _manager.hasSuffixVars(ret, "'") ){
			try{
				throw new Exception("has primed variables after regress action");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		return ret;
	}

	private ADDRNode discountAndAddReward(final ADDRNode input, 
			final NavigableMap<String, Boolean> action,
			final boolean constrain_naively ,
			final List<Long> size_change,
			final boolean do_apricodd, 
			final double apricodd_epsilon, 
			final APPROX_TYPE apricodd_type ) {
		
		
		List<ADDRNode> rewards = null;
		if( action == null ){
			rewards = _mdp.getRewards();
		}else if( action != null ){
			final Map<Map<String, Boolean>, ArrayList<ADDRNode>> actionRewards = _mdp.getActionRewards();
			rewards = actionRewards.get( action );
			if( rewards == null ){
				rewards = new ArrayList<ADDRNode>();
				final List<ADDRNode> all_rewards = _mdp.getRewards();
				for( final ADDRNode r : all_rewards ){
					rewards.add( _manager.restrict( r, action ) );					
				}
			}
		}

		final ADDRNode discounted = _manager.scalarMultiply(input, _mdp.getDiscount());
		final ADDRNode discounted_approx = _manager.doApricodd( discounted, 
				do_apricodd, apricodd_epsilon, apricodd_type );
		ADDRNode ret = discounted_approx; 
		
		if( size_change != null ){
			size_change.addAll( _manager.countNodes( input, ret ) );
		}

		
		for( ADDRNode this_reward : rewards ){
			final ADDRNode added_rew = _manager.apply( ret, this_reward, DDOper.ARITH_PLUS );
			final ADDRNode added_rew_approx = _manager.doApricodd( added_rew, 
					do_apricodd,
					apricodd_epsilon, apricodd_type );
			final ADDRNode added_rew_constrained 
				= applyMDPConstraints(added_rew_approx, action, _manager.DD_NEG_INF,
					constrain_naively, size_change );
			if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
				System.out.println("Showing diagrams after one addition" );
				_manager.showGraph( ret, this_reward, added_rew, added_rew_constrained );
			}
			
//			System.out.println("Adding reward " 
			if( size_change != null ){
				size_change.addAll( _manager.countNodes( added_rew , added_rew_constrained ) );
			}
			ret = added_rew_constrained;
			_manager.flushCaches( );
		}
		
//		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
//		}
		
		return ret;
	}

	@Override
	public ADDRNode regressPolicy(
			ADDRNode initial_value_func, ADDRNode policy, final boolean withActionVars,
			final boolean constrain_naively,
			final List<Long> size_change, 
			final boolean do_apricodd, final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ) {
		UnorderedPair<ADDValueFunction,ADDPolicy> ret = null;
		addPolicyConstraint( policy );
		ret = regress(initial_value_func, withActionVars, false, false,
				constrain_naively, size_change, do_apricodd, apricodd_epsilon, apricodd_type );
		if( !removePolicyConstraint( policy ) ){
			System.err.println("policy constraint not found");
			System.exit(1);
		}
		return ret._o1._valueFn;
	}

	private boolean removePolicyConstraint( final ADDRNode input ) {
		return __policy_constraints.remove( input );
	}

	private int addPolicyConstraint(ADDRNode policy) {
		__policy_constraints.add(policy);
		return __policy_constraints.size()-1;
	}

	@Override
	public UnorderedPair<ADDRNode, Integer> evaluatePolicy(
			ADDRNode initial_value_func, ADDRNode policy, final int nSteps, 
			final double epsilon, final boolean withActionVars,
			final boolean constraint_naively , final List<Long> size_change, 
			final boolean do_apricodd, 
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ) {
		int steps = 0;
		double error = Double.MAX_VALUE, prev_error = Double.NaN;
		ADDRNode value_func = initial_value_func, new_value_func = null;
		Timer evalT = new Timer();
		
		while( steps++ < nSteps && error > epsilon ){
			evalT.ResumeTimer();
			new_value_func = regressPolicy(value_func, policy,
					withActionVars, constraint_naively, size_change, do_apricodd, 
					apricodd_epsilon, apricodd_type );
			error = getBellmanError(new_value_func, value_func);
			evalT.PauseTimer();
			System.out.println( "Policy evaluation " + steps + " " +
					error + " Size of value : " + _manager.countNodes(new_value_func) 
					+ " time = " + evalT.GetElapsedTimeInMinutes() );
			System.out.println( "Size change " + size_change );
			size_change.clear();
			if( !do_apricodd && prev_error != Double.NaN && prev_error < error ){
				try{
					throw new Exception("BE increased here");
				}catch( Exception e ){
					e.printStackTrace();
					System.exit(1);
				}
			}
			prev_error = error;
			value_func = new_value_func;
		}
		return new UnorderedPair<ADDRNode, Integer>( value_func, steps-1 );
	}
	
	public static void testConstrainNaively( ){
		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp_same.rddl", "./rddl/sysadmin_uniring_1_3_0.rddl", 
				true, DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, true, 42);
		
		ADDDecisionTheoreticRegression ADD_dtr 
			= new ADDDecisionTheoreticRegression(mdp, 42);
		ADDManager manager = mdp.getManager();
		ADDRNode thing = manager.DD_ZERO;
		
		int i = 0;
		UnorderedPair< ADDValueFunction, ADDPolicy> naively = null;
		while( i++ < 20 ){
			naively = ADD_dtr.regressAllActions(thing, true, true, true, null, 
					false, 0.0d, APPROX_TYPE.NONE );
			System.out.println( i +  " " + ADD_dtr.getBellmanError( naively._o1._valueFn , thing ) );
			thing = naively._o1._valueFn;
		}
		manager.showGraph( naively._o1._valueFn, naively._o1._jointQFn, naively._o2._bddPolicy );	
		System.out.println( manager.countNodes( naively._o1._valueFn ,
				naively._o1._jointQFn, naively._o2._bddPolicy ) );
		
		i = 0;
		thing = manager.DD_ZERO;
		UnorderedPair< ADDValueFunction, ADDPolicy> smartly = null;
		while( i++ < 20 ){
			smartly = ADD_dtr.regressAllActions(thing, true, true, false, null, 
					false, 0.0d, APPROX_TYPE.NONE );
			System.out.println( i +  " " + ADD_dtr.getBellmanError( smartly._o1._valueFn , thing ) );
			thing = smartly._o1._valueFn;
		}
		
		manager.showGraph( smartly._o1._valueFn, smartly._o1._jointQFn, smartly._o2._bddPolicy );	
		System.out.println( manager.countNodes( smartly._o1._valueFn ,
				smartly._o1._jointQFn, smartly._o2._bddPolicy ) );
		
		
		//execute policy
		naively._o2.executePolicy(3, 4, true, mdp.getHorizon(), mdp.getDiscount()).printStats();
		smartly._o2.executePolicy(3, 4, true, mdp.getHorizon(), mdp.getDiscount()).printStats();
	}

	public static void main(String[] args) {
//		testRegressAction();
//		testRegressAllActions();
//		testSPUDD();
		testConstrainNaively();
//		try {
//			testRegressPolicy(true);
//		} catch (EvalException e) {
//			e.printStackTrace();
//		}
//		try {
//			testEvaluatePolicy();
//		} catch (EvalException e) {
//			e.printStackTrace();
//		}
	}
	
	private static void testEvaluatePolicy() throws EvalException {
		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp_same.rddl",
				"./rddl/sysadmin_uniring_1_3_0.rddl", 
				true, DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, true, 42);
		
		ADDDecisionTheoreticRegression ADD_dtr 
			= new ADDDecisionTheoreticRegression(mdp, 42);
		ADDManager manager = mdp.getManager();
	
		ADDRNode handCodedPolicy = getRebootDeadPolicy( manager, ADD_dtr, 
				mdp.getFactoredActionSpace().getActionVariables() );
		
		manager.showGraph( ADD_dtr.evaluatePolicy(manager.DD_ZERO, handCodedPolicy, 
				20, 0.01d, true, false, null , false , 0.0d, APPROX_TYPE.NONE )._o1 );
	}

	private static void testRegressPolicy(final boolean withActionVars) throws EvalException {
		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp_same.rddl",
				"./rddl/sysadmin_uniring_1_3_0.rddl", 
				withActionVars, DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, true, 42);
		
		ADDDecisionTheoreticRegression ADD_dtr 
			= new ADDDecisionTheoreticRegression(mdp, 42);
		ADDManager manager = mdp.getManager();
		ADDRNode oldValueFn = manager.DD_ZERO;
	
//		ADDRNode noOpPolicy = getNoOpPolicy( 
//				mdp.getFactoredActionSpace().getActionVariables(),
//				manager);
		
		ADDRNode handCodedPolicy = getRebootDeadPolicy( manager, ADD_dtr, 
				mdp.getFactoredActionSpace().getActionVariables() );
		
		int i = 0;
		ADDRNode newValueFn = null;
		while( i++ < 20 ){
			newValueFn = ADD_dtr.regressPolicy(oldValueFn, handCodedPolicy, 
					withActionVars, false, null , false,  0.0d, APPROX_TYPE.NONE );
			System.out.println( i +  " " 
					+ ADD_dtr.getBellmanError(newValueFn , oldValueFn ) );
			oldValueFn = newValueFn;
		}
//		manager.showGraph( (ADDRNode[])ret._o1._qFn.values().toArray() );
		manager.showGraph( newValueFn );	
		//execute policy
		ADDPolicy policy = new ADDPolicy(manager, mdp.getFactoredStateSpace(), 
				mdp.getFactoredTransition(), mdp.getFactoredReward(), 42);
		policy._bddPolicy = handCodedPolicy;
		policy.executePolicy(3, 4, true, mdp.getHorizon(), mdp.getDiscount()).printStats();
	}

	private static ADDRNode getRebootDeadPolicy(ADDManager manager,
			ADDDecisionTheoreticRegression dtr, Set<String> actionVars) throws EvalException {
		
		ADDRNode policy = manager.DD_ONE;
		for( String actionVar : actionVars ){
//			ADDRNode rebooter = manager.getIndicatorDiagram(actionVar, true);
//			ADDRNode runner = manager.getIndicatorDiagram(actionVar.replace("reboot", "running"), 
//					false );
//			ADDRNode thisComputerReboot = manager.apply( runner, rebooter, DDOper.ARITH_PROD );
//			
//			ADDRNode thisComputerDontReboot = manager.apply( 
//					manager.getIndicatorDiagram(actionVar, false),
//					manager.getIndicatorDiagram(actionVar.replace("reboot", "running"), true),
//					DDOper.ARITH_PROD );
			
			ADDRNode thisPolicy = manager.getINode( 
					actionVar.replace("reboot", "running"), 
					manager.getIndicatorDiagram(actionVar, false),
					manager.getIndicatorDiagram(actionVar, true) );

			policy = manager.apply(policy, thisPolicy, DDOper.ARITH_PROD );
		}
		
		policy = dtr.applyMDPConstraints( policy , null, manager.DD_ZERO, false, null);
		return policy;
	}

	private static ADDRNode getNoOpPolicy(final Set<String> actionVariables,
			final ADDManager manager) {
		ADDRNode ret = manager.DD_ONE;
		for(String s : actionVariables ){
			ret = manager.apply(ret, manager.getIndicatorDiagram(s, false), 
					DDOper.ARITH_PROD );
		}
		return ret;
	}

	private static void testSPUDD(){
		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp_same.rddl", "./rddl/sysadmin_uniring_1_3_0.rddl", 
				false, DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, true, 42);
		
		ADDDecisionTheoreticRegression ADD_dtr 
			= new ADDDecisionTheoreticRegression(mdp, 42);
		ADDManager manager = mdp.getManager();
		ADDRNode thing = manager.DD_ZERO;
		
		int i = 0;
		UnorderedPair<
		ADDValueFunction, ADDPolicy> ret = null;
		while( i++ < 20 ){
			ret = ADD_dtr.regressSPUDD(thing, true, true, false, null, false,  0.0d, APPROX_TYPE.NONE );
			System.out.println( i +  " " + ADD_dtr.getBellmanError( ret._o1._valueFn , thing ) );
			thing = ret._o1._valueFn;
		}
//		manager.showGraph( (ADDRNode[])ret._o1._qFn.values().toArray() );
		manager.showGraph( ret._o1._valueFn, ret._o2._addPolicy );	
		//execute policy
		ret._o2.executePolicy(3, 4, true, mdp.getHorizon(), mdp.getDiscount()).printStats();
	}

	private static void testRegressAction() {
		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp_same.rddl", "./rddl/sysadmin_uniring_1_3_0.rddl", 
				false, DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, true, 42);
		
		ADDDecisionTheoreticRegression ADD_dtr 
			= new ADDDecisionTheoreticRegression(mdp, 42);
		NavigableMap<String, Boolean> action = new TreeMap<String, Boolean>();
		action.put( "reboot__c1", false );
		action.put( "reboot__c2", false );
//		action.put( "reboot__c3", false );
		ADDManager manager = mdp.getManager();
		ADDRNode thing = manager.getINode("running__c1", manager.getLeaf(4d, 4d), 
									manager.getLeaf(7.5, 7.5) );
		thing = manager.remapVars( thing, mdp.getPrimeRemap() );
		thing = ADD_dtr.regressAction( thing, action, false, null , false,  0.0d, APPROX_TYPE.NONE );
		manager.showGraph( thing );
	}
	
	private static void testRegressAllActions(){
		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp_same.rddl", "./rddl/sysadmin_uniring_1_3_0.rddl", 
				true, DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, true, 42);
		
		ADDDecisionTheoreticRegression ADD_dtr 
			= new ADDDecisionTheoreticRegression(mdp, 42);
		ADDManager manager = mdp.getManager();
		ADDRNode thing = manager.DD_ZERO;
		
		int i = 0;
		UnorderedPair<
		ADDValueFunction, ADDPolicy> ret = null;
		while( i++ < 20 ){
			ret = ADD_dtr.regressAllActions(thing, true, true, false, null, false,  0.0d, APPROX_TYPE.NONE );
			System.out.println( i +  " " + ADD_dtr.getBellmanError( ret._o1._valueFn , thing ) );
			thing = ret._o1._valueFn;
		}
		manager.showGraph( ret._o1._valueFn, ret._o1._jointQFn, ret._o2._bddPolicy );	
		//execute policy
		ret._o2.executePolicy(3, 4, true, mdp.getHorizon(), mdp.getDiscount()).printStats();
	}

	public double getBellmanError(final ADDRNode valueFn, final ADDRNode oldValueFn) {
//		Objects.requireNonNull( valueFn );
//		Objects.requireNonNull( oldValueFn );
		
		ADDRNode diff = _manager.apply(valueFn, oldValueFn, DDOper.ARITH_MINUS );
		diff = _manager.remapLeaf( diff, _manager.DD_NEG_INF, _manager.DD_ZERO );
		
		final double max = diff.getMax();
		final double min = ( diff.getMin() == _manager.getNegativeInfValue() ) ? 0 : diff.getMin();
		return Math.max( Math.abs(max), Math.abs(min) );
	}

	public UnorderedPair<ADDRNode, Integer> evaluatePolicyMBFAR(
			final ADDRNode initial_value_func, final ADDRNode policy, int nSteps,
			final double epsilon, final boolean constraint_naively, 
			final boolean make_policy, final long bigdd,
			final  boolean LIMIT_EVAL, 
			final boolean do_apricodd, 
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ) {
		int steps = 0;
		double error = Double.MAX_VALUE, prev_error = Double.NaN;
		ADDRNode value_func = initial_value_func, new_value_func = null;
		Timer evalT = new Timer();
		while( steps++ < nSteps && error > epsilon ){
			evalT.ResumeTimer();
			new_value_func = regressPolicyMBFAR(value_func, policy, constraint_naively,
					make_policy, bigdd, 
					do_apricodd, 
					apricodd_epsilon, apricodd_type );
			error = getBellmanError(new_value_func, value_func);
			evalT.PauseTimer();
			final long size = _manager.countNodes(new_value_func).get(0);
			System.out.println( "MBFAR Policy evaluation " + steps + " " +
					error + " Size of value : " + size + " time : " + evalT.GetElapsedTimeInMinutes() );
			if( prev_error != Double.NaN && prev_error < error ){
				try{
					throw new Exception("BE increased here");
				}catch( Exception e ){
					e.printStackTrace();
					System.exit(1);
				}
			}
			prev_error = error;
			value_func = new_value_func;
			if( LIMIT_EVAL && size > bigdd ){
				System.out.println("MB stopping evaluation");
				break;
			}
		}
		return new UnorderedPair<ADDRNode, Integer>( value_func, steps-1 );
	}

	private ADDRNode regressPolicyMBFAR(final ADDRNode input, final ADDRNode policy,
			final boolean constraint_naively, final boolean make_policy,
			final long BIGDD, 
			final boolean do_apricodd, 
			final double apricodd_epsilon, final APPROX_TYPE apricodd_type ) {
		UnorderedPair<ADDValueFunction,ADDPolicy> ret = null;
		addPolicyConstraint( policy );
		ret = regressMBFAR(input, make_policy, constraint_naively, BIGDD, null ,
				do_apricodd, 
				apricodd_epsilon, apricodd_type );
		if( !removePolicyConstraint( policy ) ){
			System.err.println("policy constraint not found");
			System.exit(1);
		}
		return ret._o1._valueFn;
	}

	@Override
	public <T extends SymbolicValueFunction<ADDNode, 
	ADDRNode, ADDINode, ADDLeaf, RDDLFactoredStateSpace, RDDLFactoredActionSpace>,
	U extends SymbolicPolicy<ADDNode, ADDRNode, ADDINode, ADDLeaf, 
		RDDLFactoredStateSpace, RDDLFactoredActionSpace>> UnorderedPair<T, U> regress(
			ADDRNode input, boolean withActionVars, boolean keepQ,
			boolean makePolicy, boolean constraint_naively,
			List<Long> size_change,
			final boolean do_apricodd, 
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ) {
		final ADDRNode primed = _manager.remapVars(input, _mdp.getPrimeRemap() );
//		final ADDRNode constrained_zero = _manager.remapLeaf( primed ,  _manager.DD_NEG_INF, _manager.DD_ZERO );
		
		UnorderedPair< ADDValueFunction, ADDPolicy > ret = null;
		if( withActionVars ){
			ret = regressAllActions( primed, keepQ, makePolicy, 
					constraint_naively , size_change , do_apricodd, apricodd_epsilon, apricodd_type );
		}else{
			ret =  regressSPUDD( primed, keepQ, makePolicy, 
					constraint_naively, size_change, do_apricodd, apricodd_epsilon, apricodd_type );
		}
		
		if( !keepQ ){
			ret._o1.throwAwayQFunctions();
		}
		return (UnorderedPair<T, U>) ret;
	}

	public boolean terminate(final double BE,  
			final int iteration, 
			final double epsilon,
			final int horizon) {
		return ( horizon != -1 && iteration >= horizon ) || 
				( horizon == -1 && BE <= epsilon );
	}

//	TODO
//	public void checkModel( final NavigableMap<String, Boolean> action ){
//		
//	}
	
//	private static void testRegressAllActions() {
//		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp_same.rddl", "./rddl/sysadmin_uniring_1_3_0.rddl", 
//				false, DEBUG_LEVEL.SOLUTION_INFO, ORDER.GUESS);
//		
//		ADDDecisionTheoreticRegression<RDDLFactoredStateSpace, RDDLFactoredActionSpace> ADD_dtr 
//			= new ADDDecisionTheoreticRegression<RDDLFactoredStateSpace, RDDLFactoredActionSpace>(mdp);
//		ADDManager manager = mdp.getManager();
//		ADDRNode thing = manager.DD_ZERO;
//		thing = ADD_dtr.regressAction( thing, action );
//		manager.showGraph( thing );
//	}
}
