package dtr.add;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
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

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

import rddl.EvalException;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_NAME;
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
import factored.mdp.define.FactoredState;

public class ADDDecisionTheoreticRegression implements
SymbolicRegression<ADDNode, ADDRNode, ADDINode, ADDLeaf, RDDLFactoredStateSpace, 
RDDLFactoredActionSpace> {

	public enum BACKUP_TYPE{
		VMAX, VI_SPUDD, VI_FAR, VI_MBFAR, MONTE
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
	protected ADDRNode domain_constraints;
	protected ADDRNode domain_constraints_neg_inf;

	public enum INITIAL_STATE_CONF{
		//UNIFORM, BERNOULLI, 
		CONJUNCTIVE, RDDL
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
			final ADDRNode Vmax = _mdp.getVMax(-1,-1);
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
	public UnorderedPair< ADDRNode, UnorderedPair< ADDRNode , Double > > backup(
			final ADDRNode current_value, 
			final ADDRNode cur_policy,//BDD
			final ADDRNode source_value_fn, 
//			final ADDRNode from, 
			final ADDRNode to, 
			final BACKUP_TYPE backup_type, 
			final boolean do_apricodd,
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ,
			final boolean makePolicy,
			final long BIGDD ,
			final boolean constrain_naively,
			final ADDRNode policy_constraint 
			){

		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
			
		    if( source_value_fn.getMin() == _manager.getNegativeInfValue() ){
			try{
			    throw new Exception("has -inf value in state that is being backed up");//THIS SI HAPPENING
			}catch( Exception e ){
			    e.printStackTrace();
			    System.exit(1);
			}
		    }
		}
		//	    	System.out.println("Backup");
		//	    	System.out.println("From, To = " + _manager.countNodes(from, to).toString() );
		if( to.equals(_manager.DD_ONE) ){
			System.out.println("WARNING : VI backup! Check generalization" );
		}

		//	    	if( from.equals(_manager.DD_ONE) ){
		//	    	    System.out.println("WARNING : backing up from all states " );
		//	    	}else 
		//			if( from.equals(_manager.DD_ZERO) ){
		//	    	    System.out.println("WARNING : backing up from no states " );
		//	    	    return new UnorderedPair<ADDRNode, UnorderedPair<ADDRNode,Double>>( current_value, new UnorderedPair<ADDRNode,Double>( cur_policy, 0.0d ) );
		//	    	}
		//	    	
		//	    	if( to.equals(_manager.DD_ONE) ){
		//	    	    System.out.println("WARNING : backing up to all states " );
		//	    	}else 
		if( to.equals(_manager.DD_ZERO ) ){
			System.out.println("WARNING : backing up TO no states " );
			return new UnorderedPair<ADDRNode, UnorderedPair<ADDRNode,Double>>( current_value, new UnorderedPair<ADDRNode,Double>( cur_policy, 0.0d ) );
		}

		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
//	    	if( _manager.hasVars(from, _mdp.get_actionVars()) ){
//	    		try{
//	    			throw new Exception("From has action vars");
//	    		}catch( Exception e ){
//	    			e.printStackTrace();
//	    			System.exit(1);
//	    		}
//	    	}
//	    	
	    	if( _manager.hasVars(source_value_fn, _mdp.get_actionVars()) ){
	    		try{
	    			throw new Exception("Source vfn has action vars");
	    		}catch( Exception e ){
	    			e.printStackTrace();
	    			System.exit(1);
	    		}
	    	}
	    	

	    	if( _manager.hasVars(to, _mdp.get_actionVars()) ){
	    		try{
	    			throw new Exception("to has action vars");
	    		}catch( Exception e ){
	    			e.printStackTrace();
	    			System.exit(1);
	    		}
	    	}
		}
		
		final ADDRNode this_constraint =
				policy_constraint == null ? to : _manager.BDDIntersection(policy_constraint, to);
		final ADDRNode all_constraints = _manager.BDDIntersection( domain_constraints, this_constraint );
		final ADDRNode all_constraints_neg_inf = convertToNegInfDD(all_constraints)[0];
		
		final ADDRNode unprimed = source_value_fn;
//				_manager.BDDIntersection(source_value_fn, from) ;
//				constrain_naively ?     
//				    _manager.constrain(source_value_fn, from, _manager.DD_ZERO);

		final ADDRNode primed = _manager.remapVars( unprimed  , _mdp.getPrimeRemap() );
		
//		if( primed.getMin() == _manager.getNegativeInfValue() ){
//			try{
//				throw new Exception("source has -inf value");
//			}catch( Exception e ){
//				e.printStackTrace();
//			}
//		}
		
		if( !backup_type.equals(BACKUP_TYPE.VI_FAR) ){
			try{
				throw new Exception("backup type not FAR not supported yet");
			}catch( Exception e ){
				e.printStackTrace();
			}
		}
		
		final List<ADDRNode> reward_dds = _mdp.getRewards();
		final Map<String, ADDRNode> cpt_dds = _mdp.getCpts();
		
//		final int state_constraint = addStateConstraint(theConstraint);

		
		ADDRNode value_ret = constrain_naively ? 
				_manager.apply( primed, all_constraints_neg_inf, DDOper.ARITH_PLUS ) : primed;
				
		for( final String ns_var : _mdp.getSumOrder() ){
			if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
				System.out.println("expectation " + ns_var );
			}
			
			final ADDRNode this_cpt = cpt_dds.get( ns_var );
			//remove inconsistent things in cpt without adding vars
//			final ADDRNode restricted_cpt = _manager.constrain(this_cpt,
//					this_constraint, _manager.DD_ZERO );
			
			//multiply
			value_ret = _manager.apply( value_ret, this_cpt, DDOper.ARITH_PROD );
			//marginalize
			value_ret = _manager.marginalize( value_ret, ns_var, DDMarginalize.MARGINALIZE_SUM );
			value_ret = constrain_naively ? _manager.apply( value_ret, all_constraints_neg_inf, DDOper.ARITH_PLUS ) :
				_manager.constrain( value_ret, all_constraints, _manager.DD_NEG_INF );
		}
//		value_ret = constrain_naively ? 
//				_manager.apply( value_ret, all_constraints_neg_inf, DDOper.ARITH_PLUS ) : 
//					value_ret;
		
		value_ret = _manager.scalarMultiply(value_ret, _mdp.getDiscount() );
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){
			System.out.println("adding reward "  );
		}
		
		value_ret = constrain_naively ? _manager.apply( value_ret, all_constraints_neg_inf, DDOper.ARITH_PLUS ) :
			_manager.constrain( value_ret, all_constraints, _manager.DD_NEG_INF );
		for( final ADDRNode rew : reward_dds ){
//			final ADDRNode r2 = _manager.constrain(rew, this_constraint, _manager.DD_NEG_INF );
			value_ret = _manager.apply( value_ret, rew, DDOper.ARITH_PLUS );
		}
		
		//has Q now
		value_ret = do_apricodd ? 
				_manager.doApricodd(value_ret, do_apricodd, apricodd_epsilon, apricodd_type) : value_ret;
		final ADDRNode q_func = value_ret;
		value_ret = maxActionVariables(value_ret, _mdp.getElimOrder(), null, do_apricodd, apricodd_epsilon, apricodd_type);
		
//		removeStateConstraint(state_constraint);
		ADDPolicy policy = new ADDPolicy(_manager, _mdp.getFactoredStateSpace(), 
				_mdp.getFactoredTransition(), _mdp.getFactoredReward(), _mdp.getFactoredActionSpace() );
		policy.updateBDDPolicy(value_ret, q_func);
		
		final ADDRNode to_not = _manager.BDDNegate(to);
		ADDRNode new_vfn = 
				_manager.apply(
						_manager.BDDIntersection(value_ret, to),
						_manager.BDDIntersection(current_value, to_not ),
						DDOper.ARITH_PLUS );
		if( do_apricodd ){
			new_vfn = _manager.doApricodd(
					new_vfn, do_apricodd, apricodd_epsilon, apricodd_type);
		}
		
//		if( new_vfn.getMin() == _manager.getNegativeInfValue() ){
//			Syst
//		}
		
		ADDRNode new_policy = _manager.BDDUnion(
				_manager.BDDIntersection(policy._bddPolicy, to),
				_manager.BDDIntersection(cur_policy, to_not ) );
		new_policy = _manager.breakTiesInBDD(new_policy, _mdp.get_actionVars(), false );
		
//		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0 ){

//			if( !_manager.BDDIntersection( current_value, to_not ).
//					equals(_manager.BDDIntersection( new_vfn, to_not )) ){
//				try{
//					throw new Exception("Value of un updated state has changed");
//				}catch( Exception e ){
//					e.printStackTrace();
//					System.exit(1);
//				}
//			}
//		}
			
			if( _manager.hasVars(new_vfn, _mdp.get_actionVars()) ){
	    		try{
	    			throw new Exception("new vfn has action vars");
	    		}catch( Exception e ){
	    			e.printStackTrace();
	    			System.exit(1);
	    		}
	    	}
	    	
			if( _manager.hasVars(new_vfn, _mdp.get_nextStateVars() ) ){
	    		try{
	    			throw new Exception("new vfn has primed vars");
	    		}catch( Exception e ){
	    			e.printStackTrace();
	    			System.exit(1);
	    		}
	    	}
		
		return new UnorderedPair<ADDRNode, UnorderedPair<ADDRNode,Double>>(new_vfn, 
				new UnorderedPair<>(new_policy, Double.NaN) );
				
//		int idx = addStateConstraint(to);
//		UnorderedPair<ADDValueFunction, ADDPolicy> backup = null;
//		switch( backup_type ){
//		case VI_FAR : 
//			//		    	System.out.println("Regressing all actions" );
//			backup = regressAllActions( primed, false, makePolicy, constrain_naively, null,
//					do_apricodd, apricodd_epsilon, apricodd_type );
//			break;//backup will have value and policy for things not in to, 
//			//should be masked
//		case VI_SPUDD :
//			backup = regressSPUDD(primed, false, makePolicy, constrain_naively, null, 
//					do_apricodd, apricodd_epsilon, apricodd_type);
//			break;
//		case VI_MBFAR :
//			backup = regressMBFAR(unprimed, makePolicy, constrain_naively, BIGDD, null,
//					do_apricodd, apricodd_epsilon, apricodd_type);
//			break;
//		}
//
//		//		if( _manager.hasVars(backup._o1.getValueFn(), _mdp.get_actionVars() ) ){
//		//			try{
//		//				throw new Exception("Backup has action vars");
//		//			}catch( Exception e ){
//		//				e.printStackTrace();
//		//				System.exit(1);
//		//			}
//		//		}
//
//		//		System.out.println("Combining");
//		removeStateConstraint(idx);

//		if( !make ){
//			return new UnorderedPair<ADDRNode, UnorderedPair<ADDRNode,Double>>(
//					backup._o1.get_valueFn(), new UnorderedPair<ADDRNode,Double>( 
//							backup._o2._addPolicy == null ? backup._o2._bddPolicy :
//								backup._o2._addPolicy, 
//								getBellmanError(backup._o1.get_valueFn(), source_value_fn) ) );
//		}
		
//		ADDRNode value_ret = _manager.BDDIntersection( backup._o1.getValueFn(), to );
//		ADDRNode saveV = _manager.BDDIntersection( current_value ,
//				to_not );
//		value_ret = _manager.apply( value_ret, saveV, DDOper.ARITH_PLUS );
//		final double residual = getBellmanError(value_ret, source_value_fn );
//
//		ADDRNode policy_ret = null;
//		if( makePolicy ){
//			policy_ret = backup._o2._addPolicy == null ? backup._o2._bddPolicy :
//				backup._o2._addPolicy ;
//			policy_ret = _manager.BDDIntersection(policy_ret, to);//policy in 
//			//to states
//			//zero in others
//			//put input policy in others
//			policy_ret = _manager.BDDUnion( policy_ret, 
//					_manager.BDDIntersection( cur_policy, to_not ) );
//
//			policy_ret = applyMDPConstraints(policy_ret, null, _manager.DD_ZERO, constrain_naively, null);
//
////			policy_ret = _manager.breakTiesInBDD(policy_ret, _mdp.get_actionVars(), false);
//		}
//
//		//		if( _manager.hasVars( value_ret, _mdp.get_actionVars() ) ){
//		//			try{
//		//				throw new Exception("Backup has action vars");
//		//			}catch( Exception e ){
//		//				e.printStackTrace();
//		//				System.exit(1);
//		//			}
//		//		}

		

			//			updated state - value not = neg inf
//			if( _manager.BDDIntersection(value_ret, to).getMin() == _manager.getNegativeInfValue() ){
//				try{
//					throw new Exception("Updated state has value -inf");
//				}catch( Exception e ){
//					e.printStackTrace();
//					System.exit(1);
//				}
//			}
//		}

		//current value may be zero hence cause increase error
		//		final ADDRNode diff_to = _manager.apply( 
		//				_manager.BDDIntersection( current_value, to ), 
		//				_manager.BDDIntersection( value_ret, to ),
		//				DDOper.ARITH_MINUS ) ;
		//		
		//		}

//		return new UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double> >( 
//				value_ret, new UnorderedPair<ADDRNode, Double>( policy_ret , residual ) );
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

		domain_constraints = _manager.productDD( 
				__action_precondition, __state_constraints, __policy_constraints, __action_constraints );
		domain_constraints_neg_inf = convertToNegInfDD(domain_constraints)[0];
		
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

		ADDRNode constraints = domain_constraints_neg_inf;
		if( action != null ){
			constraints = _manager.restrict(constraints, action);
		}
		//		all_constraints = convertToNegInfDD( Lists.newArrayList( all_constraints ) ).get(0);
		ADDRNode ret = _manager.apply(input, constraints, DDOper.ARITH_PLUS );
		if( ! violate.equals(_manager.DD_NEG_INF) ){
			ret = _manager.remapLeaf(ret, _manager.DD_NEG_INF, violate);//EXPENSIVE
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
		
		ADDRNode constraints = domain_constraints;
		if( action != null ){
			constraints = _manager.restrict(constraints, action);
		}

		ADDRNode ret = input;
		ret = _manager.constrain(ret, constraints, violate);
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
							_mdp.getFactoredTransition(), _mdp.getFactoredReward(),
							_mdp.getFactoredActionSpace() );
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
				RDDLFactoredActionSpace>().setFactoredAction(action);
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
			final ADDRNode policy, final boolean constrain_naively ){
		int index_added = addPolicyConstraint(policy);
		final ADDRNode ret = BDDImage(reachable_states, withActionVars, action_qualification );//, constrain_naively );
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
	// NOTE : using pruning with factored MDP is not correct
	public  ADDRNode BDDImage( final ADDRNode reachable_states, 
			final boolean withActionVars, 
			final DDQuantify action_quantification ){

		if ( reachable_states.equals(_manager.DD_ZERO) ){
			try{
				throw new Exception("image input is zero");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}

		if( !_mdp.isTransitionRelationReady(withActionVars) ){
			_mdp.makeTransitionRelation( withActionVars );
		}
//		final boolean constrain_naively = true;

		ADDRNode ret_ns = null; 
		if( withActionVars ){
			ret_ns =  BDDImageFAR( reachable_states, action_quantification, false );
		}
		//		else {
		//			ret_ns =  BDDImageSPUDD( reachable_states, action_quantification, constrain_naively );
		//		}
		ADDRNode ret = _manager.remapVars(ret_ns, _mdp.getPrimeUnMap() );
		ret = _manager.BDDIntersection( ret, _manager.productDD( __state_constraints ) );
		if( ret.equals(_manager.DD_ZERO) ){
			try{
				throw new Exception("Image is zero");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		//		else if( ret.equals(_manager.DD_ONE) ){
		//		    System.out.println("WARNING : image is one");
		//		/}

		return ret;

	}

	//	private ADDRNode BDDImageSPUDD(final ADDRNode reachable_states,
	//			final DDQuantify quantification, final boolean constrain_naively) {
	//		final List<NavigableMap<String, Boolean>> actions = _mdp.getFullRegressionOrder();
	//		final Map<Map<String, Boolean>, Map<String, ADDRNode>> theRelations = _mdp.getTransitionRelationSPUDD();
	//		final Map<NavigableMap<String, Boolean>, ArrayList<Pair<String, String>>> 
	//			theRelationsLast = _mdp.getTransitionRelationLastSPUDD();
	//		ADDRNode ret = _manager.DD_ZERO;
	//		final int idx_reach = addStateConstraint(reachable_states);
	//		
	//		for( final NavigableMap<String, Boolean> action : actions ){
	//			ADDRNode ret_action = _manager.DD_ONE; 
	//			ret_action = applyMDPConstraints(ret_action, action, _manager.DD_ZERO, constrain_naively, null);
	//			final Map<String, ADDRNode> action_relation = theRelations.get( action );
	//			final ArrayList<Pair<String, String>> action_last = theRelationsLast.get( action );
	//			int lastSeen_index = 0;
	//			final int lastSeen_size = action_last.size();
	//			for( final String ns_var : _mdp.getSumOrder() ){
	//				ret_action = _manager.apply( ret_action, 
	//						action_relation.get( ns_var ),
	//						DDOper.ARITH_PROD );
	//				ret_action = applyMDPConstraints(ret_action, action, _manager.DD_ZERO, constrain_naively, null);
	//				
	//				if( lastSeen_index == lastSeen_size ){
	//					continue;
	//				}else{//check for state vars to quantify
	//					Pair<String, String> first_lastSeen = action_last.get( lastSeen_index );
	//					while( ( first_lastSeen._o1 == null || first_lastSeen._o1 == ns_var ) && 
	//							lastSeen_index < lastSeen_size ){ //counts on interned string
	//						ret_action = _manager.quantify( ret_action, first_lastSeen._o2, quantification );
	//						ret_action = applyMDPConstraints(ret_action, action, _manager.DD_ZERO, constrain_naively, null);
	//						
	//						if( ++lastSeen_index < lastSeen_size ){
	//							first_lastSeen = action_last.get( lastSeen_index );
	//						}
	//					}
	//				}
	//			}
	//			
	//			for( final String state_var : _mdp.getFactoredStateSpace().getStateVariables() ){
	//				ret_action = _manager.quantify( ret_action ,  state_var , quantification);
	//			}
	//			 
	//			for( final String action_var : _mdp.getFactoredActionSpace().getActionVariables() ){
	//				ret_action = _manager.quantify( ret_action, action_var, quantification);
	//			}
	//			
	//			ret = _manager.apply( ret, ret_action, DDOper.ARITH_MAX );
	//		}
	//		if( !removeStateConstraint(idx_reach) ){
	//			try{
	//				throw new Exception("Could not remove state constraint");
	//			}catch( Exception e ){
	//				e.printStackTrace();
	//				System.exit(1);
	//			}
	//		}
	//		return ret;
	//	}

	private static void testImageAllActions(){
		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp.rddl", "./rddl/sysadmin_star_2_1.rddl", 
				true, DEBUG_LEVEL.SOLUTION_INFO, ORDER.GUESS, true, 42);

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
			iter = ADD_dtr.BDDImage(iter, true,  DDQuantify.EXISTENTIAL);//, false);
		}

		//		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp.rddl", "./rddl/sysadmin_star_2_1.rddl", 
		//				false, DEBUG_LEVEL.SOLUTION_INFO, ORDER.GUESS, true, 42);
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
		//			iter = ADD_dtr.BDDImage(iter, false,  DDQuantify.EXISTENTIAL );//, true);
		//		}
	}

	private static void testImagePolicy(){
		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp.rddl", "./rddl/sysadmin_star_2_2.rddl", 
				true, DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, true, 42);

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

		ADDRNode policy = manager.DD_ONE;
		for( final String action_var : mdp.getFactoredActionSpace().getActionVariables() ){
			//			policy = ADD_dtr.getNoOpPolicy(mdp.get_actionVars(), manager);
			policy = manager.apply( policy, 
					manager.getIndicatorDiagram(action_var, true),
					DDOper.ARITH_PROD );
		}

		ADDRNode iter = states;
		while( i++ < 1 ){
			//			manager.showGraph( iter );
			System.out.println( i +  " " + iter );
			iter = ADD_dtr.BDDImagePolicy(iter, true,  DDQuantify.EXISTENTIAL, policy, false );
		}

		System.out.println( i +  " " + iter );
		//		manager.showGraph(iter);
		//		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp.rddl", "./rddl/sysadmin_star_2_2.rddl", 
		//				false, DEBUG_LEVEL.SOLUTION_INFO, ORDER.GUESS, true, 42);

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
		//			iter = ADD_dtr.BDDImagePolicy(iter, false,  DDQuantify.EXISTENTIAL, policy, false );
		//		}

	}

	//get transition relation
	//convert CPTs to BDDs
	private ADDRNode BDDImageFAR(final ADDRNode unprimed_reachable_states,
			final DDQuantify quantification, final boolean constrain_naively) {
		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
				&& _manager.hasVars( unprimed_reachable_states, _mdp.getFactoredActionSpace().getActionVariables() )
				&& _manager.hasVars( unprimed_reachable_states, _mdp.getFactoredStateSpace().getNextStateVars() ) ){
			try{
				throw new Exception("Has action/next state vars in Image.");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}

		final NavigableMap<String, ADDRNode> transitionRelation = _mdp.getTransitionRelationFAR();
		final ArrayList<Pair<String, String>> lastSeen = _mdp.getTransitionRelationLastFAR();
		final ArrayList<String> sumOrder = _mdp.getSumOrder();
		int lastSeen_index = 0;
		final int lastSeen_size = lastSeen.size();
		//this step removes only illegal states from reachable states
		//		final int idx_reach = addStateConstraint(unprimed_reachable_states);

		ADDRNode ret = _manager.productDD( __action_constraints, __state_constraints, __policy_constraints ); 
		ret = _manager.productDD(ret, unprimed_reachable_states );
		if( ret.equals(_manager.DD_ZERO) ){
			try{
				throw new Exception("Image source is zero");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		//		ret = applyMDPConstraints(ret, null, _manager.DD_ZERO, constrain_naively, null );

		//		System.out.println("Image computation");

		for( final String nextState : sumOrder ){
//						System.out.print("Image for variable ");
//						System.out.print( nextState );
//						System.out.println();
			final ADDRNode theRelation = transitionRelation.get(nextState);
			ret = _manager.apply( ret , theRelation, DDOper.ARITH_PROD );
			//this is necessary to include action constraints
			//			ret = applyMDPConstraints(ret, null, _manager.DD_ZERO, constrain_naively, null );

			if( lastSeen_index < lastSeen_size ){
				Pair<String, String> first_lastSeen = lastSeen.get( lastSeen_index );
				while( ( first_lastSeen._o1 == null || first_lastSeen._o1 == nextState ) && 
						lastSeen_index < lastSeen_size ){ //counts on interned string
					
//					System.out.println("Quantified " + first_lastSeen._o2 + " " + lastSeen_index );
					
					ret = _manager.quantify( ret, first_lastSeen._o2, quantification );
					if( ++lastSeen_index < lastSeen_size ){
						first_lastSeen = lastSeen.get( lastSeen_index );
					}
				}
			}
			//			ret = applyMDPConstraints(ret, null, _manager.DD_ZERO, constrain_naively, null );
		}

		for( String state_var : _mdp.getFactoredStateSpace().getStateVariables() ){
			ret = _manager.quantify( ret ,  state_var , quantification);
		}

		for( String action_var : _mdp.getFactoredActionSpace().getActionVariables() ){
			ret = _manager.quantify(ret, action_var, quantification);
		}

		//		if( !removeStateConstraint(idx_reach) ){
		//			try{
		//				throw new Exception("Could not remove state constraint");
		//			}catch( Exception e ){
		//				e.printStackTrace();
		//				System.exit(1);
		//			}
		//		}
		//do same for imageSPUDD . check image action and imagepolicy
		//write preimage 
		return ret;
	}

	public ADDRNode BDDImageAction(final ADDRNode reachable_states,
			final DDQuantify quantification,
			final ADDRNode actions,
			final boolean constrain_naively ,
			final boolean with_action_vars ) {

		int idx = addActionConstraint( actions );
		final ADDRNode ret = BDDImage(reachable_states, with_action_vars, quantification );//, constrain_naively );
		if( !removeActionConstraint( idx ) ){
			try{
				throw new Exception("Action constraint not found");
			}catch(Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		return ret;
	}

	public ADDRNode BDDPreImageAction(
			final ADDRNode next_states_unprimed, 
			final ADDRNode action,
			final boolean withActionVars, 
			final DDQuantify quantification, 
			final boolean constrain_naively ){
		final int idx_action_constr = addActionConstraint(action);
		final ADDRNode ret = BDDPreImage(next_states_unprimed, withActionVars, quantification, constrain_naively);

		if( action != null && !removePolicyConstraint( idx_action_constr ) ){
			try{
				throw new Exception("Could not remove policy constraint");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		return ret;
	}

	public ADDRNode BDDPreImagePolicy( 
			final ADDRNode next_states_unprimed, 
			final ADDRNode policy,
			final boolean withActionVars, 
			final DDQuantify quantification, 
			final boolean constrain_naively ){
		final int idx_policy_constr = addPolicyConstraint(policy);
		final ADDRNode ret = BDDPreImage(next_states_unprimed, withActionVars, quantification, constrain_naively);

		if( policy != null && !removePolicyConstraint( idx_policy_constr ) ){
			try{
				throw new Exception("Could not remove policy constraint");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		return ret;
	}
	//	preImage using BDDs
	// OKAY to use pruning here
	//similar to bakups
	public ADDRNode BDDPreImage( 
			final ADDRNode next_states_unprimed, 
			final boolean withActionVars, 
			final DDQuantify quantification, 
			final boolean constrain_naively ){

		if( next_states_unprimed.equals(_manager.DD_ZERO) ){
			try{
				throw new Exception("PreImage input is zero");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}

		if( _dbg.compareTo(DEBUG_LEVEL.SOLUTION_INFO) >= 0
				&& _manager.hasVars( next_states_unprimed, _mdp.getFactoredActionSpace().getActionVariables() )  ){
			try{
				throw new Exception("Has action vars in PreImage.");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}

		//		System.out.println("Preimage conputation" );
		if( !_mdp.isTransitionRelationReady(withActionVars) ){
			_mdp.makeTransitionRelation( withActionVars );
		}

		final NavigableMap<String, ADDRNode> transitionRelation = _mdp.getTransitionRelationFAR();
		final ArrayList<String> sumOrder = _mdp.getSumOrder();

		final ADDRNode legal_next_states = _manager.BDDIntersection(next_states_unprimed, 
				_manager.productDD(__state_constraints) );
		final ADDRNode next_states_primed = _manager.remapVars(legal_next_states, _mdp.getPrimeRemap());
		ADDRNode ret = next_states_primed;
		ret = applyMDPConstraints(ret, null, _manager.DD_ZERO, constrain_naively, null );

		for( final String nextState : sumOrder ){
			//				System.out.println("PreImage for variable " + nextState );
			//				System.out.print( nextState );
			//				System.out.println();
			final ADDRNode theRelation = transitionRelation.get(nextState);
			ret = _manager.apply( ret , theRelation, DDOper.ARITH_PROD );
			//this is necessary to include action constraints
			//			ret = applyMDPConstraints(ret, null, _manager.DD_ZERO, constrain_naively, null );

			ret = _manager.quantify( ret, nextState, quantification );
			if( !constrain_naively ){
				ret = applyMDPConstraints(ret, null, _manager.DD_ZERO, constrain_naively, null );
			}
		}

		for( final String action_var : _mdp.getFactoredActionSpace().getActionVariables() ){
			ret = _manager.quantify( ret, action_var, quantification);
		}

		if( ret.equals(_manager.DD_ZERO ) ){
			try{
				throw new Exception("Preimage is zero");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}else if( ret.equals(_manager.DD_ONE) ){
			System.out.println("Preimage is one");
		}
		return ret;
	}



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
								_mdp.getFactoredTransition(), _mdp.getFactoredReward(),
								_mdp.getFactoredActionSpace() );
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

	public ArrayList<ADDRNode> convertToNegInfDD(final ArrayList<ADDRNode> input) {
		final ArrayList<ADDRNode>  ret = new ArrayList<ADDRNode>(input.size());
		for( final ADDRNode in : input ){
			final ADDRNode inter = _manager.remapLeaf(in, _manager.DD_ZERO, _manager.DD_NEG_INF );
			ret.add( _manager.remapLeaf( inter, _manager.DD_ONE, _manager.DD_ZERO) );
		}
		return ret;
	}

	public ADDRNode[] convertToNegInfDD(final ADDRNode... input) {
		final ADDRNode[]  ret = new ADDRNode[input.length];
		int i = 0;
		for( final ADDRNode in : input ){
			final ADDRNode inter = _manager.BDDNegate(in);
			ret[i++] = _manager.apply( inter, _manager.DD_NEG_INF, DDOper.ARITH_PROD );
			//			remapLeaf(in, _manager.DD_ZERO, _manager.DD_NEG_INF );
			//			ret[i++] = ( _manager.remapLeaf( inter, _manager.DD_ONE, _manager.DD_ZERO) );
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
					_manager.flushCaches();

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
						_mdp.getFactoredReward(), _mdp.getFactoredActionSpace()  );
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
		//		_manager.flushCaches(  );
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
		//		_manager.flushCaches( );
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
		
		Objects.requireNonNull( init_state_conf, "init state conf is null");
		
		Set<String> state_vars_set = _mdp.get_stateVars();
		final String[] state_vars = state_vars_set.toArray(new String[ state_vars_set.size() ]);
		ADDRNode ret = null;

		switch( init_state_conf ){
//		case BERNOULLI :
//			ret = _mdp.getIIDBernoulliDistribution(init_state_prob, state_vars);
//			break;
		case CONJUNCTIVE :
			ret = _mdp.getIIDConjunction(!(init_state_prob==0.0d), state_vars );
			break;
//		case UNIFORM :
//			ret = _mdp.getIIDUniformDistribution( state_vars );
//			break;
		case RDDL : 
			final NavigableMap<String, Boolean> initialStateSet = _mdp.getInitialState();
			System.out.println( initialStateSet );
			ret = _manager.getProductBDDFromAssignment( initialStateSet );
			break;
		}
//		final ADDRNode constr = _manager.productDD(this.__state_constraints);
//		if( !constr.equals(_manager.DD_ONE) ){
//			ret = _manager.normalizePDF( ret, constr );
//		}

		return ret;
	}

	public int addStateConstraint( final ADDRNode input ){
		__state_constraints.add( input );
		__state_constraints_neginf.add( convertToNegInfDD(input)[0] );
		return __state_constraints.size()-1;
	}

	public int addActionConstraint( final ADDRNode input ){
		__action_constraints.add( input );
		__action_constraints_neginf.add( convertToNegInfDD(input)[0] );
		return __action_constraints.size()-1;
	}

	public boolean removeStateConstraint( final int index ){
		final boolean ret1 =  __state_constraints.remove( index ) != null;
		final boolean ret2 = __state_constraints_neginf.remove(index) != null;
		return ret1 && ret2;
	}

	public boolean removeActionConstraint( final int index ){
		final boolean ret1 =  __action_constraints.remove( index ) != null;
		final boolean ret2 = __action_constraints_neginf.remove(index) != null;
		return ret1 && ret2;
	}

	public boolean removePolicyConstraint( final int index ) {
		final boolean ret1 =  __policy_constraints.remove( index ) != null;
		final boolean ret2 = __policy_constraints_neginf.remove( index ) != null;
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
		naively._o2.executePolicy(3, 4, true, mdp.getHorizon(), mdp.getDiscount(),
				new Random(42), new Random(23), new Random(61) ).printStats();
		smartly._o2.executePolicy(3, 4, true, mdp.getHorizon(), mdp.getDiscount(),
				new Random(42), new Random(23), new Random(61) ).printStats();
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

	@SuppressWarnings("unused")
	private static void testEvaluatePolicy() throws EvalException {
		RDDL2ADD mdp = new RDDL2ADD("./rddl/sysadmin_mdp_same.rddl",
				"./rddl/sysadmin_uniring_1_3_0.rddl", 
				true, DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, true, 42);

		ADDDecisionTheoreticRegression ADD_dtr 
		= new ADDDecisionTheoreticRegression(mdp, 42);
		ADDManager manager = mdp.getManager();

		ADDRNode handCodedPolicy = ADD_dtr.getRebootDeadPolicy(  );

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

		ADDRNode handCodedPolicy = ADD_dtr.getRebootDeadPolicy(  );

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
				mdp.getFactoredTransition(), mdp.getFactoredReward() , mdp.getFactoredActionSpace() );
		policy._bddPolicy = handCodedPolicy;
		policy.executePolicy(3, 4, true, mdp.getHorizon(), mdp.getDiscount(),
				new Random(42), new Random(23), new Random(61) ).printStats();
	}

	public static ADDRNode getRandomAction( 
			final ADDManager manager,
			final ADDDecisionTheoreticRegression dtr, 
			final ADDRNode state, final Random rand ) throws EvalException {

		ADDRNode policy = dtr.applyMDPConstraints( state, null, manager.DD_ZERO, true, null );
		return manager.sampleBDD(policy, rand, 1);
	}

	public ADDRNode getRebootDeadPolicy() throws EvalException {

		ADDRNode policy = _manager.DD_ONE;
		int to_go =  _mdp._i._nNonDefActions;

		for( final String actionVar : _mdp.get_actionVars() ){
			//			ADDRNode rebooter = manager.getIndicatorDiagram(actionVar, true);
			//			ADDRNode runner = manager.getIndicatorDiagram(actionVar.replace("reboot", "running"), 
			//					false );
			//			ADDRNode thisComputerReboot = manager.apply( runner, rebooter, DDOper.ARITH_PROD );
			//			
			//			ADDRNode thisComputerDontReboot = manager.apply( 
			//					manager.getIndicatorDiagram(actionVar, false),
			//					manager.getIndicatorDiagram(actionVar.replace("reboot", "running"), true),
			//					DDOper.ARITH_PROD );

			ADDRNode thisPolicy = _manager.getINode( 
					actionVar.replace("reboot", "running"), 
					_manager.getIndicatorDiagram(actionVar, false),
					_manager.getIndicatorDiagram(actionVar, true) );

			policy = _manager.BDDIntersection(policy, thisPolicy);//, DDOper.ARITH_PROD );
			--to_go;
			if( to_go == 0 ){
				break;
			}
		}

		policy = applyMDPConstraints( policy , null, _manager.DD_ZERO, true, null);
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
		ret._o2.executePolicy(3, 4, true, mdp.getHorizon(), mdp.getDiscount(),
				new Random(42), new Random(31), new Random(63) ).printStats();
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
		ADDRNode thing = manager.getINode("running__c1", manager.getLeaf( 4d), 
				manager.getLeaf(7.5) );
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
		ret._o2.executePolicy(3, 4, true, mdp.getHorizon(), mdp.getDiscount(),
				new Random(42), new Random(21), new Random(43) ).printStats();
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
			if( LIMIT_EVAL && size > bigdd ){
				System.out.println("MB stopping evaluation");
				break;
			}
			_manager.flushCaches();
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

	public UnorderedPair<ADDRNode, ADDRNode> getGreedyPolicy(
			final ADDRNode dd_with_action) {
		final ADDRNode constrainted = applyMDPConstraintsNaively(dd_with_action,
				null, _manager.DD_NEG_INF, null );

		final ADDRNode max = maxActionVariables(constrainted, 
				_mdp.getElimOrder(), null, false, 0.0d, APPROX_TYPE.NONE);
		final ADDRNode diff = _manager.apply( max, constrainted, DDOper.ARITH_MINUS );
		final ADDRNode greedyPolicy = _manager.threshold(diff, 0.0d, false );
		return new UnorderedPair<ADDRNode, ADDRNode>( max, greedyPolicy );
	}

//	public List<ADDRNode> maxActionVariables(List<ADDRNode> rewards,
//			ArrayList<String> elimOrder, List<Long> size_change,
//			boolean do_apricodd, double apricodd_epsilon,
//			APPROX_TYPE apricodd_type) {
//		final List<ADDRNode> ret = new ArrayList<ADDRNode>();
//		for( final ADDRNode rew : rewards ){
//			ret.add( maxActionVariables(rew, elimOrder, size_change, do_apricodd,
//					apricodd_epsilon, apricodd_type));
//		}
//		return Collections.unmodifiableList(ret);
//	}

	public double get_prob_transition(
			final FactoredState<RDDLFactoredStateSpace> cur_state,
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> cur_action,
			final FactoredState<RDDLFactoredStateSpace> next_state) {
		return get_prob_transition( cur_state.getFactoredState(), 
				cur_action.getFactoredAction(), next_state.getFactoredState() );
	}

	public double get_prob_transition(
			final NavigableMap<String, Boolean> factoredState,
			final NavigableMap<String, Boolean> factoredAction,
			final NavigableMap<String, Boolean> factoredNextState) {
		final Map<String, ADDRNode> cpts = _mdp.getCpts();
		double prob_transition = 1.0d;

		for( final String ns : _mdp.getSumOrder() ){
			final ADDRNode cpt_add = cpts.get( ns );
			ADDRNode cur = cpt_add;
			while( cur.getNode() instanceof ADDINode ){
				final String testVar = cur.getTestVariable();
				Boolean val = null;
				if( _mdp.isActionVariable(testVar) ){
					val = factoredAction.get( testVar );
				}else if( _mdp.isNextStateVariable(testVar) ){
					val = factoredNextState.get( testVar.subSequence(0, testVar.length()-1 ) );
				}else{
					val = factoredState.get( testVar );
				}
				val = val == null ? false : val ;
				cur = val ? cur.getTrueChild() : cur.getFalseChild();
			}
			final double val = ((ADDLeaf)cur.getNode()).getMax();
			if( val == 0.0d ){
				try{
					throw new Exception("Prob of transition is zero");
				}catch( Exception e ){
					e.printStackTrace();
					System.exit(1);
				}
			}
			prob_transition *= val;
		}

		//		System.out.println( factoredState.toString() );
		//		System.out.println( factoredAction.toString() );
		//		System.out.println( factoredNextState.toString() );
		//		System.out.println( "Prob = " + prob_transition );

		return prob_transition;
	}
	//	TODO
	//	public void checkModel( final NavigableMap<String, Boolean> action ){
	//		
	//	}

	public ADDRNode getDomainConstraintsNegInf() {
		return domain_constraints_neg_inf;
	}

	public ADDRNode getDomainConstraintsBDD() {
		return domain_constraints;
	}

	
//	public ADDRNode breakActionTies( final ADDRNode policy, 
//			final FactoredState<RDDLFactoredStateSpace> actual_state  ) {
//		Set<String> action_vars = _mdp.get_actionVars();
//		ADDRNode restricted_policy = _manager.restrict(policy, actual_state.getFactoredState());
//		ADDRNode ret = _manager.DD_ONE;
//		while( restricted_policy.getNode() instanceof ADDINode ){
//			final String var = restricted_policy.getTestVariable();
//			if( !action_vars.contains( var ) ){
//				try{
//					throw new Exception("state var in policy action");
//				}catch( Exception e ){
//					e.printStackTrace();
//					System.exit(1);
//				}
//			}
//			if( restricted_policy.getTrueChild().getMax() == 1 ){
//				ret = _manager.BDDIntersection( ret, _manager.getIndicatorDiagram(var, true) );
//				restricted_policy = restricted_policy.getTrueChild();
//			}else{
//				ret = _manager.BDDIntersection( ret, _manager.getIndicatorDiagram(var, false) );
//				restricted_policy = restricted_policy.getFalseChild();
//			}
//		}
//		return ret;
//	}

//	public ADDRNode getGreedyAction( final ADDRNode state_dd ) {
//		final ADDRNode state_dd_neg_inf = _manager.apply( 
//				_manager.BDDNegate( state_dd ), _manager.DD_NEG_INF, DDOper.ARITH_PROD );
//		ADDRNode max_actions = applyMDPConstraintsNaively(state_dd_neg_inf ,
//				null, _manager.DD_NEG_INF, null );
//		for( final ADDRNode rew : _mdp.getRewards() ){
//			max_actions = _manager.apply( max_actions, rew, DDOper.ARITH_PLUS );
//		}
//		final ADDRNode state_value = maxActionVariables(max_actions, _mdp.getElimOrder(), 
//				null, false, 0, null );
//		ADDRNode diff = _manager.apply( state_value, max_actions, DDOper.ARITH_PLUS );
//		ADDRNode policy = _manager.threshold(diff, 0, false );
//		if( policy.equals(_manager.DD_ZERO ) ){
//			try{
//				throw new Exception("policy not initialized properly");
//			}catch( Exception e ){
//				e.printStackTrace();
//				System.exit(1);
//			}
//		}
//		
//		ADDRNode policy_deterministic = _manager.breakTiesInBDD(policy, _mdp.get_actionVars(), false);
//		if( policy_deterministic.equals(_manager.DD_ZERO ) ){
//			try{
//				throw new Exception("policy not initialized properly");
//			}catch( Exception e ){
//				e.printStackTrace();
//				System.exit(1);
//			}
//		}
//		
//		return policy_deterministic;
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
