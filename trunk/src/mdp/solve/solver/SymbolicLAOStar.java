package mdp.solve.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeoutException;

import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import util.Timer;
import util.UnorderedPair;
import add.ADDINode;
import add.ADDLeaf;
import add.ADDManager;
import add.ADDNode;
import add.ADDRNode;
import dd.DDManager.APPROX_TYPE;
import dd.DDManager.DDOper;
import dd.DDManager.DDQuantify;
import dtr.SymbolicPolicy;
import dtr.SymbolicValueFunction;
import dtr.add.ADDDecisionTheoreticRegression;
import dtr.add.ADDDecisionTheoreticRegression.BACKUP_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import dtr.add.ADDPolicy;
import dtr.add.ADDValueFunction;

public class SymbolicLAOStar implements Runnable {

	private boolean CONSTRAIN_NAIVELY = false;
	private static double	EPSILON	= 0;
	private ADDDecisionTheoreticRegression _dtr;
	private ADDManager _manager;
	private Timer _cptTimer;
	private Timer _solutionTimer;
	private Timer _DPTimer = null;
	private Timer _reachTimer = null;
	private int HORIZON;
	private double DISCOUNT;
	private int _nStates;
	private int _nRounds;
	private boolean _useDiscounting;
	private boolean _FAR;
	private APPROX_TYPE apricodd_type;
	private double apricodd_epsilon;
	private boolean do_apricodd;
	private BACKUP_TYPE heuristic_type;
	private double time_heuristic_mins;
	private int steps_heuristic;
	private int steps_dp;
	private long MB;
	private RDDL2ADD _mdp;
	private INITIAL_STATE_CONF init_state_conf;
	private double init_state_prob;
	private BACKUP_TYPE dp_type;
	private long seed;
	private ADDRNode __initial_state_add;
	private ADDRNode policyDD;
	
	public SymbolicLAOStar(String domain, String instance, double epsilon,
			DEBUG_LEVEL debug, ORDER order, final long seed,
			final boolean useDiscounting, 
			final int numStates,
			final int numRounds,
			final boolean FAR ,
			final boolean constrain_naively ,
			final boolean do_apricodd,
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type,
			final BACKUP_TYPE heuristic_type,
			final double time_heuristic_mins, 
			final int steps_heuristic, 
			final int steps_dp,  //number of steps of VI within solution graph
			final long MB ,
			final INITIAL_STATE_CONF init_state_conf ,
			final double init_state_prob,
			final BACKUP_TYPE dp_type ) {
		_FAR = FAR;
		EPSILON = epsilon;
		_cptTimer = new Timer();
		_mdp = new RDDL2ADD(domain, instance, _FAR, debug, order, true, seed);
		_cptTimer.StopTimer();
		_nStates = numStates;
		_nRounds = numRounds;
		_useDiscounting = useDiscounting;
		_dtr = new ADDDecisionTheoreticRegression(_mdp, seed);
		_manager = _mdp.getManager();
		DISCOUNT = _mdp.getDiscount();
		HORIZON = _mdp.getHorizon();
		CONSTRAIN_NAIVELY = constrain_naively;
		this.do_apricodd = do_apricodd;
		this.apricodd_epsilon = apricodd_epsilon;
		this.apricodd_type = apricodd_type;
		this.heuristic_type = heuristic_type;
		this.time_heuristic_mins = time_heuristic_mins;
		this.steps_heuristic = steps_heuristic;
		this.steps_dp = steps_dp;
		this.MB = MB;
		this.init_state_conf = init_state_conf;
		this.init_state_prob = init_state_prob;
		this.dp_type = dp_type;
		this.seed = seed;
	}
	
	//policy expansion
	public UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, ADDRNode>> policyExpansion( 
			final ADDRNode policy,
			final ADDRNode initial_states,
			final ADDRNode solution_graph ){
		if( _reachTimer == null ){
			_reachTimer = new Timer();
		}else{
			_reachTimer.ResumeTimer();
		}
		ADDRNode expanded = _manager.DD_ZERO;
		ADDRNode fringe = _manager.DD_ZERO;
		ADDRNode from = initial_states;
		ADDRNode solution_graph_ret = solution_graph;
		int iter = 0;
		do{
			final ADDRNode to = _dtr.BDDImagePolicy(from, 
					_FAR, DDQuantify.EXISTENTIAL, policy);
			fringe = _manager.BDDUnion(fringe, 
					_manager.BDDSubtract(to, solution_graph) );
			expanded = _manager.BDDUnion(expanded, from );
			from = _manager.BDDIntersection( to, solution_graph );
			from = _manager.BDDSubtract(from , expanded );
			System.out.println( "reach iter " +  (iter++) );
			System.out.println( "Nodes " + 
					_manager.countNodes( expanded, fringe, solution_graph ) );
		}while( from != _manager.DD_ZERO );
		expanded = _manager.BDDUnion(expanded, fringe);
		solution_graph_ret = _manager.BDDUnion(solution_graph, fringe);
		_reachTimer.PauseTimer();
		return new UnorderedPair< ADDRNode, UnorderedPair< ADDRNode, ADDRNode > >
			( expanded, new UnorderedPair<ADDRNode, ADDRNode>(fringe, solution_graph_ret ) );
	}
	
	public void run() {
		
		ADDRNode valueDD = _dtr.computeLAOHeuristic( steps_heuristic, heuristic_type, CONSTRAIN_NAIVELY,
					do_apricodd, apricodd_epsilon, apricodd_type, MB, time_heuristic_mins );
//		_manager.showGraph( valueDD );
		
		policyDD = _dtr.getNoOpPolicy( _mdp.get_actionVars(), _manager );
		ADDRNode solution_graph = _manager.DD_ZERO;
		__initial_state_add = _dtr.getIIDInitialStates( init_state_conf ,
				init_state_prob );
//		_manager.showGraph(initial_state );
			
		int overall_iter = 1;
		boolean done = false;
		double residual = Double.MAX_VALUE;
		ADDRNode expanded_states = null;
		
		_solutionTimer = new Timer();
		
		while( !done ) {

			UnorderedPair< ADDRNode, UnorderedPair< ADDRNode, ADDRNode > >
				policy_expansion = policyExpansion( policyDD, __initial_state_add, solution_graph );
			
			expanded_states = policy_expansion._o1;
			final ADDRNode fringe = policy_expansion._o2._o1;
			solution_graph = policy_expansion._o2._o2;
//			_manager.showGraph( solution_graph );
			
			
			UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> 
				dp_result = do_DP( expanded_states, valueDD, policyDD );
			
			valueDD = dp_result._o1;
			policyDD = dp_result._o2._o1;
			residual = dp_result._o2._o2;
//			_manager.showGraph( valueDD , policyDD ); 
			
			if( fringe == _manager.DD_ZERO && residual <= EPSILON ){
				done = true;
			}
			
			System.out.println( "Overall iter = " + (overall_iter++) );
		}

		System.out.println("Solution time: " + _solutionTimer.GetTimeSoFarAndResetInMinutes() );
		System.out.println("CPT time: " + _cptTimer.GetTimeSoFarAndResetInMinutes() );
		System.out.println("Reach time: " + _reachTimer.GetTimeSoFarAndResetInMinutes() );
		System.out.println("DP time: " + _DPTimer.GetTimeSoFarAndResetInMinutes() );
		
		System.out.println("Final BE = " + residual );
		System.out.println("Size of value fn. = " + _manager.countNodes( valueDD ) );
		System.out.println("Size of policy = " + 
				_manager.countNodes( policyDD ) );
		System.out.println( "No. of leaves = " + _manager.countLeaves( valueDD ) );
		System.out.println( "Size of Solution graph = " + _manager.countNodes( solution_graph ) );
		System.out.println( "Size of expanded = " + _manager.countNodes( expanded_states ) );
//		_manager.showGraph( valueDD, policyDD );
	}
	
	private UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> do_DP(
			final ADDRNode states, 
			final ADDRNode current_value,
			final ADDRNode current_policy ) {
		if( _DPTimer == null ){
			_DPTimer = new Timer();
		}else{
			_DPTimer.ResumeTimer();
		}
		
		final ADDRNode one_step = _dtr.BDDImage(states, _FAR, DDQuantify.EXISTENTIAL );
		double residual = Double.MAX_VALUE;
		
		ADDRNode ret_value = current_value;
		ADDRNode ret_policy = current_policy;
		
		int iter = 0;
		boolean done = false;
		
		do{

			UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> one_backup = 
					_dtr.backup(ret_value, current_policy, one_step, states, 
					this.dp_type, this.do_apricodd, this.apricodd_epsilon, 
					this.apricodd_type, done, this.MB );
			
			residual = getResidual( one_backup._o1, ret_value, states );
			ret_value = one_backup._o1;
			ret_policy = one_backup._o2._o1;
					
			System.out.println("DP Iter = " + iter + " BE = " + residual + " " 
					+ _manager.countNodes(ret_value, ret_policy ) );

			if( done ){
				break;
			}
			if( residual <= EPSILON || ( ++iter > steps_dp && steps_dp != -1 ) ){
				done = true;
			}
		}while( true );
//		_manager.showGraph( ret_policy );
		ret_policy = _manager.breakTiesInBDD( ret_policy, _mdp.get_actionVars(), false);
		_DPTimer.PauseTimer();
		return new UnorderedPair<ADDRNode, UnorderedPair<ADDRNode,Double>>(
				ret_value, new UnorderedPair<ADDRNode,Double>( ret_policy, residual ) ); 
	}

	private double getResidual(
			final ADDRNode new_value, 
			final ADDRNode old_value, 
			final ADDRNode states) {
		return _dtr.getBellmanError(
				_manager.BDDIntersection(new_value, states),
				_manager.BDDIntersection(old_value, states) );
	}

	public static void main(String[] args) throws InterruptedException {
		
		final boolean use_disc = Boolean.parseBoolean( args[4] );
		final int nStates = Integer.parseInt( args[5] );
		final int nRounds = Integer.parseInt( args[6] );
		
		SymbolicLAOStar worker = new SymbolicLAOStar(
				args[0], args[1],
				Double.parseDouble( args[2] ), 
				DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, 
				Long.parseLong( args[3] ), use_disc, 
				nStates, nRounds, Boolean.parseBoolean( args[7] ),
				Boolean.parseBoolean( args[8] ), 
				Boolean.parseBoolean(args[9] ), 
				Double.parseDouble( args[10] ), 
				APPROX_TYPE.valueOf( args[11] ),
				BACKUP_TYPE.valueOf( args[12] ), 
				Double.parseDouble( args[13] ),
				Integer.parseInt( args[14] ),
				Integer.parseInt( args[15] ),
				Long.parseLong( args[16] ),
				INITIAL_STATE_CONF.valueOf( args[17] ),
				Double.valueOf( args[18] ),
				BACKUP_TYPE.valueOf( args[ 19 ] ) );
//		Runnable worker = new SPUDDFAR(args[0], args[1], Double.parseDouble(args[2]), null, 
//				DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, Long.parseLong(args[3]), 
//				Boolean.parseBoolean( args[4] ), Integer.parseInt(args[5]), 
//				Integer.parseInt(args[6]) , Boolean.parseBoolean(args[7]),
//				Boolean.parseBoolean( args[8] ), Boolean.parseBoolean( args[9] ),
//				Double.parseDouble( args[10] ),  APPROX_TYPE.valueOf( args[11] ) );
		Thread t = new Thread( worker );
		t.start();
		t.join( (long)(Double.parseDouble( args[20] ) * 60 * 1000 ));

		
		try{
			worker.getPolicy().executePolicy( nRounds, nStates, use_disc, 
					worker.getHorizon(), 
					worker.getDiscount(), null, worker.getInitialStateADD()  ).printStats();
		}catch( Exception e ){
			e.printStackTrace();
		}
		
	}

	private ADDRNode getInitialStateADD() {
		return __initial_state_add;
	}

	private double getDiscount() {
		return DISCOUNT;
	}

	private int getHorizon() {
		return HORIZON;
	}

	private ADDPolicy getPolicy() {
		ADDPolicy policy = new ADDPolicy(_manager, _mdp.getFactoredStateSpace(), 
				_mdp.getFactoredTransition(),
				_mdp.getFactoredReward(), seed);
		policy._bddPolicy = _FAR ? policyDD : null;
		policy._addPolicy = _FAR ? null : policyDD;
		return policy;
	}
	
	
	
}