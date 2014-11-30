package mdp.solve.solver;

import java.util.ArrayList;
import java.util.Random;

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
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_VALUE;

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
	private boolean _stop = false;
	private ADDRNode _valueDD;
	private ADDPolicy _policy;
	
	public SeqHindsight(String domain, String instance, double epsilon,
			DEBUG_LEVEL debug, ORDER order, final long seed,
			final boolean useDiscounting, 
			final int numStates,
			final int numRounds,
			final boolean FAR ,
			final boolean constrain_naively,
			final boolean do_apricodd,
			final double apricodd_epsilon,
			final APPROX_TYPE approx_type, INITIAL_VALUE initial_VALUE ) {
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
		
		_valueDD = _manager.DD_ZERO;
		_policy = null;
		
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
				_policy = newValueDD._o2;
				break;
			}
			if( _dtr.terminate(error, iter, EPSILON, HORIZON) && !lastiter ){//)*(1-DISCOUNT)/(2*DISCOUNT) ){
				lastiter = true;
			}
		}
		
		System.out.println("Solution time: " + _solutionTimer.GetElapsedTimeInMinutes() );
		System.out.println("CPT time: " + _cptTimer.GetElapsedTimeInMinutes() );
		System.out.println("Size of value fn. = " + _manager.countNodes(_valueDD) );
		System.out.println("Size of policy = " + 
				_manager.countNodes( _FAR ? _policy._bddPolicy : _policy._addPolicy ) );
		System.out.println("Bellman backups = " + iter );
//		_manager.showGraph( _valueDD,_FAR ? _policy._bddPolicy : _policy._addPolicy );
	}
	
	public void stop(){
		_stop  = true;
	}
	
	private ADDRNode getInitialStateADD(
			final INITIAL_STATE_CONF init_conf, 
			final Double init_prob) {
		if( init_conf == null ){
			return null;
		}
		final ADDRNode ret = _dtr.getIIDInitialStates(init_conf, init_prob);
		return ret;
	}

	public ADDRNode getValueDD() {
		return _valueDD;
	}
	
	public ADDManager getManager() {
		return _manager;
	}
	
	public ADDDecisionTheoreticRegression getDTR() {
		return _dtr;
	}

	public ADDPolicy getPolicy() {
		return _policy;
	}
	
	public static void main(String[] args) throws InterruptedException {
		final int nStates = Integer.parseInt(args[5]);
		final int nRounds = Integer.parseInt(args[6]);
		final boolean useDisc = Boolean.parseBoolean( args[4] );
		
		final long seed = Long.parseLong(args[3]);
		final Random topLevel = new Random( seed );
		
		final SeqHindsight worker = new SeqHindsight(args[0], args[1], Double.parseDouble(args[2]), 
				DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, topLevel.nextLong(), 
				useDisc , nStates , 
				nRounds, Boolean.parseBoolean(args[7]),
				Boolean.parseBoolean( args[8] ), Boolean.parseBoolean( args[9] ),
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
	
	private int getHorizon() {
		return HORIZON;
	}
	
	private double getDiscount(){
		return DISCOUNT;
	}

}