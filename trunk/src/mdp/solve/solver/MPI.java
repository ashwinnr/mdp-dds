package mdp.solve.solver;

import java.util.concurrent.ArrayBlockingQueue;

import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;

import util.Timer;
import util.UnorderedPair;
import add.ADDManager;
import add.ADDRNode;
import dtr.ADDDecisionTheoreticRegression;
import dtr.ADDPolicy;
import dtr.ADDValueFunction;

public class MPI implements Runnable {
	
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
	private int _evalSteps;
	private boolean _FAR;
	
	public MPI(String domain, String instance, double epsilon,
			ArrayBlockingQueue<UnorderedPair<ADDRNode, Integer>> bq,
			DEBUG_LEVEL debug, ORDER order, final long seed,
			final boolean useDiscounting, 
			final int numStates,
			final int numRounds,
			final int evalSteps,
			final boolean FAR ) {
		_FAR = FAR;
		_evalSteps = evalSteps;
		_bq = bq;
		EPSILON = epsilon;
		_cptTimer = new Timer();
		RDDL2ADD mdp = new RDDL2ADD(domain, instance, true, debug, order, true, seed);
		_cptTimer.StopTimer();
		_nStates = numStates;
		_nRounds = numRounds;
		_useDiscounting = useDiscounting;
		_dtr = new ADDDecisionTheoreticRegression(mdp, seed);
		_manager = mdp.getManager();
		DISCOUNT = mdp.getDiscount();
		HORIZON = mdp.getHorizon();
	}
	
	public void run() {
		
		ADDRNode _valueDD = _manager.DD_ZERO;
		ADDRNode _policyDD = _manager.DD_ZERO;
		
		ADDPolicy _policy = null;
		
		int iter = 1;

		_solutionTimer = new Timer();
		double prev_error = Double.NEGATIVE_INFINITY;

		while( true ) {
			_solutionTimer.ResumeTimer();
			_manager.addPermenant(_valueDD);
			UnorderedPair<ADDValueFunction, ADDPolicy> newValueDD 
				= _dtr.regress(_valueDD, _FAR, false, true ); 
			double error =  _dtr.getBellmanError(newValueDD._o1.getValueFn(), 
					_valueDD );
			_solutionTimer.PauseTimer();
			System.out.println( "MPI iter = " + iter + " BE = " + error + " time = " + 
					_solutionTimer.GetElapsedTimeInMinutes() + " size of value "
					+ _manager.countNodes(newValueDD._o1.getValueFn()) 
					+ " size of policy " + 
					_manager.countNodes(newValueDD._o2._bddPolicy) );
			
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

			if( error < EPSILON ){//)*(1-DISCOUNT)/(2*DISCOUNT) ){
				break;
			}
			
			_manager.removePermenant(_valueDD);
			_valueDD = newValueDD._o1.getValueFn();
			_policyDD = _FAR ? newValueDD._o2._bddPolicy : 
				newValueDD._o2._addPolicy;
			_policy = newValueDD._o2;
			_solutionTimer.ResumeTimer();
			//policy eval
			ADDRNode evaluated 
				= _dtr.evaluatePolicy(_valueDD, _policyDD, _evalSteps, EPSILON, _FAR);
			_solutionTimer.PauseTimer();
			_valueDD = evaluated;
			++iter;
		}
		
		
		_policy.executePolicy(_nRounds, _nStates, _useDiscounting, HORIZON, DISCOUNT ).printStats();
		
		System.out.println("Solution time: " + _solutionTimer.GetElapsedTimeInMinutes() );
		System.out.println("CPT time: " + _cptTimer.GetElapsedTimeInMinutes() );
		System.out.println("Final BE = " + prev_error );
		System.out.println("Size of value fn. = " + _manager.countNodes(_valueDD) );
		System.out.println("Size of policy = " + 
				_manager.countNodes( _FAR ? _policy._bddPolicy : _policy._addPolicy ) );
//		_manager.showGraph( _valueDD,_FAR ? _policy._bddPolicy : _policy._addPolicy );
	}
	
	public static void main(String[] args) throws InterruptedException {
		Runnable worker = new MPI(args[0], args[1], Double.parseDouble(args[2]), 
				null, DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, Long.parseLong(args[3]), 
				Boolean.parseBoolean(args[4]), Integer.parseInt(args[5]), 
				Integer.parseInt(args[6]), Integer.parseInt(args[7]),
				Boolean.parseBoolean(args[8] ) );
		Thread t = new Thread( worker );
		t.start();
		t.join();
	}

}
