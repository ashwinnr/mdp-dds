package rddl.mdp;

import java.util.ArrayList;
import factored.mdp.define.FactoredTransition;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

import add.ADDRNode;


import rddl.RDDL.DOMAIN;
import rddl.RDDL.EXPR;
import rddl.RDDL.INSTANCE;
import rddl.RDDL.LCONST;
import rddl.RDDL.LVAR;
import rddl.RDDL.NONFLUENTS;
import rddl.RDDL.PVAR_NAME;
import rddl.State;
import util.Pair;
import util.UnorderedPair;

import dd.DDINode;
import dd.DDLeaf;
import dd.DDManager;
import dd.DDNode;
import dd.DDRNode;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredReward;
import factored.mdp.define.FactoredStateSpace;

public abstract class RDDL2DD<D extends DDNode, DR extends DDRNode<D>,
DI extends DDINode<D,DR,? extends Collection<DR> >, 
		DL extends DDLeaf<?> > {
	
	protected enum DISTRIBUTION{
		UNIFORM, BERNOULLI
	}

	protected Set<String> _nextStateVars = null;
	
	protected Set<String> _stateVars = null;

	protected Set<String> _actionVars = null;

	public Set<String> get_actionVars() {
		return _actionVars;
	}

	public void set_actionVars(Set<String> _actionVars) {
		this._actionVars = _actionVars;
	}

	public Set<String> get_nextStateVars() {
		return _nextStateVars;
	}

	public void set_nextStateVars(Set<String> _nextStateVars) {
		this._nextStateVars = _nextStateVars;
	}

	public Set<String> get_stateVars() {
		return _stateVars;
	}

	public void set_stateVars(Set<String> _stateVars) {
		this._stateVars = _stateVars;
	}

	public TreeMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> get_action_vars() {
		return _action_vars;
	}

	public void set_action_vars(
			TreeMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> _action_vars) {
		this._action_vars = _action_vars;
	}

	protected NavigableMap< String, DR > _cpts;//x' -> dd

	//optional : action by action Cpts
	
	protected Map< Map<String, Boolean>, Map<String, DR> > _actionCpts;
	
	//optional : transitional relation
	protected NavigableMap< String, DR > _transitionRelation = null;
	
	//list of rewards
	protected List<DR> _rewards = null;

	//optional - list of action costs
	Map< Map<String, Boolean> , ArrayList<DR> > _actionRewards 
	= new HashMap<Map<String,Boolean>, ArrayList<DR> >();

	protected Set<DR> _state_constraints;//BDDs
	protected Set<DR> _action_constraints;//BDDs
	protected Set<DR> _action_preconditions;

	public enum DEBUG_LEVEL{
		PROBLEM_INFO, SOLUTION_INFO, DIAGRAMS 
	}

	public enum ORDER{
		XAXP, AXXP, XPAX, XXPA, 
		AXPX, XPXA, GUESS, INTERLEAVE 
	}

	public static String CleanFluentName(String s) {
		s = s.replace("[", "__");
		s = s.replace("]", "");
		s = s.replace(", ", "_");
		s = s.replace(',','_');
		s = s.replace(' ','_');
		s = s.replace('-','_');
		s = s.replace("()","");
		s = s.replace("(", "__");
		s = s.replace(")", "");
		if (s.endsWith("__"))
			s = s.substring(0, s.length() - 2);
		return s.intern();
	};

	public DEBUG_LEVEL __debug_level;

	protected TreeMap<String, UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > _tmStateVars = new TreeMap<String, UnorderedPair<PVAR_NAME,ArrayList<LCONST>>>();
	protected TreeMap<String, UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > _tmNextStateVars = new TreeMap<String, UnorderedPair<PVAR_NAME,ArrayList<LCONST>>>();
	protected TreeMap<String, UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > _tmActionVars = new TreeMap<String, UnorderedPair<PVAR_NAME,ArrayList<LCONST>>>();

	protected TreeMap<String,UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > _tmObservVars = new TreeMap<String, UnorderedPair<PVAR_NAME,ArrayList<LCONST>>>();

	protected HashMap<String, String> _hmPrimeRemap = new HashMap<String, String>();
	protected HashMap<String, String> _hmPrimeUnMap = new HashMap<String, String>();
	
	public Map<String, ArrayList<String> > _hmact2vars = new HashMap<String, ArrayList<String> >();
	public HashMap<String, ArrayList<String> > _hmvars2act = new HashMap<String, ArrayList<String>>();

	protected static final Runtime RUNTIME = Runtime.getRuntime();

	protected ORDER _order;

	protected ArrayList<Integer> _alOrder = new ArrayList<Integer>();

	protected TreeMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>>	_state_vars;

	protected TreeMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>>	_action_vars;

	protected TreeMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>>	_observ_vars;
	
	protected State _state;

	public boolean	_bRewardActionDependent = false;

	protected final ArrayList<String>	_sumOrder = new ArrayList<String>();

	protected final ArrayList<String>	_elimOrder = new ArrayList<String>();

	private final Map<Long, Map<String, Boolean>>	_hmjointActionMap = new HashMap<Long, Map<String,Boolean>>();

	private final Map<Map<String, Boolean>, Long> _hmActionIDCache = new HashMap<Map<String,Boolean>, Long>();

	private  final Map<String, Map<String, Boolean>> _hmJointActions = new HashMap<String, Map<String,Boolean>>() ;

	protected INSTANCE	_i;

	protected NONFLUENTS	_n;

	protected DOMAIN	_d;

	protected Map<String, Boolean> _defaultAct;

	protected Map< Map<String, Boolean>, Map<String, DR> > _actionTransitionRelation;
	
	public abstract DR enumerateAssignments(HashSet<UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > vars, 
			EXPR cpf_expr, HashMap<LVAR,LCONST> subs ) throws Exception ;
	
	public HashMap<String, String> getPrimeRemap() {
		return _hmPrimeRemap;
	}
	

	public Map<String, String> getPrimeUnMap() {
		return _hmPrimeUnMap;
	}
	
	public Map<Map<String, Boolean>, Map<String, DR>> getActionCpts() {
		return _actionCpts;
	}
	
	public Map<String, DR> getCpts() {
		return _cpts;
	}
	
	public boolean isTransitionRelationReady(final boolean withActionVars){
		return ( withActionVars ? _transitionRelation : _actionTransitionRelation ) != null;
	}
	
	public List<DR> getRewards() {
		return _rewards;
	}
	
	public ArrayList<String> getSumOrder() {
		return _sumOrder;
	}
	
	public ArrayList<String> getElimOrder() {
		return _elimOrder;
	}
	
	public Map<Map<String, Boolean>, ArrayList<DR>> getActionRewards() {
		return _actionRewards;
	}

	public abstract FactoredStateSpace getFactoredStateSpace();
	public abstract FactoredActionSpace<? extends FactoredStateSpace> getFactoredActionSpace();

	public abstract FactoredReward<? extends FactoredStateSpace, ? extends FactoredActionSpace<?>> 
		getFactoredReward();
	public abstract FactoredTransition<? extends FactoredStateSpace, ? extends FactoredActionSpace<?>> 
		getFactoredTransition();

	public abstract void makeTransitionRelation(boolean withActionVars);

	public abstract void makeTransitionRelationFAR();

	public abstract void makeTransitionRelationSPUDD();
	
	public boolean isNextStateVariable(final String var) {
		return _nextStateVars.contains( var );
	}
	
	public boolean isActionVariable(final String var){
		return _actionVars.contains( var );
	}
	
	public Map<Map<String, Boolean>, Map<String, DR>> getTransitionRelationSPUDD(){
		return _actionTransitionRelation;
	}
	
	public NavigableMap<String, DR> getTransitionRelationFAR(){
		return _transitionRelation;
	}
}

