/**
 * 
 */
package mdp.solve.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ArrayBlockingQueue;

import dd.DDManager.APPROX_TYPE;
import dtr.add.ADDDecisionTheoreticRegression;
import dtr.add.ADDPolicy;
import dtr.add.ADDValueFunction;

import add.ADDManager;
import add.ADDRNode;
import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDL2DD;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.viz.ElevatorDisplay;
import util.Timer;
import util.UnorderedPair;

public class SPUDDFAR implements Runnable{

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
	private boolean _FAR;
	private APPROX_TYPE apricodd_type;
	private double apricodd_epsilon;
	private boolean do_apricodd;
	
	/**
	 * @param debug 
	 * @param order 
	 * @param seed 
	 * 
	 */
	public SPUDDFAR(String domain, String instance, double epsilon,
			ArrayBlockingQueue<UnorderedPair<ADDRNode, Integer>> bq,
			DEBUG_LEVEL debug, ORDER order, final long seed,
			final boolean useDiscounting, 
			final int numStates,
			final int numRounds,
			final boolean FAR ,
			final boolean constrain_naively ,
			final boolean do_apricodd,
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type ) {
		_FAR = FAR;
		_bq = bq;
		EPSILON = epsilon;
		_cptTimer = new Timer();
		RDDL2ADD mdp = new RDDL2ADD(domain, instance, _FAR, debug, order, true, seed);
		_cptTimer.StopTimer();
		_nStates = numStates;
		_nRounds = numRounds;
		_useDiscounting = useDiscounting;
		_dtr = new ADDDecisionTheoreticRegression(mdp, seed);
		_manager = mdp.getManager();
		DISCOUNT = mdp.getDiscount();
		HORIZON = mdp.getHorizon();
		CONSTRAIN_NAIVELY = constrain_naively;
		this.do_apricodd = do_apricodd;
		this.apricodd_epsilon = apricodd_epsilon;
		this.apricodd_type = apricodd_type;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
//	@Override
	public void run() {
		
		ADDRNode _valueDD = _manager.DD_ZERO;
		ADDPolicy _policy = null;
		
		int iter = 1;
		boolean done = false;
		boolean lastiter = false;

		_solutionTimer = new Timer();
		double prev_error = Double.NEGATIVE_INFINITY;
		List<Long> size_change = new ArrayList<Long>();
		
		while( !done ) {
			_solutionTimer.ResumeTimer();
			//			_manager.addPermenant(_valueDD);
			UnorderedPair<ADDValueFunction, ADDPolicy> newValueDD 
				= _dtr.regress(_valueDD, _FAR, false, lastiter, CONSTRAIN_NAIVELY, size_change,
						do_apricodd, apricodd_epsilon, apricodd_type );
			double error =  _dtr.getBellmanError(newValueDD._o1.getValueFn(), 
					_valueDD );
			_solutionTimer.PauseTimer();
			System.out.println( "iter = " + iter + " BE = " + error + " time = " + 
					_solutionTimer.GetElapsedTimeInMinutes() + " size of value"
					+ _manager.countNodes(newValueDD._o1.getValueFn()) + 
					( lastiter ? ( "size of policy " + 
					_manager.countNodes( _FAR ? newValueDD._o2._bddPolicy :
						newValueDD._o2._addPolicy) ) : "" ) );
			System.out.println( "Size of change " + size_change );
			size_change.clear();
//			_manager.cacheSummary();
			
			if( !do_apricodd && prev_error != Double.NEGATIVE_INFINITY
					&& error > prev_error ){
				try{
					throw new Exception("BE increased here");
				}catch( Exception e ){
					e.printStackTrace();
					System.exit(1);
				}
			}
			
			prev_error = error;

			if( lastiter ) {
				_policy = newValueDD._o2;
				done = true;
			}

			if( _dtr.terminate( error, iter, EPSILON, HORIZON ) ){//|| iter == HORIZON-1 ){
				lastiter = true;
			}
			
			++iter;
//			_manager.removePermenant(_valueDD);
			_valueDD = newValueDD._o1.getValueFn();
//			_manager.showGraph( _valueDD, _policy );
		}

		try{
			_policy.executePolicy(_nRounds, _nStates, _useDiscounting, 
					HORIZON, DISCOUNT, null  ).printStats();
		}catch( Exception e ){
			e.printStackTrace();
		}
		
		System.out.println("Solution time: " + _solutionTimer.GetElapsedTimeInMinutes() );
		System.out.println("CPT time: " + _cptTimer.GetTimeSoFarAndResetInMinutes() );
		System.out.println("Final BE = " + prev_error );
		System.out.println("Size of value fn. = " + _manager.countNodes(_valueDD) );
		System.out.println("Size of policy = " + 
				_manager.countNodes( _FAR ? _policy._bddPolicy : _policy._addPolicy ) );
		System.out.println( "No. of leaves = " + _manager.countLeaves(_valueDD) );
//		_manager.showGraph( _valueDD,_FAR ? _policy._bddPolicy : _policy._addPolicy );
	}
	
	public static void main(String[] args) throws InterruptedException {
		Runnable worker = new SPUDDFAR(args[0], args[1], Double.parseDouble(args[2]), null, 
				DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, Long.parseLong(args[3]), 
				Boolean.parseBoolean( args[4] ), Integer.parseInt(args[5]), 
				Integer.parseInt(args[6]) , Boolean.parseBoolean(args[7]),
				Boolean.parseBoolean( args[8] ), Boolean.parseBoolean( args[9] ),
				Double.parseDouble( args[10] ),  APPROX_TYPE.valueOf( args[11] ) );
		Thread t = new Thread( worker );
		t.start();
		t.join();
	}
}
