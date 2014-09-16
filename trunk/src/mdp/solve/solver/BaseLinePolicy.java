/**
 * 
 */
package mdp.solve.solver;

import java.util.Random;

import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import util.Timer;
import add.ADDManager;
import add.ADDRNode;
import dtr.add.ADDDecisionTheoreticRegression;
import dtr.add.ADDPolicy;
import dtr.add.NoOpPolicy;
import dtr.add.RandomPolicy;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;

public class BaseLinePolicy implements Runnable{

	private ADDDecisionTheoreticRegression _dtr;
	private ADDManager _manager;
	private Timer _cptTimer;
	private Timer _solutionTimer;
	private int HORIZON;
	private double DISCOUNT;
	private int _nStates;
	private int _nRounds;
	private boolean _useDiscounting;
	private enum BaseType{
		RANDOM, NOOP
	};
	private BaseType _type;
	private RDDL2ADD _mdp;
	private long _seed;
	private ADDRNode _initial_state_add;
	
	public BaseLinePolicy(
			final String domain, 
			final String instance, 
			final long seed,
			final boolean useDiscounting, 
			final int numStates,
			final int numRounds,
			final BaseType type,
			final INITIAL_STATE_CONF init_state_conf,
			final double init_prob ) {
		_cptTimer = new Timer();
		_mdp = new RDDL2ADD(domain, instance, true, DEBUG_LEVEL.PROBLEM_INFO, 
				ORDER.GUESS, false, seed);
		_cptTimer.StopTimer();
		_nStates = numStates;
		_nRounds = numRounds;
		_useDiscounting = useDiscounting;
		_seed = seed;
		_dtr = new ADDDecisionTheoreticRegression(_mdp, seed);
		_manager = _mdp.getManager();
		DISCOUNT = _mdp.getDiscount();
		HORIZON = _mdp.getHorizon();
		_type = type;
		_initial_state_add = _dtr.getIIDInitialStates(init_state_conf , init_prob);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
//	@Override
	public void run() {
		
		
		_solutionTimer = new Timer();

//		_manager, _mdp.getFactoredStateSpace(),
//		_mdp.getFactoredTransition(), _mdp.getFactoredReward(), _seed);

		ADDPolicy _policy = null;
		if( _type.equals(BaseType.RANDOM) ){
			_policy = new RandomPolicy(_manager, _mdp.getFactoredStateSpace(),
					_mdp.getFactoredTransition(), _mdp.getFactoredReward(), 
					_seed, _mdp, _dtr);
		}else{
			_policy = new NoOpPolicy(_manager, _mdp.getFactoredStateSpace(),
					_mdp.getFactoredTransition(), _mdp.getFactoredReward(), _seed );
		}
		
		try{
			final Random topLevel = new Random( _seed );
			topLevel.nextLong();
			
			_policy.executePolicy(_nRounds, _nStates, _useDiscounting, 
					HORIZON, DISCOUNT, null, _initial_state_add,
					new Random( topLevel.nextLong() ),
					new Random( topLevel.nextLong() ),
					new Random( topLevel.nextLong() ) ).printStats();
		}catch( Exception e ){
			e.printStackTrace();
		}
		
		System.out.println("Solution time: " + _solutionTimer.GetElapsedTimeInMinutes() );
		System.out.println("CPT time: " + _cptTimer.GetTimeSoFarAndResetInMinutes() );
//		_manager.showGraph( _valueDD,_FAR ? _policy._bddPolicy : _policy._addPolicy );
	}
	
	public static void main(String[] args) throws InterruptedException {

		Runnable worker = new BaseLinePolicy(args[0], args[1],
				Long.parseLong(args[2]), 
				Boolean.parseBoolean( args[3] ), Integer.parseInt(args[4]), 
				Integer.parseInt(args[5]) ,  BaseType.valueOf( args[6] ), 
				INITIAL_STATE_CONF.valueOf( args[7] ),
				Double.parseDouble( args[8] ) );
		
		Thread t = new Thread( worker );
		t.start();
		t.join();
	}
}