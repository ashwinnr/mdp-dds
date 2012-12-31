package dtr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

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
import util.UnorderedPair;
import add.ADDINode;
import add.ADDLeaf;
import add.ADDManager;
import add.ADDNode;
import add.ADDRNode;
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
	private Set<ADDRNode> _constraints;
	private Random _rand;

	public ADDDecisionTheoreticRegression(RDDL2ADD mdp, final long seed){
		_manager = mdp.getManager();
		_mdp = mdp;
		_dbg = mdp.__debug_level;
		_constraints = mdp.getConstraints();
		_rand = new Random( seed );
	}
	
	@Override
	public UnorderedPair< ADDValueFunction, ADDPolicy > regress( final ADDRNode input, 
			final boolean withActionVars,
			final boolean keepQ, final boolean makePolicy ) {
//		final ADDRNode withoutNegInf = _manager.apply(input, _manager.DD_ZERO,
//				DDOper.ARITH_MAX );
		ADDRNode primed = _manager.remapVars(input, _mdp.getPrimeRemap() );
		
		UnorderedPair< ADDValueFunction, ADDPolicy > ret = null;
		if( withActionVars ){
			ret = regressAllActions( primed, keepQ, makePolicy );
		}else{
			ret =  regressSPUDD( primed, keepQ, makePolicy );
		}
		
		if( !keepQ ){
			ret._o1.throwAwayQFunctions();
		}
		return ret;
	}
	
	@Override
	public ADDRNode applyMDPConstraints(final ADDRNode input,
			final NavigableMap<String, Boolean> action,
			final ADDRNode violate ) {
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
			System.out.println("applying constraint");
		}
		ADDRNode ret = input;
		for( ADDRNode constraint : _constraints ){
			ADDRNode action_constraint;
			if( action != null ){
				action_constraint = _manager.evaluate(constraint, action);
			}else{
				action_constraint = constraint;
			}
			ADDRNode constrained = _manager.constrain(ret, action_constraint, violate);
			
			if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
				_manager.showGraph( ret, constraint, constrained );
				try {
					System.in.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			ret = constrained;
		}
		return ret;
	}
	
	private UnorderedPair<ADDValueFunction, ADDPolicy> regressSPUDD(final ADDRNode primed, 
			boolean keepQ, boolean makePolicy){
		
		NavigableMap< FactoredAction<RDDLFactoredStateSpace , 
			RDDLFactoredActionSpace>, ADDRNode > q_map = null;
		ADDPolicy policy = null;
		ADDRNode v_func = _manager.DD_NEG_INF;
		
		List<NavigableMap<String, Boolean>> actions = _mdp.getFullRegressionOrder();
		for( NavigableMap<String, Boolean> action : actions ){
			ADDRNode this_q = regressAction( primed, action );
			if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.println("Regressing action : " + action );
			}
			
			v_func = _manager.apply( v_func, this_q, DDOper.ARITH_MAX );
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
//			_manager.flushCaches(false);
		}
		
		return new UnorderedPair<ADDValueFunction, ADDPolicy>( 
				new ADDValueFunction( v_func, q_map, null, _manager )
				, policy);
	}
	
	private UnorderedPair<ADDValueFunction, ADDPolicy> regressAllActions(final ADDRNode primed, 
			boolean keepQ, boolean makePolicy) {

		ArrayList<String> sum_order = _mdp.getSumOrder();
		
		ADDRNode ret = primed;
		for( String str : sum_order ){
			if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.println("Regressing " + str );
			}
			ret = computeExpectation( ret, str, null );
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
		//discount
		ret = _manager.scalarMultiply(ret, _mdp.getDiscount());
		
		ADDRNode q_func = ret;
		if( _mdp._bRewardActionDependent ){
			ADDRNode reward_added = addReward( ret, null );
			if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.println( "reward added " );
			}
			if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
				System.out.println( "showing diagrams");
				_manager.showGraph( ret, reward_added);
			}
			ret = reward_added;
			q_func = reward_added;
		}
		
		//ret now has Q function
		if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
			System.out.println("Q function");
			_manager.showGraph(ret);
			try {
				System.in.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		ArrayList<String> max_order = _mdp.getElimOrder();
		for( String str : max_order ){
			if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.println("Maxing " + str );
			}
			ADDRNode maxd = _manager.marginalize(ret, str, DDMarginalize.MARGINALIZE_MAX);
			if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
				System.out.println("showing diagrams");
				_manager.showGraph(ret, maxd );
				try {
					System.in.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			ret = maxd;
		}
		
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
				&& _manager.hasVars( ret, _mdp.getFactoredActionSpace().getActionVariables() ) ){
			try{
				throw new Exception("Has primed vars after max");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}

		if( !_mdp._bRewardActionDependent ){
			ADDRNode reward_added = addReward( ret, null );
			if( makePolicy ){
				q_func = addReward( q_func, null ); 
			}

			if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.println( "reward added " );
			}
			if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
				System.out.println( "showing diagrams");
				_manager.showGraph( ret, q_func, reward_added);
			}
			ret = reward_added;
		}
		
		if(_dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
				&& _manager.hasVars( ret, _mdp.getFactoredActionSpace().getActionVariables() ) ){
			try{
				throw new Exception("Has primed vars after max");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		ADDRNode v_func = ret;
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

	private ADDRNode computeExpectation(ADDRNode input, String str, NavigableMap<String, Boolean> action ){
		
		ADDRNode this_cpt = null;
		if( action == null ){
			this_cpt = _mdp.getCpts().get( str );	
		}else{
			//this throws a null ptr exception 
			//if action is a superset of a legal/illegal action
			try{
				this_cpt = _mdp.getActionCpts().get( action ).get( str );	
			}catch( NullPointerException e ){
				e.printStackTrace();
				System.exit(1);
			}
			
		}
		
		ADDRNode ret = input;
		ADDRNode mult = _manager.apply(ret, this_cpt, DDOper.ARITH_PROD);
		ADDRNode mult_constrained = applyMDPConstraints(mult, action, _manager.DD_ZERO);
		
		ADDRNode summed = _manager.marginalize(mult_constrained, str, DDMarginalize.MARGINALIZE_SUM);
		ADDRNode summed_constrained = applyMDPConstraints(summed, null, _manager.DD_NEG_INF);
		
		if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
			System.out.println("showing diagrams");
			_manager.showGraph(ret, this_cpt, mult, mult_constrained, summed, summed_constrained);
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
		
//		_manager.flushCaches(false);
		
		return ret;
	}

	@Override
	public ADDRNode regressAction(ADDRNode primed,
			NavigableMap<String, Boolean> action) {
		ArrayList<String> order = _mdp.getSumOrder();
		ADDRNode ret = primed;
		
		for( String xp : order ){
			ret = computeExpectation(ret, xp, action);
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
		//discount
		ret = _manager.scalarMultiply(ret, _mdp.getDiscount());
		
		ADDRNode reward_added = addReward( ret , action );
		
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
		
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
				&& _manager.hasVars(ret, _mdp.getFactoredActionSpace().getActionVariables() ) ){
			try{
				throw new Exception("has action variables in regress action");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		return ret;
	}

	private ADDRNode addReward(ADDRNode input, NavigableMap<String, Boolean> action) {
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
			System.out.println("Adding reward " );
		}
		List<ADDRNode> rewards = null;
		if( action == null && _mdp._bRewardActionDependent ){
			rewards = _mdp.getRewards();
		}else if( action != null && !_mdp._bRewardActionDependent ||
					action  == null && _mdp._bRewardActionDependent ){
			try{
				throw new Exception("wrong place to be adding reward");
			}catch( Exception e ){
				e.printStackTrace();
			}
		}else if( action != null && _mdp._bRewardActionDependent ){
			final Map<Map<String, Boolean>, ArrayList<ADDRNode>> actionRewards = _mdp.getActionRewards();
			rewards = actionRewards.get( action );
		}

		ADDRNode ret = input;
		for( ADDRNode this_reward : rewards ){
			ADDRNode added_rew = _manager.apply( ret, this_reward, DDOper.ARITH_PLUS );
			ADDRNode added_rew_constrained = applyMDPConstraints(added_rew, null, _manager.DD_NEG_INF);
			if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
				System.out.println("Showing diagrams after one addition" );
				_manager.showGraph( ret, this_reward, added_rew, added_rew_constrained );
			}
			ret = added_rew_constrained;
		}		
//		_manager.flushCaches(false);
		
		return ret;
	}

	@Override
	public ADDRNode regressPolicy(
			ADDRNode initial_value_func, ADDRNode policy, final boolean withActionVars ) {
		UnorderedPair<ADDValueFunction,ADDPolicy> ret = null;
		addConstraint( policy );
		ret = regress(initial_value_func, withActionVars, false, false);
		removeConstraint( policy );
		return ret._o1._valueFn;
	}

	private void removeConstraint( final ADDRNode input ) {
		_constraints.remove( input );
	}

	private int addConstraint(ADDRNode policy) {
		_constraints.add(policy);
		return _constraints.size()-1;
	}

	@Override
	public ADDRNode evaluatePolicy(
			ADDRNode initial_value_func, ADDRNode policy, final int nSteps, 
			final double epsilon, final boolean withActionVars ) {
		int steps = 0;
		double error = Double.MAX_VALUE;
		ADDRNode value_func = initial_value_func, new_value_func = null;
		
		while( steps++ < nSteps && error > epsilon ){
			new_value_func = regressPolicy(value_func, policy, withActionVars);
			System.out.println( getBellmanError(new_value_func, value_func) );
			value_func = new_value_func;
		}
		return value_func;
	}

	public static void main(String[] args) {
//		testRegressAction();
//		testRegressAllActions();
		testSPUDD();
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
				20, 0.01d, true ) );
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
					withActionVars );
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
		
		policy = dtr.applyMDPConstraints( policy , null, manager.DD_ZERO );
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
			ret = ADD_dtr.regressSPUDD(thing, true, true );
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
		thing = ADD_dtr.regressAction( thing, action );
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
			ret = ADD_dtr.regressAllActions(thing, true, true );
			System.out.println( i +  " " + ADD_dtr.getBellmanError( ret._o1._valueFn , thing ) );
			thing = ret._o1._valueFn;
		}
		manager.showGraph( ret._o1._valueFn, ret._o1._jointQFn, ret._o2._bddPolicy );	
		//execute policy
		ret._o2.executePolicy(3, 4, true, mdp.getHorizon(), mdp.getDiscount()).printStats();
	}

	public double getBellmanError(ADDRNode valueFn, ADDRNode oldValueFn) {
		ADDRNode diff = _manager.apply(valueFn, oldValueFn, DDOper.ARITH_MINUS );
		double max = diff.getMax();
		double min = diff.getMin();
		min = ( min == _manager.getNegativeInfValue() ) ? 0 : min;
		return Math.max( Math.abs(max), Math.abs(min) );
	}
	
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
