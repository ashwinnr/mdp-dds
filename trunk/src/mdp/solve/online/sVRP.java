package mdp.solve.online;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import add.ADDManager;
import add.ADDRNode;
import dd.DDManager.APPROX_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.BACKUP_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.GENERALIZE_PATH;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import util.Timer;
import util.UnorderedPair;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;

public class sVRP extends RDDLOnlineActor {

	//in sumOrder
	protected ADDRNode[][] abstractModels = null;//depth rows, vars columns
	//any order
	protected ADDRNode[] abstractRewards = null;//depth rows, fixed cols
	
	private ADDRNode[] value_fns;
	private ADDRNode[] policy;
	
	private int steps_dp;
	private int steps_lookahead;
	private GENERALIZE_PATH _genRule;
	private int nTrials;
	private ADDManager _manager;
	private boolean CONSTRAIN_NAIVELY;
	private boolean do_apricodd;
	private double apricodd_epsilon;
	private APPROX_TYPE apricodd_type;
	private BACKUP_TYPE heuristic_type;
	private double time_heuristic_mins;
	private int steps_heuristic;
	private long MB;
	private BACKUP_TYPE dp_type;
	private Timer _DPTimer;
	private UnorderedPair<ADDRNode, ADDRNode> heuristic;
	private ADDRNode Rmax;
	private double EPSILON_REWARD;
	private double EPSILON_PROB;
			
	@Override
	public FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> act(
			FactoredState<RDDLFactoredStateSpace> state) {
		return null;
	}
	public sVRP(String domain, String instance, double epsilon_prob,
			final double epsilon_reward,
			DEBUG_LEVEL debug, ORDER order, long seed, boolean useDiscounting,
			int numStates, int numRounds, boolean FAR,
			boolean constrain_naively, boolean do_apricodd,
			double apricodd_epsilon, APPROX_TYPE apricodd_type,
			BACKUP_TYPE heuristic_type, double time_heuristic_mins,
			int steps_heuristic, long MB, INITIAL_STATE_CONF init_state_conf,
			double init_state_prob, BACKUP_TYPE dp_type, int nTrials,
			GENERALIZE_PATH rule, int steps_dp, int steps_lookahead) {
		super(domain, instance, FAR, debug, order, seed, useDiscounting, numStates,
				numRounds, init_state_conf, init_state_prob );
		value_fns = new ADDRNode[ HORIZON ];
		policy = new ADDRNode[ HORIZON ];
		abstractModels = new ADDRNode[ HORIZON ][ _mdp.getSumOrder().size() ];
		abstractRewards = new ADDRNode[ HORIZON ];
		
		EPSILON_REWARD = epsilon_reward;
		EPSILON_PROB = epsilon_prob;
		
		this.steps_dp = steps_dp;
		this.steps_lookahead = steps_lookahead;
		_genRule = rule;
		this.nTrials = nTrials;
		_manager = _mdp.getManager();
		CONSTRAIN_NAIVELY = constrain_naively;
		this.do_apricodd = do_apricodd;
		this.apricodd_epsilon = apricodd_epsilon;
		this.apricodd_type = apricodd_type;
		this.heuristic_type = heuristic_type;
		this.time_heuristic_mins = time_heuristic_mins;
		this.steps_heuristic = steps_heuristic;
		this.MB = MB;
		this.dp_type = dp_type;
		_DPTimer = new Timer();
		_DPTimer.PauseTimer();
		//initialize V0 to R
		final UnorderedPair<ADDRNode, ADDRNode> init = _dtr.computeLAOHeuristic(steps_heuristic, heuristic_type, 
				constrain_naively, do_apricodd, apricodd_epsilon, apricodd_type, MB, time_heuristic_mins);
		value_fns[0] = init._o1;
		policy[0] = init._o2;
		heuristic = init;
		this.Rmax = _mdp.getRMax();
		makeAbstractModels();
	}
	private void makeAbstractModels() {
		final Map<String, ADDRNode> true_model = _mdp.getCpts();
		final ADDRNode true_reward = _mdp.getSumOfRewards();
		final ArrayList<String> sumOrder = _mdp.getSumOrder();
		final int ns_size = sumOrder.size();
		
		//iterate for each depth
		for( int depth = 0; depth < HORIZON; ++depth ){
			final double error_prob = EPSILON_PROB/Math.pow(DISCOUNT, depth );
			final double error_rew = EPSILON_REWARD/Math.pow(DISCOUNT, depth );
			
			System.out.println( error_prob + " " + error_rew);
			
			//approximate model and reward
			for( int ns_index = 0 ; ns_index < ns_size; ++ns_index ){
				final String ns_var = sumOrder.get(ns_index);
				final ADDRNode this_true_model = true_model.get( ns_var );
				ADDRNode this_approx = _manager.doApricodd(this_true_model, do_apricodd, error_prob, apricodd_type);

				if( this_true_model.getID() != this_approx.getID() ){
					try{
						final ADDRNode approx_prob = _manager.normalizeConditionalPDF( this_approx, ns_var );
						System.out.println( "Leaves = " +
								Arrays.toString( 
										_manager.countLeaves( this_true_model, this_approx , approx_prob) ) );
						System.out.println( " Nodes = " + 
										_manager.countNodes( this_true_model, this_approx , approx_prob).toString() ) ;
						System.out.println( " Vars = " + 
								_manager.getVars( this_true_model, this_approx , approx_prob).toString() ) ;
						this_approx = approx_prob;
					}catch( ArithmeticException e ){
						System.out.println("Caught");
						e.printStackTrace();
						System.exit(1);
					}
//					_manager.showGraph( this_true_model, this_approx, approx_prob );
//					try {
//						System.in.read();
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
				}
				abstractModels[ depth ][ ns_index ] = this_approx;
				
			}
			//approximate reward
			final ADDRNode this_approx_reward = _manager.doApricodd(true_reward, do_apricodd, error_rew, apricodd_type);
			abstractRewards[ depth ] = this_approx_reward;
//			_manager.showGraph( true_reward, this_approx_reward );
		}
	}
	
	public static void main(String[] args) throws InterruptedException {

		final boolean use_disc = Boolean.parseBoolean( args[5] );
		final int nStates = Integer.parseInt( args[6] );
		final int nRounds = Integer.parseInt( args[7] );

		Runnable worker = new sVRP(
				args[0], args[1],
				Double.parseDouble( args[2] ),
				Double.parseDouble( args[3] ), 
				DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, 
				Long.parseLong( args[4] ), use_disc, 
				nStates, nRounds, Boolean.parseBoolean( args[8] ),
				Boolean.parseBoolean( args[9] ), 
				Boolean.parseBoolean(args[10] ), 
				Double.parseDouble( args[11] ), 
				APPROX_TYPE.valueOf( args[12] ),
				BACKUP_TYPE.valueOf( args[13] ), 
				Double.parseDouble( args[14] ),
				Integer.parseInt( args[15] ),
				Long.parseLong( args[16] ),
				INITIAL_STATE_CONF.valueOf( args[17] ),
				Double.valueOf( args[18] ),
				BACKUP_TYPE.valueOf( args[ 19 ] ),
				Integer.parseInt( args[ 20 ] ) , 
				GENERALIZE_PATH.valueOf( args[ 21 ] ),
				Integer.parseInt( args[ 22 ] ) , 
				Integer.parseInt( args[ 23 ] ) );

		Thread t = new Thread( worker );
		t.start();
		t.join();
	}

}
