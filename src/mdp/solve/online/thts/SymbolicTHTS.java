package mdp.solve.online.thts;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;

import com.google.common.collect.Maps;

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
import mdp.generalize.trajectory.parameters.RewardGeneralizationParameters;
import mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH;
import mdp.generalize.trajectory.type.GeneralizationType;
import mdp.solve.online.Exploration;
import mdp.solve.online.RDDLOnlineActor;
import mdp.solve.solver.HandCodedPolicies;

public abstract class SymbolicTHTS< T extends GeneralizationType, 
P extends GeneralizationParameters<T> > extends RDDLOnlineActor 
implements THTS< RDDLFactoredStateSpace, RDDLFactoredActionSpace >{

	protected boolean CONSTRAIN_NAIVELY = false;
	protected double	EPSILON;
	protected Timer _DPTimer = null;
	
	protected APPROX_TYPE apricodd_type;
	protected double[] apricodd_epsilon;
	protected boolean do_apricodd;
	
	protected long MB;
	protected BACKUP_TYPE dp_type;
	protected int nTrials;
	private int steps_dp;
	protected int steps_lookahead;
	
	protected ADDRNode[] _valueDD;
	protected ADDRNode[] _policyDD;
	protected ADDRNode[] _visited; 
	protected ADDRNode[] _solved;
//	private ADDRNode _baseLinePolicy;
	
	protected static final FactoredAction[] EMPTY_ACTION_ARRAY = {};
	
	protected GenericTransitionGeneralization<T, P> _generalizer;
	protected GenericTransitionParameters<T, P, RDDLFactoredStateSpace, RDDLFactoredActionSpace> _genaralizeParameters;
	private Exploration< RDDLFactoredStateSpace, RDDLFactoredActionSpace > exploration;  
	protected static FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> cur_action 
		= new FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>( );
	
	protected boolean truncateTrials;
	protected double _RMAX;
	protected boolean enableLabelling;
	private ADDRNode _baseLinePolicy;
	
//	public enum SUCCESSOR{
//		NONE, BRTDP, FRTDP
//	}
	
	public enum FORWARD_PRUNING{
		NONE, PROB
	}
	
	public enum BACKWARD_PRUNING{
		NONE, BE
	}
	
	public SymbolicTHTS( 
			final String domain, 
			final String instance, 
			final double epsilon,
			final DEBUG_LEVEL debug, 
			final ORDER order, 
			final long seed,
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
			final int steps_dp,
			final int steps_lookahead ,
			final Generalization< RDDLFactoredStateSpace, RDDLFactoredActionSpace, T, P > generalizer, 
			final P generalize_parameters_wo_manager ,
			final boolean gen_fix_states,
			final boolean gen_fix_actions,
			final int gen_num_states,
			final int gen_num_actions, 
			final GENERALIZE_PATH gen_rule,
			final Exploration<RDDLFactoredStateSpace, RDDLFactoredActionSpace> exploration,
			final Consistency[] cons,
			final boolean  truncateTrials,
			final boolean enableLabeling  ) {
		
		
		super( domain, instance, FAR, debug, order, seed, useDiscounting, numStates, numRounds, init_state_conf,
				init_state_prob, 
				null );//domain.contains("sysadmin") ? new SysAdminScreenDisplay() : 
					//domain.contains("crossing_traffic") ? new CrossingTrafficDisplay(50) : null  );

		_baseLinePolicy = HandCodedPolicies.get(domain, _dtr, _manager, _mdp.get_actionVars() );

		this.exploration = exploration; 
		
		this.truncateTrials = truncateTrials;
		this.enableLabelling = enableLabeling;
		
		_generalizer = new GenericTransitionGeneralization<T, P>( _dtr, cons );

		_genaralizeParameters = new GenericTransitionParameters<T, P, 
				RDDLFactoredStateSpace, RDDLFactoredActionSpace>(_manager, 
						gen_rule, new Random( _rand.nextLong() ),
						gen_fix_states, gen_fix_actions, gen_num_actions, gen_num_states, 
						generalizer, generalize_parameters_wo_manager, constrain_naively);
		EPSILON = epsilon;
		this.steps_dp = steps_dp;
		this.steps_lookahead = steps_lookahead;

		this.nTrials = nTrials;
		_manager = _mdp.getManager();
		CONSTRAIN_NAIVELY = constrain_naively;
		this.do_apricodd = do_apricodd;
		this.apricodd_epsilon = apricodd_epsilon;
		this.apricodd_type = apricodd_type;
		this.MB = MB;
		this.dp_type = dp_type;
		//heuristic too bad anyways
		//final UnorderedPair<ADDRNode, ADDRNode> init 
		//	= _dtr.computeLAOHeuristic( steps_heuristic, heuristic_type, CONSTRAIN_NAIVELY,
		//		false, 0.0d, apricodd_type, MB, time_heuristic_mins );

		
		throwAwayEverything();

		//_solved = new ADDRNode[ steps_lookahead ];
		//for( int depth =  0; depth < steps_lookahead-1; ++depth ){
		//    _solved[ depth ] = _manager.DD_ZERO;
		//}
		//_solved[ steps_lookahead-1 ] = _manager.DD_ONE;

		_DPTimer = new Timer();
		_DPTimer.PauseTimer();
		
		final P inner_params = _genaralizeParameters.getParameters();
		inner_params.set_manager(_manager);
		
		if( inner_params instanceof EBLParams ){
			((EBLParams)inner_params).set_dtr( _dtr );
		}else if( inner_params instanceof RewardGeneralizationParameters ){
			final List<ADDRNode> rewards = _mdp.getRewards();
			((RewardGeneralizationParameters)inner_params).set_rewards( rewards );
			((RewardGeneralizationParameters)inner_params).set_max_rewards(
					_dtr.maxActionVariables(rewards, _mdp.getElimOrder(), 
							null , do_apricodd, 
							do_apricodd ? apricodd_epsilon[steps_lookahead-1] : 0, 
							apricodd_type) );
		}
		//try {
		//	base_line = ADDDecisionTheoreticRegression.getRebootDeadPolicy(_manager, _dtr, _mdp.get_actionVars() );
		//} catch (EvalException e) {
		//	e.printStackTrace();
		//}
		//display(_valueDD, _policyDD);

	}
	
	@Override
	public void initilialize_node(final FactoredState<RDDLFactoredStateSpace> state,
			final int depth) {
		
		if( _manager.evaluate(_visited[depth], state.getFactoredState()).equals(_manager.DD_ONE) ){
			try{
				throw new Exception("Initializing already visited node?");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		//this value is used for backup
		//actually no need to initialize here if backup is done backwards
		

//		if( _valueDD[depth] == null ){
//			_valueDD[depth] = _manager.DD_ZERO;
//		}
		if( depth == steps_lookahead-1 ){
		    //only need to add last level nodes
		    //to _valueDD
		    final double value = get_heuristic_value( state, depth );
		    _valueDD[ depth ] = set_value( state.getFactoredState(), depth, value );    
		}
		
	}
	
	protected double get_heuristic_val( final NavigableMap<String, Boolean> state_assign, final int depth) {
	    double value = Double.POSITIVE_INFINITY;
				
		final double reward = _mdp.getReward( state_assign );
		
		if( depth == steps_lookahead-1 ){
			value = reward;
		}else{
			for( int d = depth+1; d < steps_lookahead; ++d ){
				if( is_node_visited( state_assign, d ) ){
					value = _RMAX*(d-depth) + get_value( state_assign, d );
				}
			}
		}
		if( value == Double.POSITIVE_INFINITY ){
			value = _RMAX*(steps_lookahead-depth);
		}
		
		return value;

	}

	protected double get_heuristic_value(
			final FactoredState<RDDLFactoredStateSpace> state, final int depth) {
	    return get_heuristic_val(state.getFactoredState(), depth);
	}

	public boolean is_node_visited(final NavigableMap<String, Boolean> state_assign,
		final int depth ) {
	    return _manager.restrict( _visited[depth], state_assign ).equals(_manager.DD_ONE);
	}
	
	public boolean is_node_visited(final FactoredState<RDDLFactoredStateSpace> state,
			final int depth ) {
		return is_node_visited(state.getFactoredState(), depth );
	}

	public ADDRNode set_value( final  NavigableMap<String, Boolean> assign, final int depth ,
			final double value ) {
		return _manager.assign( _valueDD[depth], assign, value);
	}
	
	public double get_value( final NavigableMap<String, Boolean> assign, final int depth ) {
		return _manager.restrict(_valueDD[depth], assign).getMax();
	}
	
	@Override
	public boolean is_node_solved(
			final FactoredState<RDDLFactoredStateSpace> assign,
			final int depth) {
		return is_node_solved( assign.getFactoredState() , depth );
	}
	
	public boolean is_node_solved(
			final NavigableMap<String, Boolean> assign,
			final int depth) {
		return _manager.restrict( _solved[depth], assign ).equals(_manager.DD_ONE);
	}
	
	public void mark_node_solved( final FactoredState<RDDLFactoredStateSpace> assign,
			final int depth ){
		mark_node_solved(assign.getFactoredState(), depth);
	}

	public void mark_node_solved( final NavigableMap<String, Boolean> assign,
			final int depth ){
		_solved[depth] = _manager.BDDUnion( _solved[depth], 
				_manager.getProductBDDFromAssignment(assign) );
	}

	@Override
	public FactoredState<RDDLFactoredStateSpace> pick_successor_node(
			FactoredState<RDDLFactoredStateSpace> state,
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
			int depth) {
		return _transition.sampleFactored(state, action);
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
				Maps.newTreeMap( _manager.sampleOneLeaf( action_dd , _rand ) );
//		final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> partial_action 
//		= cur_action.setFactoredAction( path );
		for( final String actvar : _mdp.get_actionVars() ){
			if( !partial_path.containsKey(actvar) ){
				partial_path.put( actvar, false );
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
	
	@Override
	public void visit_node(FactoredState<RDDLFactoredStateSpace> state,
			int depth) {
		
		if( depth == steps_lookahead-1 ){
			return;
		}
		
		_visited[depth] = _manager.BDDUnion(_visited[depth], 
				_manager.getProductBDDFromAssignment( state.getFactoredState() ) );
		if( _manager.restrict( _visited[depth], state.getFactoredState() ).equals(_manager.DD_ZERO)){
			try {
				throw new Exception("visted not marked as visited");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	protected void throwAwayEverything() {
		_policyDD = new ADDRNode[ steps_lookahead ];
		Arrays.fill(_policyDD, _baseLinePolicy );
		
		_valueDD = new ADDRNode[ steps_lookahead ];
		Arrays.fill( _valueDD, _manager.DD_ZERO );
		
		_solved = new ADDRNode[ steps_lookahead ];
		Arrays.fill( _solved, _manager.DD_ZERO );
		_solved[ _solved.length-1 ] = _manager.DD_ONE;
		
		_visited = new ADDRNode[ steps_lookahead ];
		Arrays.fill( _visited, _manager.DD_ZERO );
		
		
//		_manager.clearNodes();
		
		System.gc();
		
	}

	protected void display(  ) {
		for( int i = 0 ; i < _valueDD.length; ++i ){
			ADDRNode vfn = _valueDD[i];
			ADDRNode plcy = _policyDD[i];
			System.out.println("i = " + i );
			System.out.println("Size of Value fn. " + _manager.countNodes(vfn) );
			System.out.println("Size of policy " + _manager.countNodes( plcy ) );
			System.out.println("Size of visited " + _manager.countNodes( _visited[i] ) );
			System.out.println("Size of solved " + _manager.countNodes( _solved[i] ) );
		}
		System.out.println("DP time: " + _DPTimer.GetElapsedTimeInMinutes() );
	}
}
