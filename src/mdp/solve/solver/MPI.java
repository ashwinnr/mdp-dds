package mdp.solve.solver;

import java.util.ArrayList;
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
import dtr.add.ADDDecisionTheoreticRegression;
import dtr.add.ADDPolicy;
import dtr.add.ADDValueFunction;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;

public class MPI implements Runnable {
	
	private static double	EPSILON	= 0;
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
	private boolean _FAR;
	private int _bellman_backups;
	private int _eval_backups;
	private int eval_backups;
	private RDDL2ADD _mdp;
	private boolean CONSTRAIN_NAIVELY;
	private boolean DO_APRICODD;
	private double APRICODD_EPSILON;
	private APPROX_TYPE APRICODD_APRROX;
	private boolean _stop = false;
	private ADDPolicy _policy = null;
	
	public void stop(){
		System.out.println("Stopping stop()!");
		_stop = true;
//		synchronized( this ){
//			this.notifyAll();
//		}
	}
	
	public MPI(String domain, String instance, double epsilon,
			ArrayBlockingQueue<UnorderedPair<ADDRNode, Integer>> bq,
			DEBUG_LEVEL debug, ORDER order, final long seed,
			final boolean useDiscounting, 
			final int numStates,
			final int numRounds,
			final int evalSteps,
			final boolean FAR ,
			final boolean constrain_naively,
			final boolean do_apricodd,
			final double apricodd_epsilon,
			final APPROX_TYPE approx_type ) {
		_FAR = FAR;
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
		DO_APRICODD = do_apricodd;
		APRICODD_EPSILON = apricodd_epsilon;
		APRICODD_APRROX = approx_type;
	}
	
	public void run() {
		
		ADDRNode _valueDD = _manager.DD_ZERO;
		ADDRNode _policyDD = _manager.DD_ZERO;
		
		int iter = 1;

		_solutionTimer = new Timer();
		_improvTimer = new Timer();
		_evalTimer = new Timer();
		double prev_error = Double.NEGATIVE_INFINITY;
		boolean lastiter = false;
		final ArrayList< Long > size_change = new ArrayList<Long>(); 

		while( !_stop ) {
			_solutionTimer.ResumeTimer();
//			_manager.addPermenant(_valueDD);
			_improvTimer.ResumeTimer();
			final boolean makePolicy = ( _evalSteps > 0 || lastiter ) ? true : false;
			UnorderedPair<ADDValueFunction, ADDPolicy> newValueDD 
				= _dtr.regress(_valueDD, _FAR, false, 
						makePolicy  , CONSTRAIN_NAIVELY, size_change,
						DO_APRICODD, APRICODD_EPSILON, APRICODD_APRROX ); 
			if( _stop ){
				break;
			}
			
			double error =  _dtr.getBellmanError(newValueDD._o1.getValueFn(), 
					_valueDD );
			_solutionTimer.PauseTimer();
			_improvTimer.PauseTimer();
			System.out.println( "MPI iter = " + iter + " BE = " + error + " time = " + 
					_solutionTimer.GetElapsedTimeInMinutes() + " size of value "
					+ _manager.countNodes(newValueDD._o1.getValueFn()) 
					+ ( ( makePolicy ? ( " size of policy " + 
					_manager.countNodes(newValueDD._o2._bddPolicy) ) : "" ) ) );
			System.out.println( "Size change " + size_change );
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
			
			prev_error = error;

//			_manager.removePermenant(_valueDD);
			_valueDD = newValueDD._o1.getValueFn();
			if( makePolicy ){
				_policyDD = _FAR ? newValueDD._o2._bddPolicy : 
					newValueDD._o2._addPolicy;
//				break ties in policy
				_policyDD = _manager.breakTiesInBDD(_policyDD, _mdp.getFactoredActionSpace().getActionVariables(),
						false );
				_policy  = new ADDPolicy(_manager, _mdp.getFactoredStateSpace(), 
						_mdp.getFactoredTransition(), _mdp.getFactoredReward(), 
						_mdp.getFactoredActionSpace() );
				_policy._bddPolicy = _FAR ? _policyDD : null;
				_policy._addPolicy = null;
			}
			
			if( makePolicy && !_stop ){
				_solutionTimer.ResumeTimer();
				_evalTimer.ResumeTimer();
				
				//policy eval
				UnorderedPair<ADDRNode, Integer> evaluation 
					= _dtr.evaluatePolicy(_valueDD, _policyDD, _evalSteps, 
							EPSILON, _FAR, CONSTRAIN_NAIVELY, size_change ,
							DO_APRICODD, APRICODD_EPSILON, APRICODD_APRROX );
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
			if( _dtr.terminate(error, iter, EPSILON, HORIZON) && !lastiter ){//)*(1-DISCOUNT)/(2*DISCOUNT) ){
				lastiter = true;
			}
		}
		
//		_policy.executePolicy(_nRounds, _nStates, _useDiscounting, HORIZON, DISCOUNT ).printStats();
		
		System.out.println("Solution time: " + _solutionTimer.GetElapsedTimeInMinutes() );
		System.out.println("CPT time: " + _cptTimer.GetElapsedTimeInMinutes() );
		System.out.println("Eval time: " + _evalTimer.GetElapsedTimeInMinutes() );
		System.out.println("Improve time: " + _improvTimer.GetElapsedTimeInMinutes() );
		System.out.println("Final BE = " + prev_error );
		System.out.println("Size of value fn. = " + _manager.countNodes(_valueDD) );
		System.out.println("Size of policy = " + 
				_manager.countNodes( _FAR ? _policy._bddPolicy : _policy._addPolicy ) );
		System.out.println("Eval backups = " + eval_backups );
		System.out.println("Bellman backups = " + iter );
//		_manager.showGraph( _valueDD,_FAR ? _policy._bddPolicy : _policy._addPolicy );
		stop();
	}
	
	public static void main(String[] args) throws InterruptedException {
		
		final long seed = Long.parseLong(args[3]);
		final Random topLevel = new Random( seed );
		int nStates = Integer.parseInt(args[5]);
		int nRounds = Integer.parseInt(args[6]);
		
		MPI worker = new MPI(args[0], args[1], Double.parseDouble(args[2]), 
				null, DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, topLevel.nextLong(), 
				Boolean.parseBoolean(args[4]), nStates, 
				nRounds, Integer.parseInt(args[7]),
				Boolean.parseBoolean(args[8] ), Boolean.parseBoolean(args[9]) ,
				Boolean.parseBoolean(args[10]), Double.parseDouble(args[11]),
				APPROX_TYPE.valueOf(args[12]) );
		Thread t = new Thread( worker );
		t.start();
		t.join( (long) (Double.parseDouble( args[13] ) * 60 * 1000) );
		
		worker.stop();
		System.out.println("waiting for thread to terminate");
		t.join();
		
		final ADDPolicy policy = worker.getPolicy();
		
		INITIAL_STATE_CONF thing1 = ( args.length == 14 ) ? null : INITIAL_STATE_CONF.valueOf( args[14] );
		Double thing2 = ( args.length == 14 ) ? null : Double.parseDouble( args[15] );
		
		final ADDRNode init_state = worker.getInitialStateADD( thing1 , thing2 );
		
		try{
			policy.executePolicy( nRounds, nStates, Boolean.parseBoolean(args[8] ), 
					worker.getHorizon(), worker.getDiscount(), null , init_state, 
					new Random( topLevel.nextLong() ) ,
					new Random( topLevel.nextLong() ) ,
					new Random( topLevel.nextLong() ) ).printStats();
		}catch( Exception e ){
			e.printStackTrace();
		}
		
	}
	
	private ADDRNode getInitialStateADD(INITIAL_STATE_CONF thing1, Double thing2) {
		return _dtr.getIIDInitialStates(thing1, thing2 );
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
