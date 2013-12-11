package mdp.solve.solver;

import java.util.ArrayList;
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

public class SeqHindsight implements Runnable {
	
	private static double	EPSILON	= 0;
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
	private RDDL2ADD _mdp;
	private boolean CONSTRAIN_NAIVELY;
	private boolean DO_APRICODD;
	private double APRICODD_EPSILON;
	private APPROX_TYPE APRICODD_APRROX;
	
	public SeqHindsight(String domain, String instance, double epsilon,
			DEBUG_LEVEL debug, ORDER order, final long seed,
			final boolean useDiscounting, 
			final int numStates,
			final int numRounds,
			final boolean FAR ,
			final boolean constrain_naively,
			final boolean do_apricodd,
			final double apricodd_epsilon,
			final APPROX_TYPE approx_type ) {
		_FAR = FAR;
		CONSTRAIN_NAIVELY = constrain_naively;
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
		ADDPolicy _policy = null;
		
		int iter = 1;

		_solutionTimer = new Timer();
		double prev_error = Double.NEGATIVE_INFINITY;
		boolean lastiter = false;
		final ArrayList< Long > size_change = new ArrayList<Long>(); 

		while( true ) {
			_solutionTimer.ResumeTimer();
			final boolean makePolicy = ( lastiter ) ? true : false;
			UnorderedPair<ADDValueFunction, ADDPolicy> newValueDD 
				= new UnorderedPair<ADDValueFunction, ADDPolicy>(null, null);
			if( makePolicy ){
				newValueDD 
				= _dtr.regress(_valueDD, _FAR, false, 
						makePolicy  , CONSTRAIN_NAIVELY, size_change,
						DO_APRICODD, APRICODD_EPSILON, APRICODD_APRROX );
			}else{
				newValueDD._o1 = _dtr.regressHindSight(_valueDD, CONSTRAIN_NAIVELY,
						size_change, DO_APRICODD, APRICODD_EPSILON, APRICODD_APRROX );
			}
			double error =  _dtr.getBellmanError(newValueDD._o1.getValueFn(), 
					_valueDD );
			_solutionTimer.PauseTimer();
			System.out.println( "MPI iter = " + iter + " BE = " + error + " time = " + 
					_solutionTimer.GetElapsedTimeInMinutes() + " size of value "
					+ _manager.countNodes(newValueDD._o1.getValueFn()) 
					+ ( ( makePolicy ? ( " size of policy " + 
					_manager.countNodes(newValueDD._o2._bddPolicy) ) : "" ) ) );
			System.out.println( "Size change " + size_change );
			size_change.clear();
			prev_error = error;
			_valueDD = newValueDD._o1.getValueFn();
			
			if( makePolicy ){
				_policy = newValueDD._o2;
			}
			
			++iter;
			if( lastiter ){
				break;
			}
			if( _dtr.terminate(error, iter, EPSILON, HORIZON) && !lastiter ){//)*(1-DISCOUNT)/(2*DISCOUNT) ){
				lastiter = true;
			}
		}
		
		_policy.executePolicy(_nRounds, _nStates, _useDiscounting, HORIZON, DISCOUNT ).printStats();
		
		System.out.println("Solution time: " + _solutionTimer.GetElapsedTimeInMinutes() );
		System.out.println("CPT time: " + _cptTimer.GetElapsedTimeInMinutes() );
		System.out.println("Size of value fn. = " + _manager.countNodes(_valueDD) );
		System.out.println("Size of policy = " + 
				_manager.countNodes( _FAR ? _policy._bddPolicy : _policy._addPolicy ) );
		System.out.println("Bellman backups = " + iter );
//		_manager.showGraph( _valueDD,_FAR ? _policy._bddPolicy : _policy._addPolicy );
	}
	
	public static void main(String[] args) throws InterruptedException {
		Runnable worker = new SeqHindsight(args[0], args[1], Double.parseDouble(args[2]), 
				DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, Long.parseLong(args[3]), 
				Boolean.parseBoolean(args[4]), Integer.parseInt(args[5]), 
				Integer.parseInt(args[6]), 
				Boolean.parseBoolean(args[7] ), Boolean.parseBoolean(args[8]) ,
				Boolean.parseBoolean(args[9]), Double.parseDouble(args[10]),
				APPROX_TYPE.valueOf(args[11]) );
		Thread t = new Thread( worker );
		t.start();
		t.join();
	}

}