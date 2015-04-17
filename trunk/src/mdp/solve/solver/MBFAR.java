package mdp.solve.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import util.Timer;
import util.UnorderedPair;
import add.ADDManager;
import add.ADDRNode;
import dd.DDManager.APPROX_TYPE;
import dd.DDManager.DDOper;
import dtr.add.ADDDecisionTheoreticRegression;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_VALUE;
import dtr.add.ADDPolicy;
import dtr.add.ADDValueFunction;

public class MBFAR implements Runnable{

	private static boolean DO_APRICODD = false;
	private static  double APRICODD_EPSILON = 0;
	private static  APPROX_TYPE APRICODD_TYPE = null;
	private boolean CONSTRAIN_NAIVELY = false;
	private static double	EPSILON	= 0;
	private ArrayBlockingQueue<UnorderedPair<ADDRNode, Integer>> _bq = null;
	private ADDDecisionTheoreticRegression _dtr;
	private ADDManager _manager;
	private Timer _cptTimer;
	private Timer _solutionTimer;
	private int HORIZON;
	private double DISCOUNT;
	private int _nStates;
	private int _nRounds;
	private boolean _useDiscounting;
	private long BIGDD;
	private ADDRNode _valueDD;
	private ADDPolicy _policy;
	private RDDL2ADD _mdp;
	
	private boolean _stop = false;
	
	/**
	 * @param debug 
	 * @param order 
	 * @param seed 
	 * @param do_apricodd 
	 * @param apricodd_epsilon 
	 * @param apricodd_type 
	 * 
	 */
	public MBFAR(String domain, String instance, double epsilon,
			ArrayBlockingQueue<UnorderedPair<ADDRNode, Integer>> bq,
			DEBUG_LEVEL debug, ORDER order, final long seed,
			final boolean useDiscounting, 
			final int numStates,
			final int numRounds,
			final boolean constrain_naively,
			final long bigdd, 
			final boolean do_apricodd, 
			final double apricodd_epsilon, 
			final APPROX_TYPE apricodd_type  ,
			final INITIAL_VALUE init_value ) {
		_bq = bq;
		BIGDD = bigdd;
		EPSILON = epsilon;
		_cptTimer = new Timer();
		_mdp = new RDDL2ADD(domain, instance, true, debug, order, true, seed);
		_cptTimer.StopTimer();
		_nStates = numStates;
		_nRounds = numRounds;
		_useDiscounting = useDiscounting;
		_dtr = new ADDDecisionTheoreticRegression( _mdp, seed);
		_manager = _mdp.getManager();
		DISCOUNT = _mdp.getDiscount();
		HORIZON = _mdp.getHorizon();
		CONSTRAIN_NAIVELY = constrain_naively;
		DO_APRICODD = do_apricodd;
		APRICODD_EPSILON = apricodd_epsilon;
		APRICODD_TYPE = apricodd_type;
		
		_valueDD = ( init_value.equals( INITIAL_VALUE.ZERO ) ?
				_manager.DD_ZERO : _mdp.getVMax(-1,-1) );
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
//	@Override
	public void run() {
		
		_valueDD = _manager.DD_ZERO;
		_policy = null;
		
		int iter = 1;
		boolean done = false;
		boolean lastiter = false;

		_solutionTimer = new Timer();
		double prev_error = Double.NEGATIVE_INFINITY;
		List<Long> size_change = new ArrayList<Long>();
		
		while( !done && !_stop ) {
			_solutionTimer.ResumeTimer();
			//			_manager.addPermenant(_valueDD);
			UnorderedPair<ADDValueFunction, ADDPolicy> newValueDD 
				= _dtr.regressMBFAR(_valueDD,
						lastiter, CONSTRAIN_NAIVELY, BIGDD,
						size_change ,
						DO_APRICODD,
						APRICODD_EPSILON,
						APRICODD_TYPE ); 
			double error =  _dtr.getBellmanError(newValueDD._o1.getValueFn(), 
					_valueDD );
			_solutionTimer.PauseTimer();
			System.out.println( "iter = " + iter + " BE = " + error + " time = " + 
					_solutionTimer.GetElapsedTimeInMinutes() + " size of value"
					+ _manager.countNodes(newValueDD._o1.getValueFn()) + 
					( lastiter ? ( "size of policy " + 
					_manager.countNodes( newValueDD._o2._bddPolicy ) ) : "" ) );
			System.out.println("size change " + size_change );
			size_change.clear();
//			_manager.cacheSummary();
			
//			if( prev_error != Double.NEGATIVE_INFINITY
//					&& error > prev_error ){
//				try{
//					throw new Exception("BE increased here");
//				}catch( Exception e ){
//					e.printStackTrace();
//					System.exit(1);
//				}
//			}
			
			prev_error = error;

			if( lastiter ) {
				_policy = newValueDD._o2;
				done = true;
			}

			if( _dtr.terminate( error, iter, EPSILON, HORIZON ) ){
				lastiter = true;
			}
			
			++iter;
//			_manager.removePermenant(_valueDD);
			_valueDD = newValueDD._o1.getValueFn();
//			_manager.showGraph( _valueDD );
		}

//		_manager.showGraph( _valueDD, _policy._bddPolicy );
//		try {
//			System.in.read();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		System.out.println("Solution time: " + _solutionTimer.GetElapsedTimeInMinutes() );
		System.out.println("CPT time: " + _cptTimer.GetTimeSoFarAndResetInMinutes() );
		System.out.println("Final BE = " + prev_error );
		System.out.println("Size of value fn. = " + _manager.countNodes(_valueDD) );
		System.out.println("Size of policy = " + 
				_manager.countNodes( _policy._bddPolicy ) );
		
	}
	
	public void stop(){
		System.out.println("stopping");
		_stop = true;
	}
	
	public static void main(String[] args) throws InterruptedException {
		
		System.out.println( Arrays.toString(args) );
		
		final int nStates = Integer.parseInt(args[5]);
		final int nRounds = Integer.parseInt(args[6]);
		final boolean useDisc = Boolean.parseBoolean( args[4] );
		
		final long seed = Long.parseLong(args[3]);
		final Random topLevel = new Random( seed );
		
		final MBFAR worker = new MBFAR(args[0], args[1], Double.parseDouble(args[2]), null, 
				DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, topLevel.nextLong(), 
				useDisc , nStates , 
				nRounds, Boolean.parseBoolean(args[7]), Long.parseLong( args[8] ), 
				Boolean.parseBoolean( args[9] ),
				Double.parseDouble( args[10] ),  APPROX_TYPE.valueOf( args[11] ) ,
				INITIAL_VALUE.valueOf( args[12] )  );
		Thread t = new Thread( worker );
		System.out.println("Timeout = " + args[13] ); 
		t.start();
		t.join( (long) ( Double.parseDouble( args[13] ) * 60 * 1000 ) );
		worker.stop();
		
		final ADDPolicy policy = worker.getPolicy();
		
		INITIAL_STATE_CONF thing1 = ( args.length == 14 ) ? null : INITIAL_STATE_CONF.valueOf( args[14] );
		Double thing2 = ( args.length == 14 ) ? null : Double.parseDouble( args[15] );
		
		final ADDRNode init_state = worker.getInitialStateADD( thing1 , thing2 );
	
		try{
			policy.executePolicy( nRounds, nStates, useDisc, 
					worker.getHorizon(), worker.getDiscount(), null, 
					init_state, 
					new Random( topLevel.nextLong() ) ,
					new Random( topLevel.nextLong() ) ,
					new Random( topLevel.nextLong() )
					).printStats();
		}catch( Exception e ){
			e.printStackTrace();
		}
		t.interrupt();
		
	}
	
	private ADDRNode getInitialStateADD(
			final INITIAL_STATE_CONF init_conf, 
			final Double init_prob) {
		Objects.requireNonNull(init_conf);
		
		final ADDRNode ret = _dtr.getIIDInitialStates(init_conf, init_prob);
		final ADDRNode ret_neg_inf = _dtr.convertToNegInfDD( ret )[ 0 ];

		final ADDRNode value_init_state = _manager.apply( _valueDD, ret_neg_inf, DDOper.ARITH_PLUS );
		System.out.println("value of state : " + _manager.enumeratePathsADD(value_init_state) );
		return ret;
	}

	public ADDRNode getValueDD() {
		return _valueDD;
	}
	
	public ADDManager getManager() {
		return _manager;
	}
	
	public ADDPolicy getPolicy() {
		return _policy;
	}
	
	public ADDDecisionTheoreticRegression getDTR() {
		return _dtr;
	}
	
	
	private int getHorizon() {
		return HORIZON;
	}
	
	private double getDiscount(){
		return DISCOUNT;
	}
	
}
