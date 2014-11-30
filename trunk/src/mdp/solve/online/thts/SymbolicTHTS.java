package mdp.solve.online.thts;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlElementDecl.GLOBAL;

import com.google.common.collect.Maps;

import add.ADDINode;
import add.ADDRNode;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.viz.CrossingTrafficDisplay;
import rddl.viz.StateViz;
import rddl.viz.SysAdminScreenDisplay;
import util.Timer;
import util.UnorderedPair;
import dd.DDManager.APPROX_TYPE;
import dd.DDManager.DDOper;
import dd.DDManager.DDQuantify;
import dtr.add.ADDDecisionTheoreticRegression.BACKUP_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;
import mdp.generalize.trajectory.EBLGeneralization;
import mdp.generalize.trajectory.Generalization;
import mdp.generalize.trajectory.GenericTransitionGeneralization;
import mdp.generalize.trajectory.GenericTransitionGeneralization.Consistency;
import mdp.generalize.trajectory.parameters.EBLParams;
import mdp.generalize.trajectory.parameters.GeneralizationParameters;
import mdp.generalize.trajectory.parameters.GenericTransitionParameters;
import mdp.generalize.trajectory.parameters.OptimalActionParameters;
import mdp.generalize.trajectory.parameters.RewardGeneralizationParameters;
import mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH;
import mdp.generalize.trajectory.type.GeneralizationType;
import mdp.solve.online.Exploration;
import mdp.solve.online.RDDLOnlineActor;
import mdp.solve.solver.HandCodedPolicies;

public abstract class SymbolicTHTS< T extends GeneralizationType, 
P extends GeneralizationParameters<T> > extends RDDLOnlineActor 
implements THTS< RDDLFactoredStateSpace, RDDLFactoredActionSpace >{

	
	protected FactoredState[] trajectory_states;
	protected FactoredAction[] trajectory_actions;
	
	protected boolean CONSTRAIN_NAIVELY = false;
	protected double	EPSILON;
	protected Timer _DPTimer = null;
	
	protected APPROX_TYPE apricodd_type;
	protected double[] apricodd_epsilon;
	protected boolean do_apricodd;
	
//	protected long MB;
//	protected BACKUP_TYPE dp_type;
	protected int nTrials;
	protected double timeOutMins;
	protected int steps_lookahead;
	
	protected ADDRNode[] _valueDD;
	protected ADDRNode[] _policyDD;
	protected ADDRNode[] _visited; 
//	protected ADDRNode[] _solved;
//	private ADDRNode _baseLinePolicy;
	
//	protected static final FactoredAction[] EMPTY_ACTION_ARRAY = {};
	
	protected GenericTransitionGeneralization<T, P> _generalizer;
	protected GenericTransitionParameters<T, P, RDDLFactoredStateSpace, RDDLFactoredActionSpace> _genaralizeParameters;
//	private Exploration< RDDLFactoredStateSpace, RDDLFactoredActionSpace > exploration;  
	protected static FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> cur_action 
		= new FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>( );
	
//	protected boolean truncateTrials;
	protected double _RMAX;
//	protected boolean enableLabelling;
	protected ADDRNode _baseLinePolicy;
//	protected int heuristic_sharing;
	private Random _stateSelectionRand = null;
	protected Random _actionSelectionRandom = null;
//	private List<ADDRNode>  max_rewards = null;
	protected final boolean INIT_REWARD = false;
	
	public enum GLOBAL_INITIALIZATION{
		VMAX, HRMAX
	}

	public enum LOCAL_INITIALIZATION{
		VMAX, VHS, HMINMIN, HRMAX
	}
	
	protected GLOBAL_INITIALIZATION _global_init;
	protected LOCAL_INITIALIZATION _local_init;
	protected boolean _truncateTrials;
	protected boolean _markVisited;
	
	public SymbolicTHTS( 
			final String domain, 
			final String instance, 
			final double epsilon,
			final DEBUG_LEVEL debug, 
			final ORDER order, 
			final boolean useDiscounting, 
			final int numStates,
			final int numRounds,
			final boolean FAR ,
			final boolean constrain_naively ,
			final boolean do_apricodd,
			final double[] apricodd_epsilon,
			final APPROX_TYPE apricodd_type,
			final BACKUP_TYPE heuristic_type,
			final double time_heuristic_mins, 
			final int steps_heuristic, 
			final long MB ,
			final INITIAL_STATE_CONF init_state_conf ,
			final double init_state_prob,
			final BACKUP_TYPE dp_type,
			final int nTrials,
			final double timeOutMins,
			final int steps_lookahead ,
			final Generalization< RDDLFactoredStateSpace, RDDLFactoredActionSpace, T, P > generalizer, 
			final P generalize_parameters_wo_manager ,
			final boolean gen_fix_states,
			final boolean gen_fix_actions,
			final int gen_num_states,
			final int gen_num_actions, 
			final GENERALIZE_PATH gen_rule,
			final Consistency[] cons,
			final boolean  truncateTrials,
			final boolean enableLabeling ,
			final Random topLevel, 
			final GLOBAL_INITIALIZATION global_init,
			final LOCAL_INITIALIZATION local_init  ) {
		
		super( domain, instance, FAR, debug, order, topLevel.nextLong(), useDiscounting, numStates, numRounds, init_state_conf,
				init_state_prob ,
				null);//, new CrossingTrafficDisplay(10) );
//				null );//domain.contains("sysadmin") ? new SysAdminScreenDisplay() : 
					//domain.contains("crossing_traffic") ? new CrossingTrafficDisplay(50) : null  );

		if( do_apricodd ){
			double cur_error = apricodd_epsilon[0];
			for( int i = 0 ; i < apricodd_epsilon.length; ++i ){
				apricodd_epsilon[i] = cur_error;
				cur_error = cur_error / _mdp.getDiscount();
			}
		}
		
		_actionSelectionRandom = new Random( topLevel.nextLong() );
		_stateSelectionRand = new Random( topLevel.nextLong() );
		
		_baseLinePolicy = HandCodedPolicies.get(domain, _dtr, _manager, _mdp.get_actionVars() );
//		this.exploration = exploration; 
//		
//		this.truncateTrials = truncateTrials;
//		this.enableLabelling = enableLabeling;
		
		_generalizer = new GenericTransitionGeneralization<T, P>( _dtr, cons );

		_genaralizeParameters = new GenericTransitionParameters<T, P, 
				RDDLFactoredStateSpace, RDDLFactoredActionSpace>(_manager, 
						gen_rule, topLevel.nextLong(),
						gen_fix_states, gen_fix_actions, gen_num_actions, gen_num_states, 
						generalizer, generalize_parameters_wo_manager, constrain_naively);
		EPSILON = epsilon;
		this.timeOutMins = timeOutMins;
		this.steps_lookahead = steps_lookahead;

		this.nTrials = nTrials;
		_manager = _mdp.getManager();
		CONSTRAIN_NAIVELY = constrain_naively;
		this.do_apricodd = do_apricodd;
		this.apricodd_epsilon = apricodd_epsilon;
		this.apricodd_type = apricodd_type;
//		this.MB = MB;
//		this.dp_type = dp_type;
		//heuristic too bad anyways
		//final UnorderedPair<ADDRNode, ADDRNode> init 
		//	= _dtr.computeLAOHeuristic( steps_heuristic, heuristic_type, CONSTRAIN_NAIVELY,
		//		false, 0.0d, apricodd_type, MB, time_heuristic_mins );

		_truncateTrials = truncateTrials;
		this._global_init  = global_init;
		this._local_init = local_init;
		if( !( _local_init.equals(LOCAL_INITIALIZATION.VMAX) 
				&& _global_init.equals(GLOBAL_INITIALIZATION.VMAX) ) ){
			_markVisited = true;
		}else if( _truncateTrials ){
			_markVisited = true;
		}
		
		throwAwayEverything();

		//_solved = new ADDRNode[ steps_lookahead ];
		//for( int depth =  0; depth < steps_lookahead-1; ++depth ){
		//    _solved[ depth ] = _manager.DD_ZERO;
		//}
		//_solved[ steps_lookahead-1 ] = _manager.DD_ONE;

		_DPTimer = new Timer();
		_DPTimer.PauseTimer();
		
		_RMAX = _mdp.getRMax();
		
//		max_rewards  = _dtr.maxActionVariables(_mdp.getRewards(), _mdp.getElimOrder(), 
//				null , do_apricodd, 
//				do_apricodd ? apricodd_epsilon[steps_lookahead-1] : 0, 
//				apricodd_type) ;
		
		final P inner_params = _genaralizeParameters.getParameters();
		if( inner_params != null ){
			inner_params.set_manager(_manager);
		}
		
		if( inner_params instanceof EBLParams ){
			((EBLParams)inner_params).set_dtr( _dtr );
		}
//		else if( inner_params instanceof RewardGeneralizationParameters ){
//			final List<ADDRNode> rewards = _mdp.getRewards();
//			((RewardGeneralizationParameters)inner_params).set_rewards( rewards );
//			((RewardGeneralizationParameters)inner_params).set_maxRewards(
//					max_rewards );
//		}
		else if( inner_params instanceof OptimalActionParameters ){
			((OptimalActionParameters)inner_params).set_actionVars( _mdp.get_actionVars() );
		}
		
		//try {
		//	base_line = ADDDecisionTheoreticRegression.getRebootDeadPolicy(_manager, _dtr, _mdp.get_actionVars() );
		//} catch (EvalException e) {
		//	e.printStackTrace();
		//}
//		this.dp_type = dp_type;
		//display(_valueDD, _policyDD);

	}
	
	//always the same without regard to specified generalization type
	protected UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> generalize_leaf(
			final FactoredState<RDDLFactoredStateSpace> state, final int depth ) {
		final NavigableMap<String, Boolean> state_path = state.getFactoredState();
		ADDRNode sum = _manager.restrict( _dtr.getDomainConstraints(), state_path );
		
		//we need an assignment to action vars to determine path
		//first find greedy action
		final List<ADDRNode> rewards_list = _mdp.getRewards();
		for( final ADDRNode rew : rewards_list ){
			final ADDRNode term = _manager.restrict(rew, state_path);
			sum = _manager.apply( sum, term, DDOper.ARITH_PLUS );
		}
		//sum is a leaf node if no action costs
		//otherwise sum has only action vars
		//max actions is leaf node in any case
		final ADDRNode max_actions 
		= _dtr.maxActionVariables( sum , _mdp.getElimOrder(), null, 
				do_apricodd, do_apricodd ? apricodd_epsilon[depth] : 0, apricodd_type);
		if( max_actions.getNode() instanceof ADDINode ){
			try {
				throw new Exception("greedy value not leaf");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		//find greedy action
		final ADDRNode diff = _manager.apply( max_actions, sum, DDOper.ARITH_MINUS );
		final ADDRNode greedy_actions = _manager.threshold( diff, 0.0d, false );
		final ADDRNode greedy_actions_ties
			= _manager.breakTiesInBDD(greedy_actions, _mdp.get_actionVars(), false);
		Set<NavigableMap<String, Boolean>> greedy_action_assigns = _manager.enumeratePathsBDD(greedy_actions_ties);
		if( greedy_action_assigns.size() != 1 ){
			try {
				throw new Exception("no/or not unnique greedy action");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		NavigableMap<String, Boolean> greedy_action_assign = greedy_action_assigns.iterator().next();
//		System.out.println("Greedy action : " + greedy_action_assign );
		
		//now get path
		ADDRNode path_ret = _manager.DD_ZERO;
		for( final ADDRNode rew : rewards_list ){
			final ADDRNode r2 = _manager.restrict(rew, greedy_action_assign);
			path_ret = _manager.BDDUnion(path_ret, _manager.get_path(r2, state_path) );
		}
		
		if( _manager.getVars(max_actions ).contains( _mdp.get_action_vars() ) ){
			try {
				throw new Exception("action var in greedy action");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		return new UnorderedPair<>( path_ret, new UnorderedPair<>(greedy_actions_ties, max_actions.getMax() ) );
		
	}
	
//	protected void initialize_leaf( final  FactoredState<RDDLFactoredStateSpace> state, final int depth ){
//		final ADDRNode flat_state_assign = _manager.getProductBDDFromAssignment(state.getFactoredState() );
//		_valueDD[depth] = _manager.apply( 
//				_manager.BDDIntersection(_valueDD[depth], _manager.BDDNegate(
//						flat_state_assign ) ),
//				_manager.scalarMultiply( flat_state_assign, _mdp.getReward( state.getFactoredState() ) ), 
//				DDOper.ARITH_PLUS );
//	}
//	
//	protected void initialize_leaf( final FactoredState<RDDLFactoredStateSpace> state, final int depth,
//			final ADDRNode gen_leaf ){
//		_valueDD[depth] = _manager.apply( 
//				_manager.BDDIntersection(_valueDD[depth], _manager.BDDNegate(gen_leaf) ),
//				_manager.scalarMultiply( gen_leaf, _mdp.getReward( state.getFactoredState() ) ), 
//				DDOper.ARITH_PLUS );
//	}
	
//	@Override
//	public void initilialize_node(final FactoredState<RDDLFactoredStateSpace> state,
//			final int depth) {
//		
////		if( _manager.evaluate(_visited[depth], state.getFactoredState()).equals(_manager.DD_ONE) ){
////			try{
////				throw new Exception("Initializing already visited node?");
////			}catch( Exception e ){
////				e.printStackTrace();
////				System.exit(1);
////			}
////		}
//		
//		//this value is used for backup
//		//actually no need to initialize here if backup is done backwards
//		
//
////		if( _valueDD[depth] == null ){
////			_valueDD[depth] = _manager.DD_ZERO;
////		}
////		if( depth == steps_lookahead-1 ){
//		    //only need to add last level nodes
//		    //to _valueDD
//		    final double value = get_heuristic_value( state, depth );
//		    _valueDD[ depth ] = set_value( state.getFactoredState(), depth, value );    
////		    System.out.println( state.getFactoredState() + " " + depth + " "  + value );
////		}
//		
//	}
	
//	protected double get_heuristic_val( final NavigableMap<String, Boolean> state_assign, final int depth) {
//	    double value = Double.POSITIVE_INFINITY;
//				
//		
//		if( depth == steps_lookahead-1 ){
//			final double reward = _mdp.getReward( state_assign );
////			System.out.println( "state " +  state_assign.toString() + "reward " + reward + " depth  " + depth );
//			return reward;
//		}
////		else{
//		for( int d = depth+1; d < steps_lookahead; ++d ){//dont include reward level as they have value 0s
//			if( d == steps_lookahead-1 ){
//				final double reward = _mdp.getReward( state_assign );
//				value = _RMAX*(d-depth) + reward;
////				System.out.println( reward + " + " + _RMAX*(d-depth) );
//			}
//			else if( is_node_visited( state_assign, d ) ){
////				++heuristic_sharing;
//				final double that_value = get_value( state_assign, d );
//				if( that_value == _manager.getNegativeInfValue() ){
//				    try{
//					throw new Exception("visited state has value -inf");
//				    }catch( Exception e ){
//					e.printStackTrace();
//					System.exit(1);
//				    }
//				}
//				value = _RMAX*(d-depth) + that_value;
//			}
//		}
////		}
////		if( value == Double.POSITIVE_INFINITY ){
////			value = reward + _RMAX*(steps_lookahead-1-depth);
////		}
//		if( Double.isInfinite(value) ){
//		    try{
//			throw new Exception("heurisitc is -inf");
//		    }catch( Exception e  ){
//			e.printStackTrace();
//			System.exit(1);
//		    }
//		}
//		return value;
//
//	}
//
//	protected double get_heuristic_value(
//			final FactoredState<RDDLFactoredStateSpace> state, final int depth) {
//	    return get_heuristic_val(state.getFactoredState(), depth);
//	}

//	public boolean is_node_visited(final NavigableMap<String, Boolean> state_assign,
//		final int depth ) {
//		final ADDRNode restriction = _manager.restrict( _visited[depth], state_assign );
////		if( restriction.getNode() instanceof ADDINode ){
////			try{
////				throw new Exception("error visited called on partial state");
////			}catch( Exception e ){
////				e.printStackTrace();
////				System.exit(1);
////			}
////		}
//		//what if : we consider partial states as visited if 
//		//there does not exists extension that is not visited
//	    return restriction.equals(_manager.DD_ONE);
//	}
	
//	public boolean is_node_visited(final FactoredState<RDDLFactoredStateSpace> state,
//			final int depth ) {
//		return is_node_visited(state.getFactoredState(), depth );
//	}

	public ADDRNode set_value( final  NavigableMap<String, Boolean> assign, final int depth ,
			final double value ) {
		return _manager.assign( _valueDD[depth], assign, value);
	}
	
	public double get_value( final NavigableMap<String, Boolean> assign, final int depth ) {
		return _manager.restrict(_valueDD[depth], assign).getMax();
	}
	
	public double get_value( final FactoredState<RDDLFactoredStateSpace> state, final int depth ) {
		return _manager.restrict( _valueDD[depth], state.getFactoredState() ).getMax();
	}
	
//	@Override
//	public boolean is_node_solved(
//			final FactoredState<RDDLFactoredStateSpace> assign,
//			final int depth) {
//		return is_node_solved( assign.getFactoredState() , depth );
//	}
	
//	public boolean is_node_solved(
//			final NavigableMap<String, Boolean> assign,
//			final int depth) {
//		return _manager.restrict( _solved[depth], assign ).equals(_manager.DD_ONE);
//	}
	
//	public void mark_node_solved( final FactoredState<RDDLFactoredStateSpace> assign,
//			final int depth ){
//		mark_node_solved(assign.getFactoredState(), depth);
//	}

//	public void mark_node_solved( final NavigableMap<String, Boolean> assign,
//			final int depth ){
//		_solved[depth] = _manager.BDDUnion( _solved[depth], 
//				_manager.getProductBDDFromAssignment(assign) );
//	}
	
	//do local init here
	@Override
	public void initialize_node(FactoredState<RDDLFactoredStateSpace> state, int depth) {
		final double disc = _mdp.getDiscount();
		switch( _local_init ){
		case VMAX : 
			if( !_global_init.equals(GLOBAL_INITIALIZATION.VMAX) ){
				try{
					throw new Exception("local init undoing global.");
				}catch( Exception e ){
					e.printStackTrace();
					System.exit(1);
				}
			}
//			ret = _manager.apply(bdd, _mdp.getVMax(depth, steps_lookahead) , DDOper.ARITH_PLUS );
			break;
		case HRMAX :
			
			final UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> rew_path = generalize_leaf(state, depth);

			double remaining = 0;
			double cur_disc = disc;
			for( int i = depth+1; i < (steps_lookahead); ++i ){
				remaining += cur_disc*_mdp.getRMax();
				cur_disc *= disc;
			}

			final ADDRNode scaled  = _manager.apply( 
					rew_path._o1, 
					_manager.BDDIntersection(rew_path._o1, _manager.getLeaf(remaining) ), 
					DDOper.ARITH_PLUS );
			
			_valueDD[depth] = _manager.apply(
					_manager.BDDIntersection(_valueDD[depth], _manager.BDDNegate(rew_path._o1) ),
					scaled, DDOper.ARITH_PLUS );
			_policyDD[depth] = _manager.BDDUnion(
					_manager.BDDIntersection( _policyDD[depth], _manager.BDDNegate(rew_path._o1) ),
					_manager.BDDIntersection( rew_path._o1, rew_path._o2._o1 ) );
			visit_node(rew_path._o1, depth);
			
			break;
			
		default :
			try{
				throw new UnsupportedOperationException();
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	public boolean is_node_visited( final FactoredState<RDDLFactoredStateSpace> state,
			final int depth) {
		return !_markVisited || _manager.evaluate(_visited[depth], state.getFactoredState() ).equals(_manager.DD_ONE);
	}

	@Override
	public void visit_node(FactoredState<RDDLFactoredStateSpace> state,
			int depth) {
		if( depth == steps_lookahead -1 ){
			return;
		}
		
		if( _markVisited ){
			_visited[depth] = _manager.BDDUnion( _visited[depth], 
					_manager.getProductBDDFromAssignment(state.getFactoredState()) );
			return;
		}
	}
	
	public void visit_node(final ADDRNode states, int depth) {
		if( _markVisited ){
			_visited[depth] = _manager.BDDUnion( _visited[depth], states );
			return;
		}
//		try{
//			throw new UnsupportedOperationException();
//		}catch( Exception e ){
//			e.printStackTrace();
//			System.exit(1);
//		}		
	}
	
	@Override
	public FactoredState<RDDLFactoredStateSpace> pick_successor_node(
			FactoredState<RDDLFactoredStateSpace> state,
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
			int depth) {
		return _transition.sampleFactored(state, action, _stateSelectionRand );
	}
	
	@Override
	public FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> pick_successor_node(
			FactoredState<RDDLFactoredStateSpace> state, int depth) {
//		if( is_node_solved(state, depth) ){
//			return null;
//		}
		return get_policy_action( state, depth );
	}

	private FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> get_policy_action(
			final FactoredState<RDDLFactoredStateSpace> state, 
			final int depth) {
	    //added randomization for action here
		final ADDRNode action_dd = _manager.restrict(
				_policyDD[depth], state.getFactoredState() );
		if( action_dd.equals(_manager.DD_ZERO ) ){
			try{
				throw new Exception("No action defined for state " + state.getFactoredState() );
			}catch(Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		final NavigableMap<String, Boolean> partial_path = 
				Maps.newTreeMap( _manager.sampleOneLeaf( action_dd , _actionSelectionRandom ) );
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
		
		return cur_action.setFactoredAction( partial_path );
	}
	
//	public void visit_node(final ADDRNode path,
//			int depth) {
//		_visited[depth] = _manager.BDDUnion(_visited[depth], path);
//	}
	
//	@Override
//	public void visit_node(FactoredState<RDDLFactoredStateSpace> state,
//			int depth) {
//		
////		if( depth == steps_lookahead-1 ){
////			return;
////		}
////		
////		if( _visited[depth].equals( _manager.DD_ONE ) ){
////			System.out.println("visited is one??");
////		}
//		
//		final ADDRNode this_dd = _manager.getProductBDDFromAssignment( state.getFactoredState() );
//		_visited[depth] = _manager.BDDUnion(_visited[depth], 
//				this_dd );
////		if( _manager.evaluate( _visited[depth], state.getFactoredState() ).equals(_manager.DD_ZERO)){
////			try {
////				throw new Exception("visted not marked as visited");
////			} catch (Exception e) {
////				e.printStackTrace();
////				System.exit(1);
////			}
////		}
////		if( _manager.evaluate(_valueDD[depth], state.getFactoredState() ).equals( _manager.DD_NEG_INF ) ){
////		    try{
////			throw new Exception("visited node not initialized properly");
////		    }catch( Exception e ){
////			e.printStackTrace();System.exit(1);
////		    }
////		}
//		
////		if( _visited[depth].equals( _manager.DD_ONE ) ){
////			System.out.println("visited is one??");
////		}
//	}
	
	protected void throwAwayEverything() {
		_policyDD = null;
		_policyDD = new ADDRNode[ steps_lookahead ];
		Arrays.fill(_policyDD, _baseLinePolicy );
		
		_valueDD = null;
		_valueDD = new ADDRNode[ steps_lookahead ];
		for( int i = 0 ; i < steps_lookahead; ++i ){
			if( i == steps_lookahead-1 && INIT_REWARD ){
				final List<ADDRNode> rews = _mdp.getRewards();
				_valueDD[i] = _manager.DD_ZERO;
				for( final ADDRNode rew : rews ){
					_valueDD[i] = _manager.apply(_valueDD[i], rew, DDOper.ARITH_PLUS);
				}
				_valueDD[i] = _dtr.applyMDPConstraints(_valueDD[i], null, _manager.DD_NEG_INF, CONSTRAIN_NAIVELY, null);
			
				if( do_apricodd ){
					_valueDD[i] = _manager.doApricodd(_valueDD[i], do_apricodd, apricodd_epsilon[i], apricodd_type);
				}
				_valueDD[i] = _dtr.maxActionVariables(_valueDD[i], _mdp.getElimOrder(), null, 
														do_apricodd, do_apricodd ? apricodd_epsilon[i] : 0, apricodd_type);
			}else{
				switch( _global_init ){
				case VMAX : 
					System.out.println("initializing with RMax");
					_valueDD[i] = _mdp.getVMax(i, steps_lookahead);
					break;
				default :
					try{
						throw new UnsupportedOperationException();
					}catch( Exception e ){
						e.printStackTrace();
						System.exit(1);
					}
				}
			}
		}
		
		trajectory_states = null;
		trajectory_states = new FactoredState[ steps_lookahead ];
		trajectory_actions = null;
		trajectory_actions = new FactoredAction[ steps_lookahead - 1 ];
		for( int i = 0 ; i < steps_lookahead-1; ++i ){
		    trajectory_actions[i] = new FactoredAction( );
		    trajectory_states[i] = new FactoredState( );
		}
		trajectory_states[ steps_lookahead - 1 ] = new FactoredState();
		
//		_solved = new ADDRNode[ steps_lookahead ];
//		Arrays.fill( _solved, _manager.DD_ZERO );
//		_solved[ _solved.length-1 ] = _manager.DD_ONE;
		
		_visited = new ADDRNode[ steps_lookahead ];
		Arrays.fill( _visited, _manager.DD_ZERO );
//		_visited[ steps_lookahead-1 ] = _manager.DD_ONE;
		
//		_manager.clearNodes();
		
//		System.gc();
		
	}

	protected ADDRNode getVMax(final int depth ) {
		return _manager.getLeaf( _RMAX * (steps_lookahead-depth) );
	}

	protected void display(  ) {
		for( int i = 0 ; i < _valueDD.length; ++i ){
			ADDRNode vfn = _valueDD[i];
			ADDRNode plcy = _policyDD[i];
			System.out.println("i = " + i );
			System.out.println("#Minterms of Value fn. " + _manager.countPathsADD(vfn) );
			System.out.println("Size of policy " + _manager.countPathsBDD( plcy ) );
//			System.out.println("Size of visited " + _manager.countPathsBDD( _visited[i] ) );
//			System.out.println("Size of ?solved " + _manager.countPathsBDD( _solved[i] ) );
		}
	}
}
