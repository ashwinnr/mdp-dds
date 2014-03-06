package dtr.add;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rddl.EvalException;
import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
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
import dd.DDManager.DDQuantify;
import dtr.SymbolicPolicy;
import dtr.SymbolicRegression;
import dtr.SymbolicValueFunction;
import factored.mdp.define.FactoredAction;

public class ADDDecisionTheoreticRegression implements
		SymbolicRegression<ADDNode, ADDRNode, ADDINode, ADDLeaf, RDDLFactoredStateSpace, 
		RDDLFactoredActionSpace> {

	public enum BACKUP_TYPE{
		VMAX, VI_SPUDD, VI_FAR, VI_MBFAR
	}
	
	private ADDManager _manager;
	private RDDL2ADD _mdp;
	private DEBUG_LEVEL _dbg;
//	private Set<ADDRNode> _constraints;
	private Random _rand;
	private List<String> __order_action_vars_appear;
	private ArrayList< Integer > __nextVar_to_actionVar_range;
	
	private ArrayList< ADDRNode >  __action_precondition = new ArrayList<ADDRNode>();
	private final ArrayList<ADDRNode> __policy_constraints = new ArrayList<ADDRNode>();
	private final ArrayList<ADDRNode> __action_constraints = new ArrayList<ADDRNode>();
	private final ArrayList<ADDRNode> __state_constraints = new ArrayList<ADDRNode>();
	private ArrayList< ADDRNode >  __action_precondition_neginf = new ArrayList<ADDRNode>();
	private final ArrayList<ADDRNode> __policy_constraints_neginf = new ArrayList<ADDRNode>();
	private final ArrayList<ADDRNode> __action_constraints_neginf = new ArrayList<ADDRNode>();
	private final ArrayList<ADDRNode> __state_constraints_neginf = new ArrayList<ADDRNode>();
	
	private ADDRNode _hsReward = null;
	
	public enum INITIAL_STATE_CONF{
		UNIFORM, BERNOULLI, CONJUNCTIVE
	}
	
	public enum GENERALIZE_PATH{
		ALL_PATHS, NONE
	}
	
	private class Heuristic_Compute implements Callable< UnorderedPair<ADDRNode, ADDRNode> >{
		
		 private final int steps;
		 private final BACKUP_TYPE heuristic_type;
		 private final boolean constrain_naively;
		 private final boolean do_apricodd;
		 private final double apricodd_epsilon;
		 private final APPROX_TYPE apricodd_type;
		 private final long MB;
		 private UnorderedPair<ADDRNode, ADDRNode> ret = new UnorderedPair<ADDRNode, ADDRNode>();
		private boolean timeOut;
		 
		public Heuristic_Compute(int steps, BACKUP_TYPE heuristic_type,
				boolean constrain_naively, boolean do_apricodd,
				double apricodd_epsilon, APPROX_TYPE apricodd_type, long mB) {
			super();
			this.steps = steps;
			this.heuristic_type = heuristic_type;
			this.constrain_naively = constrain_naively;
			this.do_apricodd = do_apricodd;
			this.apricodd_epsilon = apricodd_epsilon;
			this.apricodd_type = apricodd_type;
			MB = mB;
		}
		
		public UnorderedPair<ADDRNode, ADDRNode> getLastResult( ){
			return ret;
		}
		
		public void setTimeOut( ) {
			this.timeOut = true;
		}

		@Override
		public UnorderedPair<ADDRNode, ADDRNode> call() throws Exception {
			final ADDRNode Vmax = _mdp.getVMax();
			if( heuristic_type.equals(BACKUP_TYPE.VMAX) ){
				ret._o1 = Vmax;
				ret._o2 = null;
				return ret;
			}
			
			int iter = 0;
			ret._o1 = Vmax;
			while( ( ( steps != -1 && iter++ < steps ) || steps == -1 )
					&& !timeOut ){// NOTE : steps removed 1/20 for steps = -1 -ANR
				System.out.println("Heur step : " + iter );
				switch( heuristic_type ){
				case VI_SPUDD:
					final UnorderedPair<SymbolicValueFunction<ADDNode, ADDRNode, 
					ADDINode, ADDLeaf, RDDLFactoredStateSpace, RDDLFactoredActionSpace>, 
					SymbolicPolicy<ADDNode, ADDRNode, ADDINode, ADDLeaf, 
					RDDLFactoredStateSpace, RDDLFactoredActionSpace>> 
					regressed_spudd = regress(ret._o1, false, false, true, constrain_naively, null, do_apricodd,
							apricodd_epsilon, apricodd_type );
					ret._o1 = regressed_spudd._o1.get_valueFn();
					ret._o2 = regressed_spudd._o2._addPolicy;
					break;
				case VI_FAR : 
					final UnorderedPair<SymbolicValueFunction<ADDNode, ADDRNode, 
					ADDINode, ADDLeaf, RDDLFactoredStateSpace, RDDLFactoredActionSpace>, 
					SymbolicPolicy<ADDNode, ADDRNode, ADDINode, ADDLeaf, 
					RDDLFactoredStateSpace, RDDLFactoredActionSpace>> 
					regressed_far = regress(ret._o1, true , false, true, constrain_naively, null, do_apricodd,
							apricodd_epsilon, apricodd_type );
					ret._o1 = regressed_far._o1.get_valueFn();
					ret._o2 = regressed_far._o2._bddPolicy;
					break;
				case VI_MBFAR :
					if( MB <= 0 ){
						try{
							throw new Exception("Heuristic type VI_MBFAR does not make sense with MB 0" );
						}catch( Exception e ){
							e.printStackTrace();
							System.exit(1);
						}
					}
					final UnorderedPair<ADDValueFunction, ADDPolicy> 
					regressed_mbfar = regressMBFAR( ret._o1, true, constrain_naively, MB, null, do_apricodd, apricodd_epsilon, apricodd_type);
					ret._o1 = regressed_mbfar._o1.get_valueFn();
					ret._o2 = regressed_mbfar._o2._bddPolicy;
					break;
				}
			}
			return ret;
		}
	}
	
	public ADDRNode generalize( final ADDRNode input, 
			final NavigableMap<String, Boolean> path ,
			final GENERALIZE_PATH rule ){
		switch( rule ){
		case ALL_PATHS :
			ADDRNode eval = _manager.evaluate(input, path);
			ADDLeaf leaf = (ADDLeaf)eval.getNode();
			return _manager.all_paths_to_leaf(input, leaf); 
		case NONE :
			return _manager.getProductBDDFromAssignment( path );
		}
		return null;
	}
	
	public ADDRNode generalize( final ADDRNode input, 
			final ADDRNode paths,//0 -infty BDD
			final GENERALIZE_PATH rule ){
		switch( rule ){
		case ALL_PATHS :
			ADDRNode ret = _manager.DD_ZERO;
			ADDRNode good_paths = _manager.apply( input, paths, DDOper.ARITH_PLUS );
			Set<NavigableMap<String, Boolean>> non_neginf_paths  
				= _manager.enumeratePaths( good_paths, false, true, _manager.DD_NEG_INF, true );
			for( final NavigableMap<String, Boolean> one_path : non_neginf_paths ){
				ret = _manager.BDDUnion(ret, generalize( input, one_path, rule ) );
			}
			return ret;
			
		case NONE :
			ret = _manager.DD_ZERO;
			good_paths = _manager.apply( input, paths, DDOper.ARITH_PLUS );
			non_neginf_paths  
				= _manager.enumeratePaths( good_paths, false, true, _manager.DD_NEG_INF, true );
			for( final NavigableMap<String, Boolean> one_path : non_neginf_paths ){
				ret = _manager.BDDUnion(ret, _manager.getProductBDDFromAssignment(one_path) );
			}
			return ret;
		}
		return null;
	}
	
	public static void testComputeHeuristic(){
		
		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp.rddl", "./rddl/sysadmin_star_2_2.rddl", 
				true, DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, true, 42);
		
		ADDDecisionTheoreticRegression ADD_dtr 
			= new ADDDecisionTheoreticRegression(mdp, 42);
		ADDManager manager = mdp.getManager();
		
		final UnorderedPair<ADDRNode, ADDRNode> result
			= ADD_dtr.computeLAOHeuristic(100, 
					BACKUP_TYPE.VI_FAR, true, false, 0.1, APPROX_TYPE.UPPER, 0, 0.01 );
		System.out.println("received result");
		manager.showGraph( result._o1, result._o2 );
	}
	
	//from and to needs to be a BDD
	public UnorderedPair< ADDRNode, UnorderedPair< ADDRNode , Double > > backup( final ADDRNode current_value, 
			final ADDRNode cur_policy,//BDD
			final ADDRNode from, 
			final ADDRNode to, 
			final BACKUP_TYPE backup_type, 
			final boolean do_apricodd,
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ,
			final boolean makePolicy,
			final long BIGDD ){
		
		final ADDRNode unprimed = _manager.BDDIntersection(current_value, from);
		final ADDRNode primed = 
				_manager.remapVars( 
						unprimed  ,
						_mdp.getPrimeRemap() );
		int idx = addStateConstraint(to);
		UnorderedPair<ADDValueFunction, ADDPolicy> backup = null;
		switch( backup_type ){
		case VI_FAR : 
			backup = regressAllActions( primed, false, makePolicy, true, null,
					do_apricodd, apricodd_epsilon, apricodd_type );
			break;//backup will have value and policy for things not in to, 
			//should be masked
		case VI_SPUDD :
			backup = regressSPUDD(primed, false, makePolicy, true, null, 
					do_apricodd, apricodd_epsilon, apricodd_type);
			break;
		case VI_MBFAR :
			backup = regressMBFAR(unprimed, makePolicy, true, BIGDD, null,
					do_apricodd, apricodd_epsilon, apricodd_type);
			break;
		}
		
		ADDRNode value_ret = _manager.BDDIntersection( backup._o1.getValueFn(), to );
		ADDRNode saveV = _manager.BDDIntersection( current_value ,
				_manager.BDDNegate(to) );
		value_ret = _manager.apply( value_ret, saveV, DDOper.ARITH_PLUS );
		final double residual = getBellmanError(value_ret, current_value);
		
		ADDRNode policy_ret = null;
		if( makePolicy ){
			policy_ret = backup._o2._addPolicy == null ? backup._o2._bddPolicy :
					backup._o2._addPolicy ;
			policy_ret = _manager.BDDIntersection(policy_ret, to);//policy in 
		//to states
		//zero in others
		//put input policy in others
			policy_ret = _manager.BDDUnion( policy_ret, 
					_manager.BDDIntersection( cur_policy, _manager.BDDNegate(to) ) );
		}
		removeStateConstraint(idx);
		
		return new UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double> >( 
				value_ret, new UnorderedPair<ADDRNode, Double>( policy_ret , residual ) );
	}
	
	public enum INITIAL_VALUE{
		ZERO, VMAX
	}
	
	public UnorderedPair<ADDRNode,ADDRNode> computeLAOHeuristic( final int steps, 
			final BACKUP_TYPE heuristic_type ,
			final boolean constrain_naively, 
			final boolean do_apricodd, 
			final double apricodd_epsilon, 
			final APPROX_TYPE apricodd_type,
			final long MB ,
			final double time_heuristic_mins ){
		
		
		final Heuristic_Compute worker = new Heuristic_Compute(steps, heuristic_type, 
				constrain_naively, do_apricodd, 
				apricodd_epsilon, apricodd_type, MB);
		ExecutorService exec_serv = Executors.newSingleThreadExecutor();
		Future<UnorderedPair<ADDRNode, ADDRNode>> future = exec_serv.submit( 
				worker );
		UnorderedPair<ADDRNode, ADDRNode> ret = null;
		try {
			final UnorderedPair<ADDRNode, ADDRNode> result = future.get((long)( time_heuristic_mins * 60 * 1000), TimeUnit.MILLISECONDS );
			System.out.println("computation completed, returning result");
			ret = result;
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ExecutionException e) {
			e.printStackTrace();
			System.exit(1);
		} catch( TimeoutException e ){
			System.out.println("time out for heuristic compute, returning current result ");
			worker.setTimeOut();
			ret = worker.getLastResult();
			System.out.print("task has been cancelled ");
			System.out.println(future.cancel( true ));
		}
		System.out.println( exec_serv.shutdownNow() );
		return ret;
	}

	public ADDDecisionTheoreticRegression(RDDL2ADD mdp, final long seed){
		_manager = mdp.getManager();
		_mdp = mdp;
		_dbg = mdp.__debug_level;
		final ADDRNode act_constr = _manager.productDD( _mdp.getActionConstraints() );
		__action_constraints.add( act_constr );
		__action_constraints_neginf.add( convertToNegInfDD(act_constr)[0] );
		
		final ADDRNode state_constr = _manager.productDD( mdp.getStateConstraints() );
		__state_constraints.add( state_constr );
		__state_constraints_neginf.add( convertToNegInfDD( state_constr )[0] );
		
		final ADDRNode action_precon = _manager.productDD(_mdp.getActionPreconditions() );
		if( action_precon != null ){
			__action_precondition.add( action_precon );
			__action_precondition_neginf.add( convertToNegInfDD( action_precon )[0] );
		}
		
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
		ADDRNode all_constraints = _manager.sumDD( 
				__action_precondition_neginf, __state_constraints_neginf,
				 __policy_constraints_neginf, 
				__action_constraints_neginf );
		
		if( action != null ){
			all_constraints = _manager.restrict(all_constraints, action);
		}
//		all_constraints = convertToNegInfDD( Lists.newArrayList( all_constraints ) ).get(0);
		ADDRNode ret = _manager.apply(input, all_constraints, DDOper.ARITH_PLUS );
		if( ! violate.equals(_manager.DD_NEG_INF) ){
			ret = _manager.remapLeaf(ret, _manager.DD_NEG_INF, violate);
		}
		
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
				__policy_constraints, 
				__action_constraints );
		
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
			if( size_change != null ){
				sum_q += _manager.countNodes( this_q ).get( 0 );
			}
			if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.println("Regressing action : " + action );
			}
			
			//not noting down the size of V
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
//			_manager.flushCaches( );
		}
		
		if( size_change != null ){
			size_change.clear();
			size_change.add( sum_q / actions.size() );
		}
		
//		sum_q *= actions.size();
				
		return new UnorderedPair<ADDValueFunction, ADDPolicy>( 
				new ADDValueFunction( v_func, q_map, null, _manager )
				, policy);
	}
	
	//function to compute next state distribution over states
	//with/without given probabilistic policy
	//ie. with action variables
	//P(s') = sum_s P(s) sum_a P(a|s) P(s' | s,a)
	//P(a|s) is an ADD policy
	// no form of P(a|s) is assumed
	//when no policy is specified, assume uniform?
	//use discounting?
	public ADDRNode exactProbabilisticImage( final ADDRNode state_distribution, 
			final ADDRNode probabilistic_policy ){
		return null;
	}
	
	public ADDRNode BDDImagePolicy( final ADDRNode reachable_states,
			final boolean withActionVars,
			final DDQuantify action_qualification, 
			final ADDRNode policy ){
		int index_added = addPolicyConstraint(policy);
		final ADDRNode ret = BDDImage(reachable_states, withActionVars, action_qualification);
		removePolicyConstraint(index_added);
		return ret;
	}
	
	//all actions?
	//spudd style?
	//far style?
	//sLAO* style  BDD image
	//given : BDD over states
	//use transition ``relation'' 
	//existentially quantify actions
	//or policy is specified
	public  ADDRNode BDDImage( final ADDRNode reachable_states, 
			final boolean withActionVars, 
			final DDQuantify action_quantification ){
		if( !_mdp.isTransitionRelationReady(withActionVars) ){
			_mdp.makeTransitionRelation( withActionVars );
		}
		
		ADDRNode ret_ns = null; 
		if( withActionVars ){
			ret_ns =  BDDImageFAR( reachable_states, action_quantification );
		}else {
			ret_ns =  BDDImageSPUDD( reachable_states, action_quantification );
		}
		return _manager.remapVars(ret_ns, _mdp.getPrimeUnMap() );
	}
	
	private ADDRNode BDDImageSPUDD(final ADDRNode reachable_states,
			final DDQuantify quantification) {
		final List<NavigableMap<String, Boolean>> actions = _mdp.getFullRegressionOrder();
		final Map<Map<String, Boolean>, Map<String, ADDRNode>> theRelations = _mdp.getTransitionRelationSPUDD();
		final Map<NavigableMap<String, Boolean>, ArrayList<Pair<String, String>>> 
			theRelationsLast = _mdp.getTransitionRelationLastSPUDD();
		ADDRNode ret = _manager.DD_ZERO;
		for( final NavigableMap<String, Boolean> action : actions ){
			ADDRNode ret_action = applyMDPConstraintsNaively(reachable_states, action, _manager.DD_ZERO, null);
			final Map<String, ADDRNode> action_relation = theRelations.get( action );
			final ArrayList<Pair<String, String>> action_last = theRelationsLast.get( action );
			int lastSeen_index = 0;
			final int lastSeen_size = action_last.size();
			for( final String ns_var : _mdp.getSumOrder() ){
				ret_action = _manager.apply( ret_action, 
						action_relation.get( ns_var ),
						DDOper.ARITH_PROD );
				
				if( lastSeen_index == lastSeen_size ){
					continue;
				}else{//check for state vars to quantify
					Pair<String, String> first_lastSeen = action_last.get( lastSeen_index );
					while( ( first_lastSeen._o1 == null || first_lastSeen._o1 == ns_var ) && 
							lastSeen_index < lastSeen_size ){ //counts on interned string
						ret_action = _manager.quantify( ret_action, first_lastSeen._o2, quantification );
						if( ++lastSeen_index < lastSeen_size ){
							first_lastSeen = action_last.get( lastSeen_index );
						}
					}
				}
			}
			ret = _manager.apply( ret, ret_action, DDOper.ARITH_MAX );
		}
		return ret;
	}

	private static void testImageAllActions(){
//		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp.rddl", "./rddl/sysadmin_star_2_2.rddl", 
//				true, DEBUG_LEVEL.SOLUTION_INFO, ORDER.GUESS, true, 42);
//		
//		ADDDecisionTheoreticRegression ADD_dtr 
//			= new ADDDecisionTheoreticRegression(mdp, 42);
//		ADDManager manager = mdp.getManager();
//		
//		int i = 0;
//		Set<String> state_vars = mdp.getFactoredStateSpace().getStateVariables();
//		ADDRNode states = manager.DD_ONE;
//		for( final String state_var : state_vars ){
//			states = manager.apply( states , 
//					manager.getIndicatorDiagram(state_var, false),
//					DDOper.ARITH_PROD ) ;
//		}
//		
//		ADDRNode iter = states;
//		while( i++ < 5 ){
//			manager.showGraph( iter );
//			System.out.println( i +  " " + manager.countNodes(iter) );
//			iter = ADD_dtr.BDDImage(iter, true,  DDQuantify.EXISTENTIAL);
//		}

		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp.rddl", "./rddl/sysadmin_star_2_2.rddl", 
				false, DEBUG_LEVEL.SOLUTION_INFO, ORDER.GUESS, true, 42);
		
		ADDDecisionTheoreticRegression ADD_dtr 
			= new ADDDecisionTheoreticRegression(mdp, 42);
		ADDManager manager = mdp.getManager();
		
		int i = 0;
		Set<String> state_vars = mdp.getFactoredStateSpace().getStateVariables();
		ADDRNode states = manager.DD_ONE;
		for( final String state_var : state_vars ){
			states = manager.apply( states , 
					manager.getIndicatorDiagram(state_var, false),
					DDOper.ARITH_PROD ) ;
		}
		
		ADDRNode iter = states;
		while( i++ < 5 ){
			manager.showGraph( iter );
			System.out.println( i +  " " + manager.countNodes(iter) );
			iter = ADD_dtr.BDDImage(iter, false,  DDQuantify.EXISTENTIAL);
		}
	}

	private static void testImagePolicy(){
//		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp.rddl", "./rddl/sysadmin_star_2_2.rddl", 
//				true, DEBUG_LEVEL.SOLUTION_INFO, ORDER.GUESS, true, 42);
//		
//		ADDDecisionTheoreticRegression ADD_dtr 
//			= new ADDDecisionTheoreticRegression(mdp, 42);
//		ADDManager manager = mdp.getManager();
//		
//		int i = 0;
//		Set<String> state_vars = mdp.getFactoredStateSpace().getStateVariables();
//		ADDRNode states = manager.DD_ONE;
//		for( final String state_var : state_vars ){
//			states = manager.apply( states , 
//					manager.getIndicatorDiagram(state_var, false),
//					DDOper.ARITH_PROD ) ;
//		}
//		
//		ADDRNode policy = manager.DD_ZERO;
//		for( final String action_var : mdp.getFactoredActionSpace().getActionVariables() ){
//			policy = manager.apply( policy, 
//					manager.getIndicatorDiagram(action_var, true),
//					DDOper.ARITH_MAX );
//		}
//		
//		ADDRNode iter = states;
//		while( i++ < 5 ){
//			manager.showGraph( iter );
//			System.out.println( i +  " " + manager.countNodes(iter) );
//			iter = ADD_dtr.BDDImagePolicy(iter, true,  DDQuantify.EXISTENTIAL, policy);
//		}

		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp.rddl", "./rddl/sysadmin_star_2_2.rddl", 
				false, DEBUG_LEVEL.SOLUTION_INFO, ORDER.GUESS, true, 42);
		
		ADDDecisionTheoreticRegression ADD_dtr 
			= new ADDDecisionTheoreticRegression(mdp, 42);
		ADDManager manager = mdp.getManager();
		
		int i = 0;
		Set<String> state_vars = mdp.getFactoredStateSpace().getStateVariables();
		ADDRNode states = manager.DD_ONE;
		for( final String state_var : state_vars ){
			states = manager.apply( states , 
					manager.getIndicatorDiagram(state_var, false),
					DDOper.ARITH_PROD ) ;
		}
		
		ADDRNode policy = manager.DD_ZERO;
		for( final String action_var : mdp.getFactoredActionSpace().getActionVariables() ){
			policy = manager.apply( policy, 
					manager.getIndicatorDiagram(action_var, true),
					DDOper.ARITH_MAX );
		}
		
		ADDRNode iter = states;
		while( i++ < 5 ){
			manager.showGraph( iter );
			System.out.println( i +  " " + manager.countNodes(iter) );
			iter = ADD_dtr.BDDImagePolicy(iter, false,  DDQuantify.EXISTENTIAL, policy);
		}
		
	}

	//get transition relation
	//convert CPTs to BDDs
	private ADDRNode BDDImageFAR(final ADDRNode primed_reachable_states,
			final DDQuantify quantification) {
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
			&& _manager.hasVars( primed_reachable_states, _mdp.getFactoredActionSpace().getActionVariables() )
			&& _manager.hasVars( primed_reachable_states, _mdp.getFactoredStateSpace().getNextStateVars() ) ){
			try{
				throw new Exception("Has action/next state vars in Image.");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		ADDRNode ret = primed_reachable_states;
		final NavigableMap<String, ADDRNode> transitionRelation = _mdp.getTransitionRelationFAR();
		final ArrayList<Pair<String, String>> lastSeen = _mdp.getTransitionRelationLastFAR();
		final ArrayList<String> sumOrder = _mdp.getSumOrder();
		int lastSeen_index = 0;
		final int lastSeen_size = lastSeen.size();
		ret = applyMDPConstraintsNaively( ret, null, _manager.DD_ZERO, null );

		for( final String nextState : sumOrder ){
//			System.out.print("Image for variable ");
//			System.out.print( nextState );
//			System.out.println();
			final ADDRNode theRelation = transitionRelation.get(nextState);
			ret = _manager.apply( ret , theRelation, DDOper.ARITH_PROD );
			
			Pair<String, String> first_lastSeen = lastSeen.get( lastSeen_index );
			while( ( first_lastSeen._o1 == null || first_lastSeen._o1 == nextState ) && 
					lastSeen_index < lastSeen_size ){ //counts on interned string
				ret = _manager.quantify( ret, first_lastSeen._o2, quantification );
				if( ++lastSeen_index < lastSeen_size ){
					first_lastSeen = lastSeen.get( lastSeen_index );
				}
			}
		}

		for( String state_var : _mdp.getFactoredStateSpace().getStateVariables() ){
			ret = _manager.quantify( ret ,  state_var , quantification);
		}
		 
		for( String action_var : _mdp.getFactoredActionSpace().getActionVariables() ){
			ret = _manager.quantify(ret, action_var, quantification);
		}
		 
		return ret;
	}

	//	preImage using BDDs
	//preImage with probabilities
	
	private UnorderedPair<ADDValueFunction, ADDPolicy> regressAllActions(final ADDRNode primed, 
			final boolean keepQ, final boolean makePolicy, 
			final boolean constrain_naively, final List<Long> size_change ,
			final boolean do_apricodd, 
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ) {
		
		final ArrayList<String> sum_order = _mdp.getSumOrder();
		if( _dbg.compareTo( DEBUG_LEVEL.SOLUTION_INFO ) >= 0 ){
			System.out.print("Regressing all actions " );
		}
		ADDRNode ret = constrain_naively ? 
				applyMDPConstraints(primed, null, _manager.DD_NEG_INF, constrain_naively, size_change)
				: primed ;
		for( String str : sum_order ){
			if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.print( "*" );// + str );
			}
			ret = computeExpectation( ret, str, null, constrain_naively, size_change,
					do_apricodd, 
					apricodd_epsilon, apricodd_type );
		}
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
			System.out.println();	
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
		ADDRNode q_func = _manager.doApricodd(improper_q_func, do_apricodd, apricodd_epsilon, apricodd_type);
		
		if( !constrain_naively ){
			q_func = multiplyActionPreconditions( q_func, null );
			q_func = multiplyStateConstraints( q_func );
			if( makePolicy ){
				q_func = multiplyActionConstraints( q_func, null );
			}
		}
		
		if( _dbg.compareTo( DEBUG_LEVEL.SOLUTION_INFO ) >= 0 ){
			System.out.println("Size of |Q| = " + _manager.countNodes(q_func) );
		}
		
		
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

	public ADDValueFunction regressHindSight(final ADDRNode input, 
			final boolean constrain_naively, final List<Long> size_change ,
			final boolean do_apricodd, 
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ) {

		final ADDRNode primed = _manager.remapVars(input, _mdp.getPrimeRemap() );
		
		final ArrayList<String> sum_order = _mdp.getSumOrder();
		final Map<String, ArrayList<String>> hsOrder = _mdp.getHindSightOrder();
		
		if( _dbg.compareTo( DEBUG_LEVEL.SOLUTION_INFO ) >= 0 ){
			System.out.print("Regressing all actions " );
		}
		ADDRNode ret = constrain_naively ? 
				applyMDPConstraints(primed, null, _manager.DD_NEG_INF, constrain_naively, size_change)
				: primed;
		for( final String str : sum_order ){
			if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.print( "*" );// + str );
			}
			ret = computeExpectation( ret, str, null, constrain_naively, size_change,
					do_apricodd, 
					apricodd_epsilon, apricodd_type );
			ArrayList<String> maxes = hsOrder.get(str);
			if( maxes != null ){
				ret = maxActionVariables(ret, maxes, size_change, do_apricodd, apricodd_epsilon, apricodd_type);
			}
		}
		if( _dbg.compareTo( DEBUG_LEVEL.SOLUTION_INFO ) >= 0 ){
			System.out.println();			
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
		
		ADDRNode reward_added = hsDiscountAndAddReward( ret, constrain_naively, size_change,
				do_apricodd, 
				apricodd_epsilon, apricodd_type );
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
			System.out.println( "reward added " );
		}
		if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
			System.out.println( "showing diagrams");
			_manager.showGraph( ret, reward_added);
		}
		
		ret = reward_added;
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
				&& _manager.hasVars( ret, _mdp.getFactoredActionSpace().getActionVariables() ) ){
			try{
				throw new Exception("Has action vars after max");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
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
		return new ADDValueFunction(v_func, null, null, _manager);
	}
	
	private ADDRNode hsDiscountAndAddReward(
			final ADDRNode input, 
			boolean constrain_naively, List<Long> size_change,
			boolean do_apricodd, double apricodd_epsilon,
			APPROX_TYPE apricodd_type) {
		
		if( _hsReward  == null ){
			makeHSReward();
		}
		
		final ADDRNode discounted = _manager.scalarMultiply(input, _mdp.getDiscount());
//		final ADDRNode discounted_approx = _manager.doApricodd( discounted, 
//				do_apricodd, apricodd_epsilon, apricodd_type );
		ADDRNode ret = discounted; 
		
		if( size_change != null ){
			size_change.addAll( _manager.countNodes( input, ret ) );
		}

		final ADDRNode added_rew = _manager.apply( ret, _hsReward, DDOper.ARITH_PLUS );
//		final ADDRNode added_rew_approx = _manager.doApricodd( added_rew, 
//					do_apricodd,
//					apricodd_epsilon, apricodd_type );
		final ADDRNode added_rew_constrained 
			= constrain_naively ? 
					added_rew : applyMDPConstraints(added_rew, null, _manager.DD_NEG_INF,
				constrain_naively, size_change );
		if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
			System.out.println("Showing diagrams after one addition" );
			_manager.showGraph( ret, _hsReward, added_rew, added_rew_constrained );
		}
			
//			System.out.println("Adding reward " 
		if( size_change != null ){
			size_change.addAll( _manager.countNodes( added_rew , added_rew_constrained ) );
		}
		ret = added_rew_constrained;
		return ret;
	}

	private void makeHSReward() {
		final List<ADDRNode> rewards = _mdp.getRewards();
		ADDRNode reward = _manager.DD_ZERO;
		for( ADDRNode this_reward : rewards ){
				reward = _manager.apply(reward, this_reward, DDOper.ARITH_PLUS);
		}
		_hsReward = maxActionVariables(reward, _mdp.getElimOrder(), 
				null, false, 0, APPROX_TYPE.NONE);
	}

	private ADDRNode maxOneActionVariable(
			final ADDRNode input, final String act_var,
			final boolean constrain_naively, final List<Long> size_change,
			final boolean do_apricodd, final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type) {
		ADDRNode ret = input;
		if( size_change != null ){
			size_change.addAll( _manager.countNodes(input) );
		}
		
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
			System.out.println("Maxing " + act_var );
			System.out.println("Size of input " + _manager.countNodes( ret ) );
		}
		final ADDRNode maxd = _manager.marginalize(ret, act_var, DDMarginalize.MARGINALIZE_MAX);
//		final ADDRNode maxd_approx = _manager.doApricodd(maxd, do_apricodd,
//				apricodd_epsilon, apricodd_type);
		if( size_change != null ){
			size_change.addAll( _manager.countNodes( maxd ) );
		}
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
//		_manager.flushCaches( );
		return ret;
	}

	private ADDRNode multiplyActionPreconditions( final ADDRNode input, 
			final NavigableMap<String, Boolean> action ) {
//		final ArrayList<ADDRNode> all_constraint = convertToNegInfDD( __action_precondition );
		ADDRNode constraint = _manager.sumDD( __action_precondition_neginf );
		
		if( action != null ){
			constraint =_manager.restrict( constraint, action ); 
		}
		final ADDRNode ret = _manager.apply( input, constraint, DDOper.ARITH_PLUS );
		return ret;
	}

	private ArrayList<ADDRNode> convertToNegInfDD(final ArrayList<ADDRNode> input) {
		final ArrayList<ADDRNode>  ret = new ArrayList<ADDRNode>(input.size());
		for( final ADDRNode in : input ){
			final ADDRNode inter = _manager.remapLeaf(in, _manager.DD_ZERO, _manager.DD_NEG_INF );
			ret.add( _manager.remapLeaf( inter, _manager.DD_ONE, _manager.DD_ZERO) );
		}
		return ret;
	}
	
	private ADDRNode[] convertToNegInfDD(final ADDRNode... input) {
		final ADDRNode[]  ret = new ADDRNode[input.length];
		int i = 0;
		for( final ADDRNode in : input ){
			final ADDRNode inter = _manager.remapLeaf(in, _manager.DD_ZERO, _manager.DD_NEG_INF );
			ret[i++] = ( _manager.remapLeaf( inter, _manager.DD_ONE, _manager.DD_ZERO) );
		}
		return ret;
	}

	public ADDRNode maxActionVariables( final ADDRNode input, 
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
//			final ADDRNode maxd_approx = _manager.doApricodd(maxd, do_apricodd,
//					apricodd_epsilon, apricodd_type);
			if( size_change != null ){
				size_change.addAll( _manager.countNodes( maxd ) );
			}
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
//			_manager.flushCaches( );
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
		ADDRNode primed = _manager.remapVars(input, _mdp.getPrimeRemap() );
		primed = constrain_naively ? 
				applyMDPConstraints(primed, null, _manager.DD_NEG_INF, 
						constrain_naively, size_change) : primed ;
//		final ADDRNode constrained_zero = _manager.remapLeaf( primed , 
//				_manager.DD_NEG_INF, _manager.DD_ZERO );
		final Deque< UnorderedPair< NavigableMap< String, Boolean >, 
				UnorderedPair<Integer, ADDRNode > > > 
					evaluations = new ArrayDeque<UnorderedPair<NavigableMap<String, Boolean>, UnorderedPair<Integer, ADDRNode>>>();
		evaluations.addFirst( new UnorderedPair< NavigableMap< String, Boolean >, 
				UnorderedPair<Integer, ADDRNode > >( new TreeMap<String, Boolean>(), 
				new UnorderedPair<Integer, ADDRNode>( 0, primed ) ) );

		final ArrayList<String> sum_order = _mdp.getSumOrder();
		ADDRNode value_func = _manager.DD_NEG_INF;
		ADDRNode policy = _manager.DD_ZERO;
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
			ADDRNode this_z = maxActionVariables( ret, all_action_vars,
					size_change, do_apricodd, apricodd_epsilon, apricodd_type );
			this_z = _manager.doApricodd(this_z, do_apricodd, apricodd_epsilon, apricodd_type);
			
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
			_manager.flushCaches();
			
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
		ArrayList<ADDRNode> all_constraint = convertToNegInfDD( __action_constraints );
		ADDRNode constraint = _manager.productDD( all_constraint ); 
		if( action != null ){
			constraint = _manager.restrict( constraint, action );
		}
		final ADDRNode ret = _manager.apply(input, constraint, DDOper.ARITH_PLUS);
		return ret;
	}

	private ADDRNode multiplyStateConstraints( final ADDRNode input ) {
//		final ArrayList<ADDRNode> all_constraint = convertToNegInfDD( __state_constraints );
		ADDRNode constraint = _manager.sumDD( __state_constraints_neginf );
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
//		ADDRNode mult_constrained = applyMDPConstraints(mult, action, _manager.DD_ZERO,
//				constrain_naively, size_change );
//		_manager.flushCaches(   );
		
		final ADDRNode summed = _manager.marginalize(mult, str, DDMarginalize.MARGINALIZE_SUM);
//		final ADDRNode summed_approx = _manager.doApricodd( summed, do_apricodd , apricodd_epsilon, apricodd_type );
		ADDRNode summed_constrained = 
				constrain_naively ? summed : applyMDPConstraints(summed, action, _manager.DD_NEG_INF,
				constrain_naively, size_change);
//		_manager.flushCaches(   );
		
		if( _dbg.compareTo(DEBUG_LEVEL.DIAGRAMS) >= 0 ){
			System.out.println("showing diagrams");
			_manager.showGraph(ret, this_cpt, mult, summed, summed, summed_constrained);
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
		_manager.flushCaches(  );
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
		ADDRNode ret = constrain_naively ? 
			applyMDPConstraints(primed, action, _manager.DD_NEG_INF, 
					constrain_naively, size_change) : primed;
		
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
		
		ret = _manager.doApricodd(ret, do_apricodd, apricodd_epsilon, apricodd_type);
		
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
//		final ADDRNode discounted_approx = _manager.doApricodd( discounted, 
//				do_apricodd, apricodd_epsilon, apricodd_type );
		ADDRNode ret = discounted; 
		
		if( size_change != null ){
			size_change.addAll( _manager.countNodes( input, ret ) );
		}

		
		for( ADDRNode this_reward : rewards ){
			final ADDRNode added_rew = _manager.apply( ret, this_reward, DDOper.ARITH_PLUS );
//			final ADDRNode added_rew_approx = _manager.doApricodd( added_rew, 
//					do_apricodd,
//					apricodd_epsilon, apricodd_type );
			final ADDRNode added_rew_constrained 
				= constrain_naively ? added_rew : 
					applyMDPConstraints(added_rew, action, _manager.DD_NEG_INF,
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
		}
		
//		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
//		}
		_manager.flushCaches( );
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
		int index_added = addPolicyConstraint( policy );
		ret = regress(initial_value_func, withActionVars, false, false,
				constrain_naively, size_change, do_apricodd, apricodd_epsilon, apricodd_type );
		if( !removePolicyConstraint( index_added ) ){
			System.err.println("policy constraint not found");
			System.exit(1);
		}
		return ret._o1.get_valueFn();
	}
	
	public ADDRNode getIIDInitialStates(INITIAL_STATE_CONF init_state_conf,
			double init_state_prob ) {
		final String[] state_vars = _mdp.get_stateVars().toArray(new String[1]);
		ADDRNode ret = null;
		
		switch( init_state_conf ){
		case BERNOULLI :
			ret = _mdp.getIIDBernoulliDistribution(init_state_prob, state_vars);
		case CONJUNCTIVE :
			ret = _mdp.getIIDConjunction(!(init_state_prob==0.0d), state_vars );
		case UNIFORM :
			ret = _mdp.getIIDUniformDistribution( state_vars );
		}
		final ADDRNode constr = _manager.productDD(this.__state_constraints);
		if( !constr.equals(_manager.DD_ONE) ){
			ret = _manager.normalizePDF( ret, constr );
		}
		
		return ret;
	}

	public int addStateConstraint( final ADDRNode input ){
		__state_constraints.add( input );
		__state_constraints_neginf.add( convertToNegInfDD(input)[0] );
		return __state_constraints.size()-1;
	}
	
	public boolean removeStateConstraint( final int index ){
		final boolean ret1 =  __state_constraints.remove( index ) == null;
		final boolean ret2 = __state_constraints_neginf.remove(index) == null;
		return ret1 && ret2;
	}
	
	public boolean removePolicyConstraint( final int index ) {
		final boolean ret1 =  __policy_constraints.remove( index ) == null;
		final boolean ret2 = __policy_constraints_neginf.remove( index ) == null;
		return ret1 && ret2;
	}

	public int addPolicyConstraint(ADDRNode policy) {
		__policy_constraints.add(policy);
		__policy_constraints_neginf.add( convertToNegInfDD(policy)[0] );
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
			System.out.println( i +  " " + ADD_dtr.getBellmanError( naively._o1.get_valueFn() , thing ) );
			thing = naively._o1.get_valueFn();
		}
		manager.showGraph( naively._o1.get_valueFn(), naively._o1.get_jointQFn(), naively._o2._bddPolicy );	
		System.out.println( manager.countNodes( naively._o1.get_valueFn() ,
				naively._o1.get_jointQFn(), naively._o2._bddPolicy ) );
		
		i = 0;
		thing = manager.DD_ZERO;
		UnorderedPair< ADDValueFunction, ADDPolicy> smartly = null;
		while( i++ < 20 ){
			smartly = ADD_dtr.regressAllActions(thing, true, true, false, null, 
					false, 0.0d, APPROX_TYPE.NONE );
			System.out.println( i +  " " + ADD_dtr.getBellmanError( smartly._o1.get_valueFn() , thing ) );
			thing = smartly._o1.get_valueFn();
		}
		
		manager.showGraph( smartly._o1.get_valueFn(), smartly._o1.get_jointQFn(), smartly._o2._bddPolicy );	
		System.out.println( manager.countNodes( smartly._o1.get_valueFn() ,
				smartly._o1.get_jointQFn(), smartly._o2._bddPolicy ) );
		
		
		//execute policy
		naively._o2.executePolicy(3, 4, true, mdp.getHorizon(), mdp.getDiscount()).printStats();
		smartly._o2.executePolicy(3, 4, true, mdp.getHorizon(), mdp.getDiscount()).printStats();
	}

	public static void main(String[] args) {
//		testRegressAction();
//		testRegressAllActions();
//		testSPUDD();
//		testConstrainNaively();
//		testImageAllActions();
		testImagePolicy();
//		testComputeHeuristic();
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
		//execute policyregre
		ADDPolicy policy = new ADDPolicy(manager, mdp.getFactoredStateSpace(), 
				mdp.getFactoredTransition(), mdp.getFactoredReward(), 42);
		policy._bddPolicy = handCodedPolicy;
		policy.executePolicy(3, 4, true, mdp.getHorizon(), mdp.getDiscount()).printStats();
	}

	public static ADDRNode getRebootDeadPolicy(ADDManager manager,
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
		
		policy = dtr.applyMDPConstraints( policy , null, manager.DD_ZERO, true, null);
		return policy;
	}

	public static ADDRNode getNoOpPolicy(final Set<String> actionVariables,
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
			System.out.println( i +  " " + ADD_dtr.getBellmanError( ret._o1.get_valueFn() , thing ) );
			thing = ret._o1.get_valueFn();
		}
//		manager.showGraph( (ADDRNode[])ret._o1._qFn.values().toArray() );
		manager.showGraph( ret._o1.get_valueFn(), ret._o2._addPolicy );	
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
			System.out.println( i +  " " + ADD_dtr.getBellmanError( ret._o1.get_valueFn() , thing ) );
			thing = ret._o1.get_valueFn();
		}
		manager.showGraph( ret._o1.get_valueFn(), ret._o1.get_jointQFn(), ret._o2._bddPolicy );	
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
		final int index_added = addPolicyConstraint( policy );
		ret = regressMBFAR(input, make_policy, constraint_naively, BIGDD, null ,
				do_apricodd, 
				apricodd_epsilon, apricodd_type );
		if( !removePolicyConstraint( index_added ) ){
			System.err.println("policy constraint not found");
			System.exit(1);
		}
		return ret._o1.get_valueFn();
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
		_manager.flushCaches();
		
		return (UnorderedPair<T, U>) ret;
	}

	public boolean terminate(final double BE,  
			final int iteration, 
			final double epsilon,
			final int horizon) {
		return ( iteration >= horizon || BE <= epsilon );
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
