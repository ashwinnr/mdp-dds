
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;

import util.Timer;
import util.UnorderedPair;
import add.ADDManager;
import add.ADDRNode;
import dd.DDManager.APPROX_TYPE;
import dtr.add.ADDDecisionTheoreticRegression;
import dtr.add.ADDPolicy;
import dtr.add.ADDValueFunction;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;

public class MBMPI_Naive implements Runnable {
	
	private static double APRICODD_EPSILON;
	private static boolean DO_APRICODD;
	private static APPROX_TYPE APRICODD_TYPE;
	private static double	EPSILON	= 0;
	private boolean LIMIT_EVAL = false;
	private ADDDecisionTheoreticRegression _dtr;
	private ADDManager _manager;
	private Timer _cptTimer;
	private Timer _solutionTimer;
	private Timer _evalTimer;
	private Timer _improvTimer;
	private int HORIZON;
	private double DISCOUNT;
	private int _nStates;
	private int _nRounds;
	private boolean _useDiscounting;
	private int _evalSteps;
	private int _bellman_backups;
	private int _eval_backups;
	private int eval_backups;
	private RDDL2ADD _mdp;
	private boolean CONSTRAIN_NAIVELY;
	private long BIGDD;
	private ADDPolicy _policy;
	private boolean _stop = false;
	
	public MBMPI_Naive(String domain, String instance, double epsilon,
			DEBUG_LEVEL debug, ORDER order, final long seed,
			final boolean useDiscounting, 
			final int numStates,
			final int numRounds,
			final int evalSteps,
			final boolean constrain_naively,
			final long bigdd,
			final boolean limit_eval ,
			final boolean do_apricodd,
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ) {
		DO_APRICODD = do_apricodd;
		APRICODD_EPSILON = apricodd_epsilon;
		APRICODD_TYPE = apricodd_type;
		BIGDD = bigdd;
		LIMIT_EVAL = limit_eval;
		CONSTRAIN_NAIVELY = constrain_naively;
		_evalSteps = evalSteps;
		EPSILON = epsilon;
		_cptTimer = new Timer();
		_mdp = new RDDL2ADD(domain, instance, true, debug, order, true, seed);
		_cptTimer.StopTimer();
		_nStates = numStates;
		_nRounds = numRounds;
		_useDiscounting = useDiscounting;
		_dtr = new ADDDecisionTheoreticRegression(_mdp, seed);
		_manager = _mdp.getManager();
		DISCOUNT = _mdp.getDiscount();
		HORIZON = _mdp.getHorizon();
	}
	
	public void run() {
		
		ADDRNode _valueDD = _manager.DD_ZERO;
		ADDRNode _policyDD = _manager.DD_ZERO;
		
		_policy = null;
		
		int iter = 1;

		_solutionTimer = new Timer();
		_improvTimer = new Timer();
		_evalTimer = new Timer();
		double prev_error = Double.NEGATIVE_INFINITY;
		boolean lastiter = false;
		List<Long> size_change = new ArrayList<Long>();
		
		while( !_stop ) {
			_solutionTimer.ResumeTimer();
//			_manager.addPermenant(_valueDD);
			_improvTimer.ResumeTimer();
			final boolean makePolicy = ( _evalSteps > 0 || lastiter ) ? true : false;
			UnorderedPair<ADDValueFunction, ADDPolicy> newValueDD 
				= _dtr.regressMBFAR( _valueDD, makePolicy, CONSTRAIN_NAIVELY, BIGDD,
						size_change, DO_APRICODD, APRICODD_EPSILON, 
						APRICODD_TYPE ); 
			double error =  _dtr.getBellmanError(newValueDD._o1.getValueFn(), 
					_valueDD );
			_solutionTimer.PauseTimer();
			_improvTimer.PauseTimer();
			System.out.println( "MPI iter = " + iter + " BE = " + error + " time = " + 
					_solutionTimer.GetElapsedTimeInMinutes() + " size of value "
					+ _manager.countNodes(newValueDD._o1.getValueFn()) 
					+ ( ( makePolicy ? ( " size of policy " + 
					_manager.countNodes(newValueDD._o2._bddPolicy) ) : "" ) ) );
			System.out.println( "size change : " + size_change );
			size_change.clear();
			
//			if( prev_error != Double.NEGATIVE_INFINITY
//					&& error > prev_error ){
//				try{
//					throw new Exception("BE increased here");
//				}catch( Exception e ){
//					e.printStackTrace();
//					System.exit(1);
//				}
//			}
			_manager.flushCaches();
			prev_error = error;

//			_manager.removePermenant(_valueDD);
			_valueDD = newValueDD._o1.getValueFn();
			if( makePolicy ){
				_policyDD = newValueDD._o2._bddPolicy;
				_policy = newValueDD._o2;
			}
			
			if( makePolicy && !_stop ){
				_solutionTimer.ResumeTimer();
				_evalTimer.ResumeTimer();
				//	break ties in policy
				_policyDD = _manager.breakTiesInBDD(_policyDD, _mdp.getFactoredActionSpace().getActionVariables(),
						false );
				//policy eval
				UnorderedPair<ADDRNode, Integer> evaluation 
					= _dtr.evaluatePolicyMBFAR(_valueDD, _policyDD, _evalSteps, 
							EPSILON, CONSTRAIN_NAIVELY, false, BIGDD, LIMIT_EVAL,
							DO_APRICODD, APRICODD_EPSILON, APRICODD_TYPE );
				ADDRNode evaluated = evaluation._o1;
				eval_backups += evaluation._o2;
				_solutionTimer.PauseTimer();
				_evalTimer.PauseTimer();
				_valueDD = evaluated;
			}
			
			++iter;
			if( lastiter ){
				break;
			}
			if( error < EPSILON && !lastiter ){//)*(1-DISCOUNT)/(2*DISCOUNT) ){
				lastiter = true;
			}
			_manager.flushCaches();
		}
		
//		_policy.executePolicy(_nRounds, _nStates, _useDiscounting, HORIZON, DISCOUNT ).printStats();
		
		System.out.println("Solution time: " + _solutionTimer.GetElapsedTimeInMinutes() );
		System.out.println("CPT time: " + _cptTimer.GetElapsedTimeInMinutes() );
		System.out.println("Eval time: " + _evalTimer.GetElapsedTimeInMinutes() );
		System.out.println("Improve time: " + _improvTimer.GetElapsedTimeInMinutes() );
		System.out.println("Final BE = " + prev_error );
		System.out.println("Size of value fn. = " + _manager.countNodes(_valueDD) );
		System.out.println("Size of policy = " + 
				_manager.countNodes( _policy._bddPolicy ) );
		System.out.println("Eval backups = " + eval_backups );
		System.out.println("Bellman backups = " + iter );
//		_manager.showGraph( _valueDD,_FAR ? _policy._bddPolicy : _policy._addPolicy );
	}
	
	public void stop(){
		_stop  = true;
	}
	
	public static void main(String[] args) throws InterruptedException {
		
		System.out.println( args );
		
		MBMPI_Naive worker = new MBMPI_Naive(args[0], args[1], Double.parseDouble(args[2]), 
				 DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, Long.parseLong(args[3]), 
				Boolean.parseBoolean(args[4]), Integer.parseInt(args[5]), 
				Integer.parseInt(args[6]), Integer.parseInt(args[7]),
				Boolean.parseBoolean(args[8] ), Long.parseLong(args[9]), 
				Boolean.parseBoolean( args[10]) ,
				Boolean.parseBoolean(args[11]) ,
				Double.parseDouble(args[12]) ,
				APPROX_TYPE.valueOf(args[13]) );
		Thread t = new Thread( worker );
		t.start();
		t.join( (long) (Double.parseDouble( args[14] ) * 60 * 1000) ) ;
		worker.stop();
		System.out.println("Stopping!" );
		
		final ADDPolicy policy = worker.getPolicy();
		try{
			policy.executePolicy( Integer.parseInt(args[7]), Integer.parseInt(args[6]), Boolean.parseBoolean(args[4] ), 
					worker.getHorizon(), worker.getDiscount(), null ).printStats();
		}catch( Exception e ){
			e.printStackTrace();
		}
		
	}

	private ADDPolicy getPolicy() {
		return _policy;//._bddPolicy != null ? _policy._bddPolicy : _policy._addPolicy;
	}

	private double getDiscount() {
		return DISCOUNT;
	}

	private int getHorizon() {
		return HORIZON;
	}

}
