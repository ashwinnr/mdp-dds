package mdp.solve.online;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.NavigableMap;
import java.util.Random;

import mdp.define.Action;
import mdp.define.PolicyStatistics;
import add.ADDManager;
import add.ADDRNode;
import rddl.RDDL.DOMAIN;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredReward;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.mdp.RDDLFactoredTransition;
import rddl.viz.StateViz;
import util.Timer;
import util.UnorderedPair;
import dd.DDManager.APPROX_TYPE;
import dtr.add.ADDDecisionTheoreticRegression;
import dtr.add.ADDDecisionTheoreticRegression.BACKUP_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;

public abstract class RDDLOnlineActor implements Runnable {

	private static final boolean DISPLAY_TRAJECTORY = true;
	protected Random _actRand;
	protected Timer _cptTimer;
	protected RDDL2ADD _mdp;
	protected int _nStates;
	protected int _nRounds;
	protected boolean _useDiscounting;
	protected double DISCOUNT;
	protected int HORIZON;
	protected INITIAL_STATE_CONF init_state_conf;
	protected double init_state_prob;
	protected boolean _actionVars;
	protected RDDLFactoredTransition _transition;
	protected RDDLFactoredReward _reward;
	protected ADDDecisionTheoreticRegression _dtr;
	protected ADDManager _manager;
	private StateViz _viz;
	
	private Random _initStateRand;
	private Random _rewardRand;
	private String _domainFile;
	
	public RDDLOnlineActor(
			final String domain, 
			final String instance,
			final boolean actionVars,
			final DEBUG_LEVEL debug, 
			final ORDER order, final long seed,
			final boolean useDiscounting, 
			final int numStates,
			final int numRounds,
			final INITIAL_STATE_CONF init_state_conf ,
			final double init_state_prob,
			final StateViz visualizer ) {
		final Random topLevel = new Random( seed );
		
		_actRand = new Random( topLevel.nextLong() );
		_initStateRand = new Random( topLevel.nextLong() );
		_rewardRand = new Random( topLevel.nextLong() );
		
		_viz = visualizer;
		_actionVars = actionVars;
		
		_cptTimer = new Timer();
		_mdp = new RDDL2ADD(domain, instance, actionVars, debug, order, true, 
				topLevel.nextLong() );
		_domainFile = domain;
		
		_dtr = new ADDDecisionTheoreticRegression( this._mdp, topLevel.nextLong() );
		_manager = _mdp.getManager();
		
		_cptTimer.StopTimer();
		System.out.println("CPT time: " + _cptTimer.GetTimeSoFarAndResetInMinutes() );
		
		_nStates = numStates;
		_nRounds = numRounds;
		_useDiscounting = useDiscounting;
		DISCOUNT = _mdp.getDiscount();
		HORIZON = _mdp.getHorizon();
		this.init_state_conf = init_state_conf;
		this.init_state_prob = init_state_prob;
		
		_transition = _mdp.getFactoredTransition();
		_reward = _mdp.getFactoredReward();
		
	}
	
	public abstract FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> act( 
			final FactoredState<RDDLFactoredStateSpace> state );
	
	@Override
	public void run() {
		final ADDRNode initial_state_add = _dtr.getIIDInitialStates( init_state_conf ,
				init_state_prob );
		
		int states_to_go  = _nStates;
		final PolicyStatistics stats = new PolicyStatistics(_nStates, _nRounds);
		
		while( states_to_go --> 0 ){
			
			final FactoredState<RDDLFactoredStateSpace> init_state 
			= _transition.sampleState(initial_state_add , _initStateRand, _manager );
			System.out.println("Initial state #" + states_to_go + " " + init_state.toString() );
			int rounds_to_go = _nRounds;
			
			
			while( rounds_to_go --> 0 ){
				System.out.println("Round #" + rounds_to_go );
				
				int horizon_to_go = HORIZON;
				
				FactoredState<RDDLFactoredStateSpace> cur_state = init_state;
				double round_reward = 0;
				double cur_disc = 1;
				Timer roundTimer = new Timer();
				
				boolean trial_is_over = false;
				while( horizon_to_go --> 0 && !trial_is_over  ){
					final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> 
						action = act( cur_state );
					final FactoredState<RDDLFactoredStateSpace> next_state 
					= _transition.sampleFactored(cur_state, action, _actRand );
					round_reward += cur_disc * _reward.sample( cur_state, action, _rewardRand  );
				
					if( DISPLAY_TRAJECTORY ){
						System.out.println( "State : " + cur_state.toString() ); 
						System.out.println("Action " + action.toString() );
						System.out.println("Next state " + next_state.toString() );
					}
					
					_transition.displayState( _viz, cur_state, action, horizon_to_go );
					cur_state = next_state;
//					System.out.print( horizon_to_go );
					cur_disc = ( _useDiscounting ) ? cur_disc * DISCOUNT : cur_disc;
					
					if( _domainFile.contains("crossing_traffic") ){
						for( final PVAR_INST_DEF nfs : _mdp._n._alNonFluents ){
							if( nfs._sPredName._sPVarName.contains("GOAL") ){
								ArrayList<LCONST> goal_params = nfs._alTerms;
								ArrayList<LCONST> x_param = new ArrayList<LCONST>();
								x_param.add( goal_params.get(0) );
								
								ArrayList<LCONST> y_param = new ArrayList<LCONST>();
								y_param.add( goal_params.get(1) );
								
								if( ((Boolean)_mdp._state.getPVariableAssign(new PVAR_NAME("robot-at-x"), x_param )).equals(Boolean.TRUE)
										&& ((Boolean)_mdp._state.getPVariableAssign(new PVAR_NAME("robot-at-y"), y_param )).equals(Boolean.TRUE) ){
									System.out.println("goal reached - terminating");
									round_reward += horizon_to_go;
									trial_is_over = true;
									break;
								}
							}
						}
						
						for( final String svar : _mdp.get_stateVars() ){
							if( svar.contains("collision") ){
								if( cur_state.getFactoredState().get(svar) == true ){
									System.out.println("collision detected... terminating");
									trial_is_over = true;
									break;
								}
							}
						}
						//check for collision
					}
					
				}
				_transition.displayState( _viz, cur_state, null, horizon_to_go);
				System.out.println();
				System.out.println("Rounds to go " + rounds_to_go );
				System.out.println("Round time : " + roundTimer.GetTimeSoFarAndResetInMinutes() );
				System.out.println("Round reward : " + round_reward );
				stats.addRoundStats(round_reward);
			}
			System.out.println("States to go " + states_to_go );
		}
		stats.printStats();
	}

}
