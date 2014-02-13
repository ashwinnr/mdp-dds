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

import mdp.define.Action;
import mdp.define.PolicyStatistics;
import mdp.define.State;

import dd.DDManager.APPROX_TYPE;
import dtr.add.ADDDecisionTheoreticRegression;
import dtr.add.ADDPolicy;
import dtr.add.ADDValueFunction;
import dtr.add.NoOpPolicy;
import dtr.add.RDDLPolicy;
import dtr.add.RandomPolicy;

import add.ADDManager;
import add.ADDRNode;
import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDL2DD;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.viz.ElevatorDisplay;
import rddl.viz.StateViz;
import util.Timer;
import util.UnorderedPair;

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
	
	public BaseLinePolicy(String domain, String instance, 
			final long seed,
			final boolean useDiscounting, 
			final int numStates,
			final int numRounds,
			final BaseType type ) {
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
			_policy.executePolicy(_nRounds, _nStates, _useDiscounting, 
					HORIZON, DISCOUNT, null  ).printStats();
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
				Integer.parseInt(args[5]) ,  BaseType.valueOf( args[6] ) );
		Thread t = new Thread( worker );
		t.start();
		t.join();
	}
}