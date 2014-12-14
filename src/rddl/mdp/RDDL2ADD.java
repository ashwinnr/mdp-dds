package rddl.mdp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.collect.Maps;

import rddl.EvalException;
import rddl.RDDL;
import rddl.RDDL.AGG_EXPR;
import rddl.RDDL.BOOL_CONST_EXPR;
import rddl.RDDL.BOOL_EXPR;
import rddl.RDDL.Bernoulli;
import rddl.RDDL.CPF_DEF;
import rddl.RDDL.DOMAIN;
import rddl.RDDL.DiracDelta;
import rddl.RDDL.EXPR;
import rddl.RDDL.IF_EXPR;
import rddl.RDDL.INSTANCE;
import rddl.RDDL.INT_CONST_EXPR;
import rddl.RDDL.KronDelta;
import rddl.RDDL.LCONST;
import rddl.RDDL.LVAR;
import rddl.RDDL.NONFLUENTS;
import rddl.RDDL.OPER_EXPR;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.REAL_CONST_EXPR;
import rddl.State;
import rddl.parser.parser;
import util.Pair;
import util.UnorderedPair;
import add.ADDINode;
import add.ADDLeaf;
import add.ADDManager;
import add.ADDNode;
import add.ADDRNode;
import dd.DDManager.DDOper;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredStateSpace;

public class RDDL2ADD extends RDDL2DD<ADDNode, ADDRNode, ADDINode, ADDLeaf> {

	private static final long  MANAGER_STORE_INIT_SIZE = (long) 1e2;
	private static final long  MANAGER_STORE_INCR_SIZE = (long) 1e2;
	
	protected ADDManager _manager;
	private boolean _bCPFDeterministic;
	private ArrayList<String> _varOrder;
	private List<NavigableMap<String, Boolean>> _regOrder;
	private ADDRNode _concurrencyConstraint;
	private RDDLFactoredActionSpace _rddlActionSpace;
	private RDDLFactoredStateSpace _rddlStateSpace;
	private RDDLFactoredTransition _rddlTransition;
	private RDDLFactoredReward _rddlReward;
	private Random _rand;
	private ArrayList<Pair<String, String>> _transition_relation_last = null;//to make dynamic quantificatoin linear time
	private Map< NavigableMap<String, Boolean>, ArrayList< Pair<String, String> > > _actionTransitionRelationLast = null;
	private int _totalVariables;
	private int _numStateVars;
	private int _numActionVars;

	public int getNumStateVars(){
		return _numStateVars;
	}
	
	public int getOrderIndex( final String var ){
		return _varOrder.indexOf( var );
	}
	
	public ArrayList<Pair<String, String>> getTransitionRelationLastFAR() {
		return _transition_relation_last;
	}
	
	public Map<NavigableMap<String, Boolean>, ArrayList<Pair<String, String>>> getTransitionRelationLastSPUDD() {
		return _actionTransitionRelationLast;
	}
	
	@Override
	public ADDRNode enumerateAssignments(HashSet<UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > vars, 
			EXPR cpf_expr, HashMap<LVAR,LCONST> subs ) 
		throws Exception {
		return enumerateAssignmentsInt(new ArrayList< UnorderedPair<PVAR_NAME, ArrayList<LCONST> > >(vars), 
				cpf_expr, subs, 0  );
	}
	
	private ADDRNode enumerateAssignmentsInt(ArrayList<UnorderedPair<PVAR_NAME, ArrayList<LCONST>>> vars, 
			EXPR cpf_expr, HashMap<LVAR,LCONST> subs, int index ) 
		throws EvalException, Exception {
	
		// Need to build an assignment ADD on the way down, use it 
		// at leaf to add to current CPT and return result
		
		// Recurse until index == vars.size
		if ( index < vars.size() ) {
			
			UnorderedPair<PVAR_NAME, ArrayList<LCONST>> thisThing = vars.get( index );
			
			PVAR_NAME p = thisThing._o1;
			
			ArrayList<LCONST> terms = (ArrayList<LCONST>)thisThing._o2;
			
			String var_name = CleanFluentName(p._sPVarName + terms + (p._bPrimed ? "\'" : ""));
//			System.err.println(var_name );
	
			// Set to true
			_state.setPVariableAssign(p, terms, RDDL.BOOL_CONST_EXPR.TRUE);
//			System.out.println(index + " " + var_name);
//			System.err.println(true);
			ADDRNode ret_high = enumerateAssignmentsInt( vars, cpf_expr, 
					subs , index+1 );
//			System.out.println(_state);
			
//			System.out.println(_context.printNode(ret_high));
			
			// Set to false
			_state.setPVariableAssign(p, terms, RDDL.BOOL_CONST_EXPR.FALSE);
//			System.out.println(index + " " + var_name);
//			System.err.println(var_name );
//			System.err.println(false);
			
			ADDRNode ret_low = enumerateAssignmentsInt( vars, cpf_expr, subs , index+1 );
//			System.out.println(_state);
			
//			System.out.println(_context.printNode(ret_low));
			
			ADDRNode res = _manager.getINode(var_name, ret_high, ret_low );
			
//			System.out.println("res");
//			System.out.println(_context.printNode(res));
			
			// Unassign
			_state.setPVariableAssign(p, terms, null);
	//		_context.getGraph(res).launchViewer();
	//		System.in.read();
			// Sum both sides of the returned CPT and return it
			return res;
		
		}else{
			
			
			// At this point, all state and action variables are set
			// Just need to get the distribution for the appropriate CPT
			RDDL.EXPR e = cpf_expr.getDist(subs, _state);
	//		System.out.println( _state + " " + e );
			double prob_true = -1d;
			//System.out.println("RDDL.EXPR: " + e);
			if (e instanceof KronDelta) {
				EXPR e2 = ((KronDelta)e)._exprIntValue;
				if (e2 instanceof INT_CONST_EXPR)
					// Should either be true (1) or false (0)... same as prob_true
					prob_true = (double)((INT_CONST_EXPR)e2)._nValue;
				else if (e2 instanceof BOOL_CONST_EXPR)
					prob_true = ((BOOL_CONST_EXPR)e2)._bValue ? 1d : 0d;
				else
					throw new EvalException("Unhandled KronDelta argument: " + e2.getClass()); 
			} else if (e instanceof Bernoulli) {
				prob_true = ((REAL_CONST_EXPR)((Bernoulli)e)._exprProb)._dValue;
			} else if (e instanceof DiracDelta) {
				// NOTE: this is not a probability, but rather an actual value
				//       (presumably for the reward).  This method is a little
				//       overloaded... need to consider whether there will be
				//       any problems arising from this overloading.  -Scott
				prob_true = ((REAL_CONST_EXPR)((DiracDelta)e)._exprRealValue)._dValue;
			} else{
				throw new EvalException("Unhandled distribution type: " + e.getClass());
			}
			
//			for( Pair p : vars ) {
//				PVAR_NAME pvar = (PVAR_NAME)p._o1;
//				ArrayList<LCONST> terms = (ArrayList<LCONST>)p._o2;
//				System.out.println(pvar.toString() +  " " + terms + " " + _state.getPVariableAssign(pvar, terms));
//			}
//			System.out.println(prob_true);
			
			// Now build CPT for action variables
			return _manager.getLeaf(prob_true);
	
		} 
		
	}

	//just add to _constraints : side effect
	private ADDRNode addConstraint(BOOL_EXPR be) throws Exception {
		
		HashMap<LVAR, LCONST> empty = new HashMap<LVAR, LCONST>();
		
		HashSet<UnorderedPair<PVAR_NAME, ArrayList<LCONST>>> gfluents 
			= new HashSet<UnorderedPair<PVAR_NAME, ArrayList<LCONST>>>();
		
		be.collectGFluents(empty, _state, gfluents);
		
		IF_EXPR ife = new IF_EXPR(be, new KronDelta(new BOOL_CONST_EXPR(true)), 
				new KronDelta(new BOOL_CONST_EXPR(false)) );
		
		ADDRNode zerodd = enumerateAssignments( gfluents, ife, empty );
		
		empty.clear();
		
		if( __debug_level.compareTo(DEBUG_LEVEL.DIAGRAMS) == 0 ) {
			System.out.println("Displaying constraint : " + be);
			_manager.showGraph(zerodd);
			System.out.println("Press key...");
			System.in.read();
		}
	
		final boolean action_constraint = _manager.hasVars( zerodd,  _actionVars );
		final boolean state_constraint = _manager.hasVars( zerodd, _stateVars );
		if( state_constraint && action_constraint ){
			_action_preconditions.add( zerodd );
		}else if( action_constraint ){
			_action_constraints.add( zerodd );
		}else if( state_constraint ){
			_state_constraints.add( zerodd );
		}
		
		return zerodd;

	}
	
	public RDDL2ADD( final String pDomain, final String pInstance, boolean withActionVars, 
						DEBUG_LEVEL debug, ORDER order, final boolean buildADDs,
						long seed ) {
	    	super();
		__debug_level = debug;
		_order = order;
		_rand = new Random(seed);
		buildFromFile(pDomain, pInstance, withActionVars, buildADDs);
		
	}

	public void buildFromFile(final String pDomain, final String pInstance, boolean withActionVars,
			boolean buildADDs) {

		File domf = new File(pDomain);
		File instf = new File(pInstance);

		RDDL domain = null;
		RDDL instance = null;

		try
		{
			domain = parser.parse(domf);
		} catch (Exception e)
		{
			System.err.println("domain file did not parse");
			e.printStackTrace();
			System.exit(1);
		}


		try
		{
			instance = parser.parse(instf);
		} catch (Exception e)
		{
			System.err.println("domain file did not parse");
			e.printStackTrace();
			System.exit(1);
		}

		buildFromRDDL(domain, instance, withActionVars, buildADDs);
	}
	/**
	 * @param pDomain
	 * @param pInstance
	 * @param buildADDs 
	 */
	private void buildFromRDDL(RDDL pDomain, RDDL pInstance, boolean withActionVars, 
			boolean buildADDs) {
		
		try {
			
			DOMAIN d = pDomain._tmDomainNodes.entrySet().iterator().next().getValue();
			
			INSTANCE i = pInstance._tmInstanceNodes.entrySet().iterator().next().getValue();
			
			NONFLUENTS n = pInstance._tmNonFluentNodes.entrySet().iterator().next().getValue();
			System.out.println("concurrency : " + i._nNonDefActions );

			_bCPFDeterministic = d._bCPFDeterministic;

			this._i = i;
			this._n = n;
			this._d = d;
			
			_state = new State();

			initializeState(i, d, n);
			assert (_state._tmIntermNames.size() == 0);
			initRDDLData();

			_rddlActionSpace = new RDDLFactoredActionSpace();
			_rddlActionSpace.setActionVariables(_actionVars);
			
			_rddlStateSpace = new RDDLFactoredStateSpace();
			_rddlStateSpace.setStateVariables(_stateVars  );
			
			_rddlTransition = new RDDLFactoredTransition(_state, _rddlStateSpace,
					_rddlActionSpace, _tmStateVars, _tmActionVars);

			_rddlReward = new RDDLFactoredReward(_state, _tmStateVars, 
					_rand.nextLong(), _tmActionVars);//uses rrddl sample()

			initADD();
			makePrimeRemap();
			makeOrders();
			addConcurrencyConstraint();
			for( EXPR constraint : _state._alConstraints ){
				if( constraint instanceof BOOL_EXPR ){
					BOOL_EXPR be = (BOOL_EXPR)constraint;
					if( __debug_level.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
						System.out.println(be);
				    }
					addConstraint(be);
					_manager.flushCaches( );
				}else{
					System.err.println("Constraint not tpye of bool expr");
					System.exit(1);
				}
			}
			if( __debug_level.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.println("state constraints built");
			}
			if( buildADDs ){
				buildCPTs(withActionVars);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	//assumes with action vars
//	public List<String> sortActionsByInDegree( ){
//		final TreeSet<Pair<Integer, String>> sortedActions = new TreeSet<Pair<Integer,String>>();
//		for( final Entry<String, ArrayList<String>> entry : _hmact2vars.entrySet() ){
//			sortedActions.add(new Pair<Integer, String>(entry.getValue().size(), entry.getKey()));
//		}
//		final List<String> ret = new ArrayList<String>();
//		for( final Pair<Integer, String> entry : sortedActions ){
//			ret.add(entry._o2);
//		}
//		return Collections.unmodifiableList(ret);
//	}
	
	//depends on _sumorder
	public Map<String, ArrayList<String>> getHindSightOrder( ){
		final Map<String, ArrayList<String>> ret = new HashMap<String, ArrayList<String>>();
		final ArrayList<String> sumOrd = getSumOrder();
		final Map<String, Integer> actionCounts = new HashMap<String, Integer>();
		for( final String ns : sumOrd ){
			final ArrayList<String> affectedByActs = _hmvars2act.get(ns);
			if( affectedByActs == null ){
				continue;
			}
			for( final String affectByAct : affectedByActs ){
				final Integer current_count = actionCounts.get(affectByAct);
				actionCounts.put( affectByAct,  ( current_count == null ) ? 1 : current_count + 1 );
			}
		}
		
		for( final String ns : sumOrd ){
			final ArrayList<String> affectedByActs = _hmvars2act.get(ns);
			if( affectedByActs == null ){
				continue;
			}
			for( final String affectByAct : affectedByActs ){
				int current_count = actionCounts.get(affectByAct);
				current_count--;
				if( current_count == 0 ){
					ArrayList<String> already_list = ret.get(ns);
					if( already_list == null ){
						already_list = new ArrayList<String>();
					}
					already_list.add(affectByAct);
					ret.put(ns, already_list);
					actionCounts.remove(affectByAct);
				}else{
					actionCounts.put(affectByAct, current_count);
				}
			}
		}
		
		return Collections.unmodifiableMap(ret);
	}
	
	private Map<String, Boolean> getDefaultAct() {

		Map<String, Boolean> ret = new HashMap<String, Boolean>();

		for( Map.Entry<PVAR_NAME, ArrayList<ArrayList<LCONST>>> entry : _action_vars.entrySet() ){
			for( ArrayList<LCONST> term : entry.getValue() ){
				boolean defval = (Boolean)_state.getDefaultValue(entry.getKey());
				String name = CleanFluentName(entry.getKey()._sPVarName + term.toString() );
				ret.put(name, defval);
			}
		}

		return ret;
		
	}
	
	protected ADDRNode addConcurrencyConstraint() {
		
		if( __debug_level.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
			System.out.println("\nBuilding concurrency constraint");
		}
	
		ADDRNode sum = _manager.DD_ZERO;
		
		for( String a : _actionVars ){
		    if( __debug_level.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
			System.out.println(a);
		    }
		    if( sum.getMax() > _i._nNonDefActions ){
		    	try{
		    		throw new Exception("Error in incremental constraint building");
		    	}catch( Exception e ){
		    		e.printStackTrace();
		    		System.exit(1);
		    	}
		    }
		    sum = _manager.apply( sum, _manager.getIndicatorDiagram(a, true) , DDOper.ARITH_PLUS );
		    //incremental pruning
		    ADDRNode cur_sum_bdd = _manager.threshold(sum, _i._nNonDefActions, false);
		    //set violated to -inf in sum
		    //so that threshold will put it in zero
		    ADDRNode cur_sum_add = _manager.remapLeaf(cur_sum_bdd, _manager.DD_ZERO, _manager.DD_NEG_INF);
		    		
		    sum = _manager.BDDIntersection(sum , cur_sum_add );
		    if( __debug_level.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.println(sum.getMax());
		    }
		}
		
		//threshold
		ADDRNode zeroOneDD = _manager.threshold(sum, _i._nNonDefActions, false);
		
		_action_constraints.add( zeroOneDD );
		
		_concurrencyConstraint = zeroOneDD;
		
		if( __debug_level.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
			System.out.println("concurrency built");
	    }
		
		if( __debug_level.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
			System.out.println("\nDisplaying concurrency constraint");
			_manager.showGraph(zeroOneDD);
		}
		
		return zeroOneDD;
		
	}

	public NavigableMap<String, Boolean> getInitialState(){
		final NavigableMap<String, Boolean> ret = new TreeMap<String, Boolean>();
		ArrayList<PVAR_INST_DEF> init_state = this._i._alInitState;
		for( final PVAR_INST_DEF pdef : init_state ){
			final ArrayList<LCONST> terms = pdef._alTerms;
			ret.put( CleanFluentName( pdef._sPredName.toString() + terms ),
					(Boolean)pdef._oValue );
		}
		return ret;
	}

	private void buildCPTs(boolean withActionVars) throws Exception {

		_defaultAct = getDefaultAct();		

		EXPR rew_expr =  _state._reward;

		HashSet<UnorderedPair<PVAR_NAME, ArrayList<LCONST>>> rewardRelVars 
			= new HashSet<UnorderedPair<PVAR_NAME, ArrayList<LCONST> > >();

		rew_expr.collectGFluents(new HashMap<LVAR, LCONST>(), _state, rewardRelVars );

		for( UnorderedPair<PVAR_NAME, ArrayList<LCONST>> piar : rewardRelVars ){
			PVAR_NAME thing = (PVAR_NAME)piar._o1;
			if( _state._alActionNames.contains(thing) ){
				_bRewardActionDependent = true;
				break;
			}
		}

		List<NavigableMap<String, Boolean>> acts = null;
		if( !withActionVars ){
			_actionCpts = new HashMap<Map<String,Boolean>,Map<String,ADDRNode>>();
			acts = getFullRegressionOrder();
		}else{
			_cpts = new TreeMap< String, ADDRNode >();
		}

		for( int act = 0 ; (act == 0) || (!withActionVars && act < acts.size() ); ++act ){

			if( !withActionVars && __debug_level.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.println("Building CPT for action : " + acts.get(act) );
				for( Map.Entry<String, Boolean> ent : acts.get(act).entrySet() ){
					System.out.println( (ent.getKey()) + " " + ent.getValue() );
				}
			}

			for (int iter = 0; iter <= 1; iter++) {

				TreeMap<PVAR_NAME,ArrayList<ArrayList<LCONST>>> src = 
						iter == 0 ? _state_vars : _observ_vars;

				for (Map.Entry<PVAR_NAME,ArrayList<ArrayList<LCONST>>> e : src.entrySet()) {

					// Go through all variable names p for a variable type
					PVAR_NAME p = e.getKey();
					ArrayList<ArrayList<LCONST>> assignments = e.getValue();

					CPF_DEF cpf = _state._hmCPFs.get(new PVAR_NAME(p.toString() + 
							(iter == 0 ? "'" : "")));

					HashMap<LVAR,LCONST> subs = new HashMap<LVAR,LCONST>();
					for (ArrayList<LCONST> assign : assignments) {

						final String cpt_var = CleanFluentName(p.toString() + assign);
						final String ns_var = (cpt_var+"'").intern();
						
						if( __debug_level.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ) {
							System.out.println("Processing: " + ns_var );
						}

						subs.clear();
						for (int i = 0; i < cpf._exprVarName._alTerms.size(); i++) {
							LVAR v = (LVAR)cpf._exprVarName._alTerms.get(i);
							LCONST c = (LCONST)assign.get(i);
							subs.put(v,c);
						}

						HashSet< UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > relevant_vars 
							= new HashSet< UnorderedPair<PVAR_NAME, ArrayList<LCONST> > >();
						
						EXPR cpf_expr = cpf._exprEquals;

						if (_bCPFDeterministic) // collectGFluents expects a distribution so convert to a Delta function if needed
							cpf_expr = new KronDelta(cpf_expr);					

						cpf_expr.collectGFluents(subs, _state, relevant_vars);

						if( !withActionVars ){
							Map<String, Boolean> action = acts.get(act);
							setAction(action);
							relevant_vars = removeActionVars(relevant_vars);
						}

						add2InfluenceMap( ns_var, relevant_vars, _action_vars);

						if( __debug_level.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ) {
							System.out.println("Relevant variables : " + relevant_vars);
						}

						ADDRNode cpt = enumerateAssignments( relevant_vars, cpf_expr, subs );

						//build dual diagram

						ADDRNode prob_true = cpt;

						ADDRNode prob_false = _manager.apply(_manager.DD_ONE, prob_true, DDOper.ARITH_MINUS);

						cpt = _manager.getINode( ns_var, prob_true, prob_false );

						if( withActionVars ){
							if (iter == 0) {
								_cpts.put( ns_var, cpt);
							}else {
								throw new Exception("observ vars not handled yet.");
							}
						}else{
							Map<String, Boolean> thisAct = acts.get(act);
							Map<String, ADDRNode> inner = _actionCpts.get( thisAct );
							if( inner == null ){
								inner = new TreeMap<String, ADDRNode >();
								_actionCpts.put( thisAct, inner );
							}
							inner.put( ns_var, cpt);
						}

						_manager.addPermenant(cpt);
						_manager.flushCaches( );
						if( __debug_level.compareTo(DEBUG_LEVEL.DIAGRAMS) == 0 ) {
							System.out.println("Displaying CPT for " + ns_var );
							_manager.showGraph(cpt);
							System.out.println("Press any key");
							System.in.read();
						}
					}
				}
				_manager.flushCaches();
			}

			////
			final ArrayList<ADDRNode> this_reward 
				= convertAddExpr2ADD(rew_expr, !withActionVars);
			if( !withActionVars ){
				rew_expr.collectGFluents(new HashMap<LVAR, LCONST>(), _state, rewardRelVars );
				Map<String, Boolean> action = acts.get(act);
				setAction(action);
				rewardRelVars = removeActionVars(rewardRelVars);
				Map<String, Boolean> thisAct = acts.get(act);
				_actionRewards.put( thisAct, this_reward );
			}else if( _rewards == null ){
				_rewards = this_reward;
			}

			for( ADDRNode rew : this_reward ){
				_manager.addPermenant(rew);
			}

			if( __debug_level.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.println("Reward relevant vars : " + rewardRelVars );
			}
				
			_manager.flushCaches( );

			if( __debug_level.compareTo(DEBUG_LEVEL.DIAGRAMS) == 0 ) {
				System.out.println("Displaying reward for action " 
						+ ( withActionVars ? "all" : acts.get(act) ) );
				
				for( ADDRNode rew : _rewards ){
					_manager.showGraph(rew);
				}
				System.out.println("Press key");
				System.in.read();
			}

			if( !withActionVars ){
				unsetAction( acts.get(act) );
			}
		}
	}

	public ADDRNode getIIDUniformDistribution( final String... vars ){
		final double total_mass = Math.pow( 0.5, vars.length );
		return _manager.getLeaf(total_mass);
	}

	public ADDRNode getIIDBernoulliDistribution( final double prob_true,
			final String... vars ){
		ADDRNode ret = _manager.DD_ONE;
		for( final String var : vars ){
			ret = _manager.apply( ret, 
					_manager.getINode(var, 
							_manager.getLeaf(prob_true),
							_manager.getLeaf(1-prob_true) ),
					DDOper.ARITH_PROD );
		}
		return ret;
	}
	
	public ADDRNode getIIDConjunction( final boolean truth, 
			final String... vars ){
		ADDRNode ret = _manager.DD_ONE;
		for( final String var : vars ){
			ret = _manager.BDDIntersection(
					ret, 
					_manager.getIndicatorDiagram(var, truth) );
		}
		return ret;
	}
	
	public ADDRNode getSumOfIndicators( final boolean truth, 
			final String... vars ){
		ADDRNode ret = _manager.DD_ZERO;
		for( final String var : vars ){
			ret = _manager.apply( ret, 
					_manager.getIndicatorDiagram(var, truth),
					DDOper.ARITH_PLUS );
		}
		return ret;
	}
	
	public ADDRNode getIIDDisjunctionDistribution( final boolean truth,
			final String... vars ){
		ADDRNode conjunct = getIIDConjunction(!truth, vars);
		return _manager.BDDNegate( conjunct );
	}
			
	private void unsetAction(Map<String, Boolean> assignment) {

		for( Map.Entry<String, Boolean> assign : assignment.entrySet() ){

			String name = assign.getKey();

			PVAR_NAME pvar = getPVar(name);
			
			ArrayList<LCONST> terms = getTerms(pvar, name);

			_state.setPVariableAssign(pvar, terms, null );

		}

	}
	
	private ArrayList< UnorderedPair< HashMap<LVAR,LCONST>, EXPR > > getAdditiveComponents(EXPR e) throws Exception {

		ArrayList< UnorderedPair< HashMap<LVAR,LCONST>, EXPR > > ret 
			= new ArrayList< UnorderedPair< HashMap<LVAR,LCONST>, EXPR > >();

		if (e instanceof OPER_EXPR && ((OPER_EXPR)e)._op == OPER_EXPR.PLUS) {

			OPER_EXPR o = (OPER_EXPR)e;

			//System.out.println("\n- Oper Processing " + o._e1);
			//System.out.println("\n- Oper Processing " + o._e2);

			ret.addAll(getAdditiveComponents(o._e1));
			ret.addAll(getAdditiveComponents(o._e2));

		} else if (e instanceof AGG_EXPR && ((AGG_EXPR)e)._op == AGG_EXPR.SUM) {

			AGG_EXPR a = (AGG_EXPR)e;

			ArrayList<ArrayList<LCONST>> possible_subs = _state.generateAtoms(a._alVariables);
			HashMap<LVAR,LCONST> subs = new HashMap<LVAR,LCONST>();

			//System.out.println("\n- Sum Processing " + a);

			// Evaluate all possible substitutions
			for (ArrayList<LCONST> sub_inst : possible_subs) {
				for (int i = 0; i < a._alVariables.size(); i++) {
					subs.put(a._alVariables.get(i)._sVarName, sub_inst.get(i));
				}

				// Note: we are not currently decomposing additive structure below a sum aggregator
				ret.add( new  UnorderedPair< HashMap<LVAR,LCONST>, EXPR > 
					(	(HashMap<LVAR, LCONST>) subs.clone() , a._e) );			

				subs.clear();
				
			}

		} else {
			//System.out.println("\n- General Processing " + e);
			HashMap<LVAR,LCONST> empty_subs = new HashMap<LVAR,LCONST>();
			ret.add( new  UnorderedPair< HashMap<LVAR,LCONST>, EXPR > (empty_subs, e) );
		}

		return ret;
	}

	
	private ArrayList<ADDRNode> convertAddExpr2ADD(EXPR e, boolean filter_actions) throws Exception {

		ArrayList<ADDRNode> adds = new ArrayList<ADDRNode>();
		ArrayList<UnorderedPair<HashMap<LVAR,LCONST>,EXPR>> exprs = getAdditiveComponents(e);

//		System.out.println( "Additive components " + exprs );
		//		System.out.println("\n");
//		for ( UnorderedPair<HashMap<LVAR,LCONST>,EXPR> p : exprs) {
//			String str = "";
//			if (p._o2 instanceof RDDL.OPER_EXPR)
//				str = ((RDDL.OPER_EXPR)p._o2)._op;
//			//			System.out.println("Found pair: " + p._o1 + " -- " + p._o2.getClass() + " / " + str + "\n" + p);
//		}

		for (UnorderedPair<HashMap<LVAR,LCONST>,EXPR> p : exprs) {

			HashSet< UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > relevant_vars 
				= new HashSet< UnorderedPair< PVAR_NAME, ArrayList<LCONST> > >();
			
			HashMap<LVAR, LCONST> subs = p._o1;
			
			EXPR e2 = ((EXPR)p._o2);
			
			if ( _d._bRewardDeterministic) // collectGFluents expects distribution
				e2 = new DiracDelta(e2);
			else{
				System.out.println("WARNING: May not convert additive reward correctly... check results.");
			}

			e2.collectGFluents(subs, _state, relevant_vars);
			
			if( filter_actions ){
				relevant_vars = removeActionVars(relevant_vars);
			}

			//				System.out.println("  - relevant vars: " + relevant_vars);

			ADDRNode add = enumerateAssignments( relevant_vars, e2, subs);
			
			if ( !add.equals(_manager.DD_ZERO) ) {
				adds.add(add);
			}
			
		}
		//		System.out.println("Done processing additive expression");

		return adds;
	
	}

	
	private void add2InfluenceMap(String cpt_var, 
			HashSet< UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > relevant_vars, 
			TreeMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> action_vars) {

		for( UnorderedPair<PVAR_NAME, ArrayList<LCONST> > pair : relevant_vars ){
			PVAR_NAME pvar = (PVAR_NAME)pair._o1;
			ArrayList<LCONST> term = (ArrayList<LCONST>)pair._o2;

			//			System.out.println(pvar.toString() + " " + term + " " + (action_vars.get(pvar)!=null) );

			if( action_vars.get(pvar) != null ){

				String actname = CleanFluentName(pvar.toString() + term);

				if( _hmact2vars.get(actname) == null  ){
					_hmact2vars.put(actname, new ArrayList<String>());
				}

				_hmact2vars.get(actname).add(cpt_var);

				if( _hmvars2act .get(cpt_var) == null  ){
					_hmvars2act.put(cpt_var, new ArrayList<String>());
				}

				_hmvars2act.get(cpt_var).add(actname);


			}

		}

		//		System.exit(1);

	}


	private HashSet< UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > 
		removeActionVars(HashSet< UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > vars) {

		HashSet< UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > ret 
			= new HashSet< UnorderedPair<PVAR_NAME, ArrayList<LCONST> > >();

		for( UnorderedPair<PVAR_NAME, ArrayList<LCONST> > p : vars ){
			PVAR_NAME pv = (PVAR_NAME)p._o1;
			if( _state._alActionNames.contains(pv) ){
				continue;
			}else{
				ret.add(p);
			}
		}

		return ret;
	}
	
	private ArrayList<LCONST> getTerms(PVAR_NAME pvar, String name) {

		//		System.out.println( "PVAR : " + pvar );


		try {
			ArrayList<ArrayList<LCONST>> allTerms = _state.generateAtoms(pvar);
			for( ArrayList<LCONST> terms : allTerms ){
				if( CleanFluentName( pvar._sPVarName + terms ).equals(name) ){
					return terms;
				}
			}

		} catch (EvalException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return null;

	}


	private PVAR_NAME getPVar(final String name) {

		String ret = name.toLowerCase();
		ret = ret.replace('_', '-');
		//		System.out.println(name);

		for( PVAR_NAME state : _state._alActionNames){
			if( ret.startsWith(state._sPVarName) ){
				return state;
			}
		}

		//		for( PVAR_NAME state : _state._alStateNames){
		//			if( name.startsWith(state._sPVarName) ){
		//				return state;
		//			}
		//		}
		//		
		//		for( PVAR_NAME state : _state._alObservNames){
		//			if( name.startsWith(state._sPVarName) ){
		//				return state;
		//			}
		//		}

		return null;

	}


	private void setAction(Map<String, Boolean> assignment) {

		for( Map.Entry<String, Boolean> assign : assignment.entrySet() ){
			
			String name = (assign.getKey());

			PVAR_NAME pvar = getPVar(name);
			
			if( pvar == null ){
				try{
					throw new Exception("null here " + name);
				}catch( Exception e ){
					e.printStackTrace();
					System.exit(2);					
				}
			}
			
			ArrayList<LCONST> terms = getTerms(pvar, name);

			_state.setPVariableAssign(pvar, terms, assign.getValue() );

		}

	}
	
	public List<NavigableMap<String, Boolean>> getFullRegressionOrder() {

		if( _regOrder == null ){
			_regOrder = new ArrayList<NavigableMap<String, Boolean>>();
			Set<NavigableMap<String, Boolean>> partialRegOrder 
				= _manager.enumeratePaths( _concurrencyConstraint, false, true, (ADDLeaf)(_manager.DD_ONE.getNode()), false );
			//partial paths need to be filled with all possible values 
			//for ininstantiated variables
			
			if( partialRegOrder.isEmpty() ){
				ArrayList<String> otherVars = new ArrayList<String>( 
						_actionVars );
				_regOrder.addAll( enumerateOthers( otherVars ) );
			}else{
				for( NavigableMap<String, Boolean> act : partialRegOrder ){
					if( act.size() != _defaultAct.size() ){
						ArrayList<String> otherVars = new ArrayList<String>( 
								_actionVars );
						otherVars.removeAll( act.keySet() );
						List< NavigableMap<String, Boolean> > otherAssigns =
								enumerateOthers( otherVars );
						for( NavigableMap<String, Boolean> otherAssign 
								: otherAssigns ){
							NavigableMap<String, Boolean> fullAssign 
								= new TreeMap<String, Boolean>( act );
							fullAssign.putAll( otherAssign );
							_regOrder.add( fullAssign );
						}
					}else{
						_regOrder.add( act );
					}
				}	
			}
			System.out.println("Full actions # " + _regOrder.size() );
		}

		return _regOrder;
	}

	private List<NavigableMap<String, Boolean>> enumerateOthers(
			final ArrayList<String> otherVars) {
		List<boolean[]> assigns = enumerateBooleans( 0,
				new boolean[otherVars.size()], new ArrayList<boolean[]>() );
		List< NavigableMap<String, Boolean> > ret 
			= new ArrayList< NavigableMap<String, Boolean> >();
		for( boolean[] assign : assigns ){
			NavigableMap<String, Boolean> thisAssign = 
					new TreeMap<String, Boolean>();
			for( int i = 0 ; i < assign.length; ++i ){
				thisAssign.put( otherVars.get(i), assign[i] );
			}
			ret.add( thisAssign );
		}
		return ret;
	}

	private List<boolean[]> enumerateBooleans( int index, 
			boolean[] curAssign, List<boolean[]> assigns ) {
		
		if( index == curAssign.length ){	
			assigns.add( Arrays.copyOf(curAssign, curAssign.length) );
		}else{
			//set true
			curAssign[ index ] = true;
			//recurse
			enumerateBooleans(index+1, curAssign, assigns);
			//set false
			curAssign[ index ] = false;
			enumerateBooleans(index+1, curAssign, assigns);
		}
	
		return assigns;
	}
	
	public void makeOrders(){	
		_sumOrder.clear();
		_elimOrder.clear();

		try
		{
			ArrayList<UnorderedPair<Integer, UnorderedPair<PVAR_NAME, ArrayList<LCONST>>>> ret = null;
			ret = getCptOrderSortedByActionVars();
			System.out.println(ret);
			for( UnorderedPair<Integer, UnorderedPair<PVAR_NAME, ArrayList<LCONST>>> p : ret ) {
				UnorderedPair<PVAR_NAME, ArrayList<LCONST>> pvar = p._o2;
				String var = CleanFluentName(pvar._o1.toString()+pvar._o2);
				_sumOrder.add( (var+"'").intern() );
			}
			
		} catch (EvalException e){
			e.printStackTrace();
		}


		for( String s : _varOrder ){ 
			if( _actionVars.contains(s) ){
				_elimOrder.add(s);
			}
		}

		Collections.reverse(_elimOrder);

		if( __debug_level.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
			System.out.println("Expectation order: " + _sumOrder);
			System.out.println("Max order: " + _elimOrder);
		}
		
	}
	
	protected void makePrimeRemap() {
		for( String s : _stateVars ) {
			String ns = (s + "'").intern();
			_hmPrimeRemap.put(s , ns);
			_hmPrimeUnMap.put(ns, s );
		}
	}
	
	public ADDRNode getVMax(final int depth , final int steps_lookahead ){
		
		if( depth == -1 ){
			return 
				getDiscount() == 1 ? 
						_manager.getLeaf( getRMax()*getHorizon() ) :
						_manager.getLeaf( getRMax() * 1.0d/(1.0d-getDiscount()) );
		}else{
			if( getDiscount() == 1 ){
				
				return _manager.getLeaf( getRMax() * (steps_lookahead-depth) );
			}else{
				double ret = 0;
				double cur_disc = 1;
				for( int i = depth; i < steps_lookahead; ++i ){
					ret += getRMax() * cur_disc;
					cur_disc = cur_disc * getDiscount();
				}
				return _manager.getLeaf( ret );
			}
		}
	}
	
	public double getRMax(){
		double ret = 0;
		for( final ADDRNode rewards : getRewards() ){
			ret += rewards.getMax();
		}
		return ret;//_manager.getLeaf(ret, ret);
	}
	
	private ArrayList<UnorderedPair<Integer, UnorderedPair<PVAR_NAME, ArrayList<LCONST>>>>  
			getCptOrderSortedByActionVars() throws EvalException{
		
		ArrayList<UnorderedPair<Integer, UnorderedPair<PVAR_NAME, ArrayList<LCONST>>>> cptOrder = 
				new ArrayList<UnorderedPair<Integer,UnorderedPair<PVAR_NAME, ArrayList<LCONST> >>>();

		for (int iter = 0; iter <= 1; iter++) {

			TreeMap<PVAR_NAME,ArrayList<ArrayList<LCONST>>> src = 
					iter == 0 ? _state_vars : _observ_vars;

			for (Map.Entry<PVAR_NAME,ArrayList<ArrayList<LCONST>>> e : src.entrySet()) {

				PVAR_NAME p = e.getKey();
				ArrayList<ArrayList<LCONST>> assignments = e.getValue();

				CPF_DEF cpf = _state._hmCPFs.get(new PVAR_NAME(p.toString() + 
						(iter == 0 ? "'" : "")));

				HashMap<LVAR,LCONST> subs = new HashMap<LVAR,LCONST>();
				for (ArrayList<LCONST> assign : assignments) {

					String cpt_var = CleanFluentName(p.toString() + assign);
					//					System.out.println("Processing: " + cpt_var);


					subs.clear();
					for (int i = 0; i < cpf._exprVarName._alTerms.size(); i++) {
						LVAR v = (LVAR)cpf._exprVarName._alTerms.get(i);
						LCONST c = (LCONST)assign.get(i);
						subs.put(v,c);
					}

					EXPR cpf_expr = cpf._exprEquals;

					HashSet< UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > relvars 
						= new HashSet< UnorderedPair<PVAR_NAME, ArrayList<LCONST> > >();
					
					cpf_expr.collectGFluents(subs, _state, relvars);

					int count = 0;

					for( UnorderedPair<PVAR_NAME, ArrayList<LCONST> > pa : relvars ){
						PVAR_NAME pvar = (PVAR_NAME)pa._o1;
						ArrayList<LCONST> ass = (ArrayList<LCONST>)pa._o2;
						String name = CleanFluentName(pvar.toString()+ass);

						if( _state.getPVariableType(pvar) == _state.ACTION ){
							++count;
						}
					}

					cptOrder.add(new UnorderedPair<Integer, UnorderedPair<PVAR_NAME, ArrayList<LCONST> > >
						(count, new UnorderedPair<PVAR_NAME, ArrayList<LCONST> >(p, assign)) );
				}
			}
		}

		//		System.out.println(cptOrder);

		Comparator<UnorderedPair<Integer, UnorderedPair<PVAR_NAME, ArrayList<LCONST>>>> cptComparator = 
				new Comparator<UnorderedPair<Integer, UnorderedPair<PVAR_NAME, ArrayList<LCONST>>>>() {

			public int compare(UnorderedPair<Integer, UnorderedPair<PVAR_NAME, ArrayList<LCONST>>> o1,
					UnorderedPair<Integer, UnorderedPair<PVAR_NAME, ArrayList<LCONST>>> o2) {

				if( o1._o1.compareTo(o2._o1) != 0 ){
					return o1._o1.compareTo(o2._o1);
				}else{
					if( o1._o2._o1.compareTo(o2._o2._o1) != 0 ){
						return o1._o2._o1.compareTo(o2._o2._o1);
					}else{
						return 0;
					}
				}

			}
		};

		Collections.sort(cptOrder, cptComparator);
		//		Collections.reverse(cptOrder);
		return cptOrder;
	}
	
	private ArrayList<String> guessOrdering() throws EvalException {

		ArrayList<UnorderedPair<Integer, UnorderedPair<PVAR_NAME, ArrayList<LCONST>>>> cptOrder = 
				getCptOrderSortedByActionVars();

		ArrayList<String> order = new ArrayList<String>();

		for ( UnorderedPair<Integer, UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > cpt : cptOrder ) {

			PVAR_NAME p = cpt._o2._o1;
			CPF_DEF cpf = _state._hmCPFs.get(new PVAR_NAME(p.toString()+ "'"));
			HashMap<LVAR,LCONST> subs = new HashMap<LVAR,LCONST>();
			ArrayList<LCONST> assign = cpt._o2._o2;
			String cpt_var = CleanFluentName(p.toString() + assign);
			subs.clear();
			for (int i = 0; i < cpf._exprVarName._alTerms.size(); i++) {
				LVAR v = (LVAR)cpf._exprVarName._alTerms.get(i);
				LCONST c = (LCONST)assign.get(i);
				subs.put(v,c);
			}

			EXPR cpf_expr = cpf._exprEquals;

			HashSet<UnorderedPair<PVAR_NAME, ArrayList<LCONST>>> relvars 
				= new HashSet<UnorderedPair<PVAR_NAME, ArrayList<LCONST>>>();
			
			cpf_expr.collectGFluents(subs, _state, relvars);

			for( UnorderedPair<PVAR_NAME, ArrayList<LCONST>> pa : relvars ){
				PVAR_NAME pvar = (PVAR_NAME)pa._o1;
				ArrayList<LCONST> ass = (ArrayList<LCONST>)pa._o2;
				String name = CleanFluentName(pvar.toString()+ass);

				if( !order.contains(name) && _state.getPVariableType(pvar) == _state.STATE ){
					order.add(name);	
				}

			}

			for( UnorderedPair<PVAR_NAME, ArrayList<LCONST>> pa : relvars ){
				PVAR_NAME pvar = (PVAR_NAME)pa._o1;
				ArrayList<LCONST> ass = (ArrayList<LCONST>)pa._o2;
				String name = CleanFluentName(pvar.toString()+ass);

				if( !order.contains(name) && _state.getPVariableType(pvar) == _state.ACTION ){
					order.add(name);	
				}

			}

			order.add( (cpt_var+"'").intern() );

		}

		System.out.println("Ordering : " + order);

		for( String s : _stateVars ) {
			if( !order.contains(s) ) {
				order.add(s);
			}
		}

		for( String a : _actionVars ) {
			if( !order.contains(a) ) {
				order.add(a);
			}
		}

		if( order.size() != 2*_stateVars.size() + _actionVars.size() ){
			System.err.println("size of ordering not equal to size of variables");
			System.out.println(order);
			System.out.println(_stateVars);
			System.out.println(_actionVars);

			System.exit(1);
		}

		if( __debug_level.compareTo(DEBUG_LEVEL.PROBLEM_INFO) >= 0 ) {
			System.out.println("Problem has " + order.size() + " variables ");
			System.out.println("State vars : " + _stateVars.size());
			System.out.println("Action vars : " + _actionVars.size());
			System.out.println("Ordering : " + order);	
		}

		

		return order;
	}

	
	private void initADD() throws EvalException {
		ArrayList<String> Order = null;
		_nextStateVars = _rddlStateSpace.getNextStateVars();
		
		if( _order != ORDER.GUESS ){
			Order  = makeOrdering();
		}else{
			Order = guessOrdering();
		}
		_manager = new ADDManager(MANAGER_STORE_INIT_SIZE,
				MANAGER_STORE_INCR_SIZE, Order,42);
		_varOrder = Order;
		_totalVariables = Order.size();
		_numStateVars = _stateVars.size();
		_numActionVars = _actionVars.size();
	}

	private ArrayList<String> makeOrdering() throws EvalException {

		ArrayList<String> order = new ArrayList<String>();

		switch(_order){

		case INTERLEAVE:

			for( String s : _stateVars ) {
				order.add(s);
				order.add( (s+"'").intern() );
			}
			order.addAll(_actionVars);
			break;

		case AXPX:
			order.addAll(_actionVars);
			order.addAll(_nextStateVars);
			order.addAll(_stateVars);
			break;

		case AXXP:
			order.addAll(_actionVars);
			order.addAll(_stateVars);
			order.addAll(_nextStateVars);
			break;

		case XAXP:

			order.addAll(_stateVars);
			order.addAll(_actionVars);
			order.addAll(_nextStateVars);
			break;

		case XPAX:

			order.addAll(_nextStateVars);
			order.addAll(_actionVars);
			order.addAll(_stateVars);
			break;

		case XPXA:

			order.addAll(_nextStateVars);
			order.addAll(_stateVars);
			order.addAll(_actionVars);
			break;

		default:
			System.err.println("Unknown ordering...");

		case XXPA:


			order.addAll(_stateVars);
			order.addAll(_nextStateVars);
			order.addAll(_actionVars);
		}

		if( __debug_level.compareTo(DEBUG_LEVEL.PROBLEM_INFO) >= 0 ) {
			System.out.println("Problem has " + order.size() + " variables");
			System.out.println("Ordering : " + order);
		}

		return order;
	}

	
	private void initializeState(INSTANCE pI, DOMAIN pD, NONFLUENTS pN) {
		_state.init(pN != null ? pN._hmObjects : null, pI._hmObjects,  
				pD._hmTypes, pD._hmPVariables, pD._hmCPF,
				pI._alInitState, pN == null ? null : pN._alNonFluents, 
						pD._alStateConstraints, pD._exprReward, pI._nNonDefActions);
	}

	private void initRDDLData() throws EvalException {

		_state_vars  = collectStateVars();
		_action_vars = collectActionVars();
		_observ_vars = collectObservationVars();
		
		_stateVars = new TreeSet<String>();
		_actionVars = new TreeSet<String>();
		_nextStateVars = new TreeSet<String>();
		
		_state_constraints = new TreeSet<ADDRNode>();
		_action_constraints = new TreeSet<ADDRNode>();
		_action_preconditions = new TreeSet<ADDRNode>();

		for (Map.Entry<PVAR_NAME,ArrayList<ArrayList<LCONST>>> e : _state_vars.entrySet()) {
			PVAR_NAME p = e.getKey();
			ArrayList<ArrayList<LCONST>> assignments = e.getValue();
			for (ArrayList<LCONST> assign : assignments) {
				String name = CleanFluentName(p.toString() + assign);
				_tmStateVars.put(name, new UnorderedPair<PVAR_NAME, ArrayList<LCONST> >(p, assign));
				_stateVars.add(name);
			}
		}

		for (Map.Entry<PVAR_NAME,ArrayList<ArrayList<LCONST>>> e : _action_vars.entrySet()) {
			PVAR_NAME p = e.getKey();
			ArrayList<ArrayList<LCONST>> assignments = e.getValue();
			for (ArrayList<LCONST> assign : assignments) {
				String name = CleanFluentName(p.toString() + assign);
				_tmActionVars.put(name, new UnorderedPair<PVAR_NAME, ArrayList<LCONST> >(p, assign));
				_actionVars.add(name);
			}
		}

	}
	
	private TreeMap<PVAR_NAME,ArrayList<ArrayList<LCONST>>> collectActionVars() 
			throws EvalException {

		TreeMap<PVAR_NAME,ArrayList<ArrayList<LCONST>>> action_vars = 
				new TreeMap<PVAR_NAME,ArrayList<ArrayList<LCONST>>>();

		for (PVAR_NAME p : _state._alActionNames) {
			ArrayList<ArrayList<LCONST>> gfluents = _state.generateAtoms(p);
			action_vars.put(p, gfluents);
		}

		return action_vars;
	}

	private TreeMap<PVAR_NAME,ArrayList<ArrayList<LCONST>>> collectObservationVars() 
			throws EvalException {

		TreeMap<PVAR_NAME,ArrayList<ArrayList<LCONST>>> observ_vars = 
				new TreeMap<PVAR_NAME,ArrayList<ArrayList<LCONST>>>();

		for (PVAR_NAME p : _state._alObservNames) {
			ArrayList<ArrayList<LCONST>> gfluents = _state.generateAtoms(p);
			observ_vars.put(p, gfluents);
		}

		return observ_vars;
	}


	private TreeMap<PVAR_NAME,ArrayList<ArrayList<LCONST>>> collectStateVars() throws EvalException {

		TreeMap<PVAR_NAME,ArrayList<ArrayList<LCONST>>> state_vars = 
				new TreeMap<PVAR_NAME,ArrayList<ArrayList<LCONST>>>();

		for (PVAR_NAME p : _state._alStateNames) {
			ArrayList<ArrayList<LCONST>> gfluents = _state.generateAtoms(p);
			state_vars.put(p, gfluents);
		}

		return state_vars;
	}

	public static void main(String[] args) {
//		new RDDL2ADD( args[0], args[1], false, 
//				DEBUG_LEVEL.SOLUTION_INFO, ORDER.GUESS, true , 42);
		testMakeTransition();
	}

	public ADDManager getManager() {
		return _manager;
	}

	@Override
	public RDDLFactoredStateSpace getFactoredStateSpace() {
		return _rddlStateSpace;
	}
	
	@Override
	public RDDLFactoredActionSpace getFactoredActionSpace() {
		return _rddlActionSpace;
	}
	
	@Override
	public RDDLFactoredReward getFactoredReward() {
		return _rddlReward;
	}
	
	public RDDLFactoredTransition getFactoredTransition() {
		return _rddlTransition;
	}

	public State getState() {
		return _state;
	}

	public int getHorizon() {
		return _i._nHorizon;
	}

	public double getDiscount() {
		return _i._dDiscount;
	}

	public List<String> getAffectingActionVariables( String next_state_var ) {
		return _hmvars2act.get( next_state_var );
	}

	public Set<ADDRNode> getActionPreconditions() {
		return _action_preconditions;
	}

	public Set<ADDRNode> getActionConstraints() {
		return _action_constraints;
	}

	public Set<ADDRNode> getStateConstraints() {
		return _state_constraints;
	}

	public static void testMakeTransition(){
//		new RDDL2ADD("./rddl/sysadmin_mdp.rddl", "./rddl/sysadmin_star_7_3.rddl",
//				true, DEBUG_LEVEL.SOLUTION_INFO, ORDER.GUESS, 
//				true, 42 ).makeTransitionRelation( true );
		new RDDL2ADD("./rddl/sysadmin_mdp.rddl", "./rddl/sysadmin_star_7_3.rddl",
				false , DEBUG_LEVEL.SOLUTION_INFO, ORDER.GUESS, 
				true, 42 ).makeTransitionRelation( false );
	}
	
	@Override
	public void makeTransitionRelation( final boolean withActionVars ){
		if( withActionVars ){
			makeTransitionRelationFAR();
		}else{
			makeTransitionRelationSPUDD();
		}
		return;
	}
	
	@Override
	public void makeTransitionRelationSPUDD(){
		final List<NavigableMap<String, Boolean>> all_actions = getFullRegressionOrder();
		 _actionTransitionRelation 
		 	= new HashMap<Map<String,Boolean>, Map<String,ADDRNode>>();
		 _actionTransitionRelationLast 
		 	= new HashMap<NavigableMap<String,Boolean>, ArrayList<Pair<String,String>>>();
		 
		for( final NavigableMap<String, Boolean> action : all_actions ){
			final Map<String, ADDRNode> theCPTs = _actionCpts.get(action);
			final Map<String, ADDRNode> inner = new TreeMap<String, ADDRNode>();
			final ArrayList<Pair<String, String>> inner_last = new ArrayList<Pair<String, String>>();
			final String[] lastSeen = new String[ _totalVariables ];
			
			for( final String ns_var : _sumOrder ){
				final int ns_index = _varOrder.indexOf( ns_var );
				final ADDRNode theCPT = theCPTs.get( ns_var );
				final ADDRNode theRelation = 
						_manager.apply(
								_manager.DD_ONE, 
								_manager.threshold( theCPT, 0.0d, false ),
								DDOper.ARITH_MINUS );
				inner.put( ns_var, theRelation );
				final Set<String> vars = _manager.getVars( theRelation ).get( 0 );
				for( final String var : vars ){
					if( isActionVariable( var ) ){
						try{
							throw new Exception("action variable in SPuDD style relation");
						}catch( Exception e ){
							e.printStackTrace();
							System.exit(1);
						}
					}
					final int index = _varOrder.indexOf( var );
					if( index != ns_index ){
						lastSeen[ index ] = ns_var;
					}
				}
			}
		/////
			for( int i = 0 ; i < lastSeen.length; ++i ){
				final String lastVar = lastSeen[ i ];
				final String thisVar = _varOrder.get(i);
				if( lastVar == null && ( isNextStateVariable( thisVar ) ||
						isActionVariable( thisVar ) ) ){ //because both next state vars and 
					continue;
				}
				inner_last.add( new Pair<String, String>( lastVar,  thisVar ) );
			}
			Collections.sort( inner_last, new Comparator< Pair<String, String> >() {
				@Override
				public int compare(Pair<String, String> o1, Pair<String, String> o2) {
					if( o1._o1 == null ){
						return -1;
					}else if( o2._o1 == null ){
						return 1;
					}
					final int index1 = _sumOrder.indexOf( o1._o1 );
					final int index2 = _sumOrder.indexOf( o2._o1 );
					return index1 - index2; 
				}
			});
					////
			_actionTransitionRelationLast.put( action, inner_last );
			_actionTransitionRelation.put( action,  inner );
		}
	}
	//convert ADD CPTs to BDDs
	//call threshold()
	@Override
	public void makeTransitionRelationFAR(  ) {
		System.out.println("Making transition relation");
		if( _transitionRelation == null ){
			_transitionRelation = new TreeMap<String, ADDRNode>();
			_transition_relation_last = new ArrayList< Pair< String, String> >();
		}
		//do in same order as sum order
		//so that only last seen variable can be stored for parents
		//can dynamically move existential quant. around
		final String[] lastSeen = new String[ _totalVariables ];
		for( final String nextVar : _sumOrder ){
			System.out.println( nextVar );
			
			final ADDRNode theCpt = _cpts.get(nextVar);
			ADDRNode theRelation = _manager.threshold(theCpt, 0.0d, false );
			theRelation = _manager.apply( _manager.DD_ONE, theRelation, DDOper.ARITH_MINUS );
			_transitionRelation.put( nextVar , theRelation );
//			System.out.print("CPT nodes " );
//			System.out.print( _manager.countNodes(theCpt) );
//			System.out.print("\nTransition relation nodes ");
//			System.out.print( _manager.countNodes(theRelation) );
//			System.out.print("\nCPT relevant vars " );
//			System.out.print(_manager.getVars(theCpt) );
//			System.out.print("\nRelation relevant vars " );
			Set<String> vars = _manager.getVars( theRelation ).get(0);
//			System.out.print( vars );
			final int ns_index = _varOrder.indexOf( nextVar );
			for( final String v : vars ){
				final int index = _varOrder.indexOf( v );
				if( index != ns_index ) {
					lastSeen[ index ] = nextVar;	
				}
			}
		}
		
		for( int i = 0 ; i < lastSeen.length; ++i ){
			final String lastVar = lastSeen[ i ];
			//put nulls in first
			//since they are not in the transition relation
			//they can be immediately quantified
			//from the input BDD
			final String thisVar = _varOrder.get(i);
			if( lastVar == null && isNextStateVariable( thisVar ) ){ //because both next state vars and 
				// vars that are not in relation are last seen null/
				// but NS should not be quantified and 
				// the others can be quantified at the beginning
				continue;
			}
			_transition_relation_last.add( new Pair<String, String>( lastVar,  thisVar ) );
		}
		Collections.sort( _transition_relation_last, new Comparator< Pair<String, String> >() {
			@Override
			public int compare(Pair<String, String> o1, Pair<String, String> o2) {
				if( o1._o1 == null ){
					return -1;
				}else if( o2._o1 == null ){
					return 1;
				}
				final int index1 = _sumOrder.indexOf( o1._o1 );
				final int index2 = _sumOrder.indexOf( o2._o1 );
				return index1 - index2; 
			}
		});
		return;
	}

	public ADDRNode getSumOfRewards() {
		final List<ADDRNode> rewards = getRewards();
		ADDRNode ret = _manager.DD_ZERO;
		
		 for( int i = 0 ; i < rewards.size(); ++i ){
			 ret =  _manager.apply( ret, rewards.get(i), DDOper.ARITH_PLUS );
		 }
		 
		 return ret;
	}

	public double getRmin() {
		double ret = 0;
		for( final ADDRNode rewards : getRewards() ){
			ret += rewards.getMin();
		}
		return ret;
		
	}

//	public double getReward(final NavigableMap<String, Boolean> assign) {
//		//returns a lazy max if assign is a partial one
//		double ret = 0;
//		for( final ADDRNode rew : getRewards() ){
//			ret += _manager.restrict(rew, assign).getMax();
//		}
//		return ret;
//	}

	public Boolean getDefaultValue(final String svar) {
	    if( _stateVars.contains( svar ) ){
		return (Boolean) _state.getDefaultValue( _tmStateVars.get( svar )._o1 );
	    }else if( _actionVars.contains( svar ) ){
		return (Boolean) _state.getDefaultValue( _tmActionVars.get( svar )._o1 );
	    }
	    return null;
	}

	public boolean isStateVariable( final String s) {
		return _stateVars.contains(s);
	}

}
