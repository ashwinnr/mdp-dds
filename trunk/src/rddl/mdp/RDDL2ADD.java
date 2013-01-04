package rddl.mdp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import dd.DDManager;
import dd.DDManager.DDOper;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredReward;
import factored.mdp.define.FactoredStateSpace;
import factored.mdp.define.FactoredTransition;

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
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.REAL_CONST_EXPR;
import rddl.State;
import rddl.parser.parser;
import util.UnorderedPair;
import add.ADDINode;
import add.ADDLeaf;
import add.ADDManager;
import add.ADDNode;
import add.ADDRNode;

public class RDDL2ADD extends RDDL2DD<ADDNode, ADDRNode, ADDINode, ADDLeaf> {

	private static final int MANAGER_STORE_INIT_SIZE = 400000;
	private static final int MANAGER_STORE_INCR_SIZE = 10000;
	
	protected ADDManager _manager;
	private boolean _bCPFDeterministic;
	private ArrayList<String> _nextStateVars;
	private ArrayList<String> _varOrder;
	private List<NavigableMap<String, Boolean>> _regOrder;
	private ADDRNode _concurrencyConstraint;
	private RDDLFactoredActionSpace _rddlActionSpace;
	private RDDLFactoredStateSpace _rddlStateSpace;
	private RDDLFactoredTransition _rddlTransition;
	private RDDLFactoredReward _rddlReward;
	private Random _rand;
	
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
			} else
				throw new EvalException("Unhandled distribution type: " + e.getClass());
			
//			for( Pair p : vars ) {
//				PVAR_NAME pvar = (PVAR_NAME)p._o1;
//				ArrayList<LCONST> terms = (ArrayList<LCONST>)p._o2;
//				System.out.println(pvar.toString() +  " " + terms + " " + _state.getPVariableAssign(pvar, terms));
//			}
//			System.out.println(prob_true);
			
			// Now build CPT for action variables
			return _manager.getLeaf(prob_true, prob_true);
	
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
	
		_constraints.add( zerodd );
		
		return zerodd;

	}
	
	public RDDL2ADD( final String pDomain, final String pInstance, boolean withActionVars, 
						DEBUG_LEVEL debug, ORDER order, final boolean buildADDs,
						long seed ) {

		__debug_level = debug;
		_order = order;
		_rand = new Random(seed);
		buildFromFile(pDomain, pInstance, withActionVars, buildADDs);
		
	}

	public void buildFromFile(final String pDomain, final String pInstance, boolean withActionVars,
			boolean buildADDs) {

		RDDL _rddltemp = new RDDL();

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
			initADD();
			makePrimeRemap();
			makeOrders();
			if( buildADDs ){
				buildCPTs(withActionVars);
			}
			_rddlActionSpace = new RDDLFactoredActionSpace();
			_rddlActionSpace.setActionVariables(_actionVars);
			
			_rddlStateSpace = new RDDLFactoredStateSpace();
			_rddlStateSpace.setStateVariables(_stateVars);
			
			_rddlTransition = new RDDLFactoredTransition(_state, _rddlStateSpace,
					_rddlActionSpace, _tmStateVars, _rand.nextLong(), _tmActionVars);

			_rddlReward = new RDDLFactoredReward(_state, _tmStateVars, 
					_rand.nextLong(), _tmActionVars);
		}catch(Exception e) {
			e.printStackTrace();
		}
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
			sum = _manager.apply( sum, _manager.getIndicatorDiagram(a, true) , DDOper.ARITH_PLUS );
		}
		
		//threshold
		ADDRNode zeroOneDD = _manager.threshold(sum, _i._nNonDefActions, false);
		
		_constraints.add( zeroOneDD );
		
		_concurrencyConstraint = zeroOneDD;
		
		if( __debug_level.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
			System.out.println("\nDisplaying concurrency constraint");
			_manager.showGraph(zeroOneDD);
		}
		
		return zeroOneDD;
		
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

		addConcurrencyConstraint();

		for( EXPR constraint : _state._alConstraints ){

			if( constraint instanceof BOOL_EXPR ){
				BOOL_EXPR be = (BOOL_EXPR)constraint;
				addConstraint(be);
				_manager.flushCaches(true);
			}else{
				System.err.println("Constraint not tpye of bool expr");
				System.exit(1);
			}
			
		}

		List<NavigableMap<String, Boolean>> acts = null;
		if( !withActionVars ){
			_actionCpts = new HashMap<Map<String,Boolean>,Map<String,ADDRNode>>();
			acts = getFullRegressionOrder();
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

						String cpt_var = CleanFluentName(p.toString() + assign);

						if( __debug_level.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ) {
							System.out.println("Processing: " + cpt_var + "'");
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

						add2InfluenceMap(cpt_var+"'", relevant_vars, _action_vars);

						if( __debug_level.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ) {
							System.out.println("Relevant variables : " + relevant_vars);
						}

						ADDRNode cpt = enumerateAssignments( relevant_vars, cpf_expr, subs );

						//build dual diagram

						ADDRNode prob_true = cpt;

						ADDRNode prob_false = _manager.apply(_manager.DD_ONE, prob_true, DDOper.ARITH_MINUS);

						cpt = _manager.getINode( (cpt_var+"'").intern() , prob_true, prob_false );

						if( withActionVars ){
							if (iter == 0) {
								_cpts.put( cpt_var+"'", cpt);
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
							
							inner.put( cpt_var+"'", cpt);
							
						}

						_manager.addPermenant(cpt);
						_manager.flushCaches(true);
						
						if( __debug_level.compareTo(DEBUG_LEVEL.DIAGRAMS) == 0 ) {
							System.out.println("Displaying CPT for " + cpt_var+"'");
							_manager.showGraph(cpt);
							System.out.println("Press any key");
							System.in.read();
						}
						
					}
				}

			}

			////

			if( !withActionVars ){
				rew_expr.collectGFluents(new HashMap<LVAR, LCONST>(), _state, rewardRelVars );
				Map<String, Boolean> action = acts.get(act);
				setAction(action);
				rewardRelVars = removeActionVars(rewardRelVars);
			}

			if( __debug_level.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.println("Reward relevant vars : " + rewardRelVars );
			}

			try {
				_rewards = convertAddExpr2ADD(rew_expr, !withActionVars);

				if( __debug_level.compareTo(DEBUG_LEVEL.DIAGRAMS) == 0 ) {
					System.out.println("Displaying reward");
					
					for( ADDRNode rew : _rewards ){
						_manager.showGraph(rew);
					}
					System.out.println("Press key");
					System.in.read();
					
				}

			} catch (Exception e) {
				e.printStackTrace();
			}		

			if( !withActionVars ){
				Map<String, Boolean> thisAct = acts.get(act);
				
				ArrayList<ADDRNode> inner = _actionRewards.get( thisAct );
				
				if( inner == null ){
					inner = new ArrayList<ADDRNode>(_rewards);
					_actionRewards.put( thisAct, inner );
				}
				
			}

			for( ADDRNode rew : _rewards ){
				_manager.addPermenant(rew);
			}
			
			_manager.flushCaches(true);

			if( !withActionVars ){
				unsetAction( acts.get(act) );
			}

		}

//		translateReward(withActionVars);

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
				System.err.println("null here");
				System.exit(2);
			}
			
			ArrayList<LCONST> terms = getTerms(pvar, name);

			_state.setPVariableAssign(pvar, terms, assign.getValue() );

		}

	}
	
	public List<NavigableMap<String, Boolean>> getFullRegressionOrder() {

		if( _regOrder == null ){
			_regOrder = new ArrayList<NavigableMap<String, Boolean>>();
			List<NavigableMap<String, Boolean>> partialRegOrder 
				= _manager.enumeratePaths(_concurrencyConstraint, false, true, 1.0d);
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
			
		}
		
//		System.out.println("Full actions: " + _regOrder );
		
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
				_sumOrder.add( (var+"'") );
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
			String ns = s + "'";
			_hmPrimeRemap.put(s , ns);
		}
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

			order.add(cpt_var+"'");

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

		if( _order != ORDER.GUESS ){
			Order  = makeOrdering();
		}else{
			Order = guessOrdering();
		}

		_manager = new ADDManager(MANAGER_STORE_INIT_SIZE,
				MANAGER_STORE_INCR_SIZE, Order);
		
		_varOrder = Order;
	
	}

	private ArrayList<String> makeOrdering() throws EvalException {

		ArrayList<String> order = new ArrayList<String>();

		switch(_order){

		case INTERLEAVE:

			for( String s : _stateVars ) {
				order.add(s);
				order.add(s+"'");
			}

			order.addAll(_actionVars);

			break;

		case AXPX:
			order.addAll(_actionVars);
			order.addAll(getNextStateVars());
			order.addAll(_stateVars);

			break;

		case AXXP:
			order.addAll(_actionVars);
			order.addAll(_stateVars);
			order.addAll(getNextStateVars());
			break;

		case XAXP:

			order.addAll(_stateVars);
			order.addAll(_actionVars);
			order.addAll(getNextStateVars());
			break;

		case XPAX:

			order.addAll(getNextStateVars());
			order.addAll(_actionVars);
			order.addAll(_stateVars);
			break;

		case XPXA:

			order.addAll(getNextStateVars());
			order.addAll(_stateVars);
			order.addAll(_actionVars);
			break;

		default:
			System.err.println("Unknown ordering...");

		case XXPA:


			order.addAll(_stateVars);
			order.addAll(getNextStateVars());
			order.addAll(_actionVars);
		}

		if( __debug_level.compareTo(DEBUG_LEVEL.PROBLEM_INFO) >= 0 ) {
			System.out.println("Problem has " + order.size() + " variables");
			System.out.println("Ordering : " + order);
		}

		return order;
	}

	
	private List<String> getNextStateVars() {

		if( _nextStateVars == null ){
			_nextStateVars = new ArrayList<String>();
			for( String s : _stateVars ){
				_nextStateVars.add( s + "'");
			}
		}
		
		return _nextStateVars;
		
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
		
		_constraints = new TreeSet<ADDRNode>();
		
		_cpts = new TreeMap< String, ADDRNode >();

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
		new RDDL2ADD( args[0], args[1], false, 
				DEBUG_LEVEL.SOLUTION_INFO, ORDER.GUESS, true , 42);
	}

	public ADDManager getManager() {
		return _manager;
	}

	@Override
	public RDDLFactoredStateSpace getFactoredStateSpace() {
		return _rddlStateSpace;
	}
	
	@Override
	public FactoredActionSpace<? extends FactoredStateSpace> getFactoredActionSpace() {
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
}