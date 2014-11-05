package mdp.solve.online.thts;

import java.util.Arrays;
import java.util.NavigableMap;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import add.ADDRNode;

import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;
import util.InstantiateArgs;
import util.Timer;
import util.UnorderedPair;
import dd.DDManager.APPROX_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.BACKUP_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;
import mdp.generalize.trajectory.EBLGeneralization;
import mdp.generalize.trajectory.Generalization;
import mdp.generalize.trajectory.OptimalActionGeneralization;
import mdp.generalize.trajectory.RewardGeneralization;
import mdp.generalize.trajectory.ValueGeneralization;
import mdp.generalize.trajectory.GenericTransitionGeneralization.Consistency;
import mdp.generalize.trajectory.parameters.EBLParams;
import mdp.generalize.trajectory.parameters.GeneralizationParameters;
import mdp.generalize.trajectory.parameters.OptimalActionParameters;
import mdp.generalize.trajectory.parameters.RewardGeneralizationParameters;
import mdp.generalize.trajectory.parameters.ValueGeneralizationParameters;
import mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH;
import mdp.generalize.trajectory.type.GeneralizationType;
import mdp.solve.online.Exploration;
import mdp.solve.online.thts.SymbolicRTDP.LearningMode;
import mdp.solve.online.thts.SymbolicRTDP.LearningRule;

public class OldSymbolicRTDP< T extends GeneralizationType, 
	P extends GeneralizationParameters<T> >  extends SymbolicRTDP<T,P> {

	public OldSymbolicRTDP(
			String domain,
			String instance,
			double epsilon,
			DEBUG_LEVEL debug,
			ORDER order,
			boolean useDiscounting,
			int numStates,
			int numRounds,
			boolean constrain_naively,
			boolean do_apricodd,
			double[] apricodd_epsilon,
			APPROX_TYPE apricodd_type,
			INITIAL_STATE_CONF init_state_conf,
			double init_state_prob,
			int nTrials,
			double timeOutMins,
			int steps_lookahead,
			Generalization<RDDLFactoredStateSpace, RDDLFactoredActionSpace, T, P> generalizer,
			P generalize_parameters_wo_manager, boolean gen_fix_states,
			boolean gen_fix_actions, int gen_num_states, int gen_num_actions,
			GENERALIZE_PATH gen_rule, Consistency[] cons, Random topLevel ){
		super(domain, instance, epsilon, debug, order, useDiscounting, numStates,
				numRounds, constrain_naively, do_apricodd, apricodd_epsilon,
				apricodd_type, init_state_conf, init_state_prob, nTrials, timeOutMins,
				steps_lookahead, generalizer, generalize_parameters_wo_manager,
				gen_fix_states, gen_fix_actions, gen_num_states, gen_num_actions,
				gen_rule, cons, topLevel, steps_lookahead, 
				mdp.solve.online.thts.SymbolicRTDP.LearningRule.NONE, 
				-1,
				mdp.solve.online.thts.SymbolicRTDP.LearningMode.BATCH, 
				false, false );
	}
	
	@Override
	public FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> act(
			FactoredState<RDDLFactoredStateSpace> state) {
		do_sRTDP( state );	
		final NavigableMap<String, Boolean> action 
		= pick_successor_node(state, 0).getFactoredAction();
		cur_action.setFactoredAction(action);
		throwAwayEverything();
		return cur_action;
	}
	
	@Override
	protected void do_sRTDP(FactoredState<RDDLFactoredStateSpace> init_state) {

		int trials_to_go = nTrials;
		boolean timeOut = false;
		final Timer act_time = new Timer();
		int numSamples = 0;
		
		while( trials_to_go --> 0 && ( (timeOutMins == -1) || !timeOut ) ){
			FactoredState<RDDLFactoredStateSpace> cur_state = init_state;
			int num_actions = 0;
			
			while( num_actions < steps_lookahead-1 ){
				if( DISPLAY_TRAJECTORY ){
					System.out.println( cur_state.toString() );	
				}
				
				final NavigableMap<String, Boolean> this_action 
				= pick_successor_node(cur_state, 0).getFactoredAction();
				cur_action.setFactoredAction(this_action);
				
				if( DISPLAY_TRAJECTORY ){
					System.out.println( cur_action.toString() );	
				}

				final FactoredState<RDDLFactoredStateSpace> next_state 
				= pick_successor_node(cur_state, cur_action, 0);

				//generalize
				saveValuePolicy();
				ADDRNode gen_state
					= _generalizer.generalize_state(cur_state, cur_action, next_state, _genaralizeParameters, 0);
				
				//update
				UnorderedPair<ADDRNode, UnorderedPair<ADDRNode, Double>> the_update = _dtr.backup(_valueDD[0], _policyDD[0], _valueDD[0], gen_state, 
						BACKUP_TYPE.VI_FAR, 
						do_apricodd, do_apricodd ? apricodd_epsilon[0] : 0d, 
								apricodd_type,
						true, -1, CONSTRAIN_NAIVELY, null );
				_valueDD[0] = the_update._o1;
				_policyDD[0] = the_update._o2._o1;
				
				cur_state = next_state;
				
				++num_actions;
				
			}
			
			System.out.print("*");	
			++numSamples;
			
			act_time.PauseTimer();
			if( act_time.GetElapsedTimeInMinutes() >= timeOutMins ){
				timeOut = true;
			}
			
			if( trials_to_go % 50 == 0 ){
				FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> root_action = pick_successor_node(init_state, 0);
				
				System.out.println( "\nValue of init state " + 
						_manager.evaluate(_valueDD[0], init_state.getFactoredState() ).getMax() );
				System.out.println("DP time: " + _DPTimer.GetElapsedTimeInMinutes() );
				
				System.out.println("root node action : " + root_action.toString() );
				
				System.out.print("description of value of init state ");
				
				final ADDRNode path_aroo = _manager.get_path(_valueDD[0], init_state.getFactoredState() );
				
				System.out.println(  
								(_manager.enumeratePathsBDD( 
										path_aroo).iterator().next().size()-1.0d)/(1.0d*_mdp.getNumStateVars()) );
				
				
				System.out.println( "Value : " + 
						_manager.enumeratePathsBDD( path_aroo ) );
				
				System.out.print("description of policy of init state ");
				

				final ADDRNode policy_roo = _manager.get_path(
						_manager.restrict(_policyDD[0], root_action.getFactoredAction()), 
						init_state.getFactoredState() ) ;
				
				System.out.println( "Policy : " + 
						(_manager.enumeratePathsBDD( 
								policy_roo ).iterator().next().size()-1) / (1.0d*_mdp.getNumStateVars()) );
				
				System.out.println( "Policy : " + 
						_manager.enumeratePathsBDD( 
								policy_roo ) );
				
			}
			
			if( !timeOut ){
				act_time.ResumeTimer();
			}
			
		}
		
		System.out.println();
		System.out.println("num samples : " + numSamples );
		
		FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> root_action = pick_successor_node(init_state, 0);
		
		System.out.println( "Value of init state " + 
				_manager.evaluate(_valueDD[0], init_state.getFactoredState() ).toString() );
		System.out.println("DP time: " + _DPTimer.GetElapsedTimeInMinutes() );
		
		System.out.println("root node action : " + root_action.toString() );
		
		System.out.print("description of value of init state ");
		
		System.out.println(  
						(_manager.enumeratePathsBDD( 
								_manager.get_path(
										_valueDD[0], init_state.getFactoredState() )).iterator().next().size()-1.0d)/(1.0d*_mdp.getNumStateVars()) );
		
		
		System.out.println( "Value : " + 
				_manager.enumeratePathsBDD( _manager.get_path(_valueDD[0], init_state.getFactoredState() ) ) );
		
		System.out.print("description of policy of init state ");
		

		System.out.println( "Policy : " + 
				(_manager.enumeratePathsBDD( 
						_manager.get_path(
								_manager.restrict(_policyDD[0], root_action.getFactoredAction()), 
								init_state.getFactoredState() ) ).iterator().next().size()-1) / (1.0d*_mdp.getNumStateVars()) );
		
		System.out.println( "Policy : " + 
				_manager.enumeratePathsBDD( 
						_manager.get_path(
								_manager.restrict(_policyDD[0], root_action.getFactoredAction()), 
								init_state.getFactoredState() ) ) );
		
	}

	@Override
	protected void throwAwayEverything() {
		_policyDD = null;
		_policyDD = new ADDRNode[1];
		_policyDD[0] = _baseLinePolicy;
		
		_valueDD = null;
		_valueDD = new ADDRNode[ 1 ];
		final double rMax = _mdp.getRMax();
		final int horizon2 = _mdp.getHorizon();
		final double discount2 = _mdp.getDiscount();
		_valueDD[0] = _manager.getLeaf( discount2 == 1.0d ? rMax*horizon2
				: rMax/(1.0d-discount2) );
		
	}
	
	public static void main(String[] args) throws InterruptedException {

		System.out.println( Arrays.toString(args) );

		OldSymbolicRTDP srtdp = OldSymbolicRTDP.instantiateMe(args);
		
		Thread t = new Thread( srtdp );
		t.start();
		t.join();
	}


	public static OldSymbolicRTDP instantiateMe(String[] args) {
		
		final Options options = InstantiateArgs.createOptions();
		final CommandLine cmd = InstantiateArgs.parseOptions(args, options);
		try{
		Generalization generalizer = null;
		if( cmd.getOptionValue("generalization").equals("value") ){
			generalizer = new ValueGeneralization();
		}else if( cmd.getOptionValue("generalization").equals("action") ){
			generalizer = new OptimalActionGeneralization();
		}else if( cmd.getOptionValue("generalization").equals("EBL") ){
			generalizer = new EBLGeneralization();
		}
		else if( cmd.getOptionValue("generalization").equals("reward") ){
			generalizer = new RewardGeneralization();
		}
		
		
		final Random topLevel = new Random( Long.parseLong( cmd.getOptionValue("seed") ) );
		final boolean constrain_naively = !Boolean.parseBoolean( cmd.getOptionValue("constraintPruning") );
				
		GeneralizationParameters inner_params = null;
		if( cmd.getOptionValue("generalization").equals("value") ){
				inner_params = new ValueGeneralizationParameters( 
			null, GENERALIZE_PATH.valueOf( cmd.getOptionValue("generalizationRule") ), 
			 constrain_naively  );
		}else if( cmd.getOptionValue("generalization").equals("action") ){
				inner_params = new OptimalActionParameters(
						null, GENERALIZE_PATH.valueOf( cmd.getOptionValue("generalizationRule") ), 
						constrain_naively, null );
		}else if( cmd.getOptionValue("generalization").equals("EBL") ){
			inner_params = new EBLParams(null, null, 
					null , constrain_naively,
					Boolean.parseBoolean(cmd.getOptionValue("EBLPolicy") ) );
		}else if( cmd.getOptionValue("generalization").equals("reward") ){
			inner_params = new RewardGeneralizationParameters(null, 
					GENERALIZE_PATH.valueOf( cmd.getOptionValue("generalizationRule") ), 
					null, constrain_naively, null );
		}
		
		double[] epsilons = null;
		if( Boolean.parseBoolean( cmd.getOptionValue("doApricodd") ) ){
			epsilons = new double[ Integer.parseInt( cmd.getOptionValue("stepsLookahead") ) ];
			double epsilon = Double.parseDouble( cmd.getOptionValue("apricoddError") );
			for( int i = 0 ; i < epsilons.length; ++i ){
				epsilons[i] = epsilon;
			}
		}
		
		Exploration exploration = null;
		
		final String[] const_options = ( cmd.getOptionValues("consistencyRule") );
		final Consistency[] consistency = new Consistency[ const_options.length ];
		for( int i = 0 ; i < const_options.length; ++i ){
		    consistency[i] = Consistency.valueOf(const_options[i]);
		}
		 
		final double timeOut = Double.parseDouble( cmd.getOptionValue("timeOutMins") );
		System.out.println("Timeout " + timeOut );
		
		return new OldSymbolicRTDP(
				cmd.getOptionValue("domain"), cmd.getOptionValue("instance"),
				Double.parseDouble( cmd.getOptionValue("convergenceTest") ), 
				DEBUG_LEVEL.PROBLEM_INFO, ORDER.GUESS, 
				Boolean.parseBoolean( cmd.getOptionValue( "discounting" ) ), 
				Integer.parseInt( cmd.getOptionValue("testStates") ), 
				Integer.parseInt( cmd.getOptionValue( "testRounds" ) ),
				constrain_naively,
				Boolean.parseBoolean( cmd.getOptionValue("doApricodd") ), 
				epsilons,
				APPROX_TYPE.valueOf( cmd.getOptionValue("apricoddType") ), 
				INITIAL_STATE_CONF.valueOf( cmd.getOptionValue("initialStateConf") ),
				Double.parseDouble( cmd.getOptionValue("initialStateProb") ),
				Integer.parseInt( cmd.getOptionValue("numTrajectories") ),
				timeOut,
				Integer.parseInt( cmd.getOptionValue("stepsLookahead") ),
				generalizer, 
				inner_params, 
				!Boolean.parseBoolean( cmd.getOptionValue("generalizeStates") ),
				!Boolean.parseBoolean( cmd.getOptionValue("generalizeActions") ),
				Integer.parseInt( cmd.getOptionValue("limitGeneralizedStates") ),
				Integer.parseInt( cmd.getOptionValue("limitGeneralizedActions") ),
				GENERALIZE_PATH.valueOf( cmd.getOptionValue("generalizationRule") ), 
				consistency,
				new Random( topLevel.nextLong() ) );
		
		}catch( Exception e ){
			HelpFormatter help = new HelpFormatter();
			help.printHelp("symbolicRTDP", options);
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

}
