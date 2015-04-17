package util;

import java.util.Arrays;

import mdp.generalize.trajectory.GenericTransitionGeneralization.Consistency;
import mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH;
import mdp.solve.online.thts.SymbolicTHTS.GLOBAL_INITIALIZATION;
import mdp.solve.online.thts.SymbolicTHTS.LOCAL_INITIALIZATION;
import mdp.solve.online.thts.SymbolicTHTS.REMEMBER_MODE;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import dd.DDManager;
import dtr.add.ADDDecisionTheoreticRegression.BACKUP_TYPE;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;

public class InstantiateArgs {
    //the purpose of this class is to provide
    //static methods for instantiating solvers
    //common options are written here
    //alongwith how to instantiate sRTDP and SPUDDFAR
    public static Options createOptions( ) {
	Options ret = new Options();

	ret.addOption("discounting", true, "use discounting in planning " +
	"and testing - double in [0,1]" );
	ret.addOption("testStates", true, "number of initial states to test policy on - integer" );
	ret.addOption("testRounds", true, "number of rounds to test each initial state on - integer" );
	ret.addOption("domain", true, "RDDL domain file name - string");
	ret.addOption("instance", true, "RDDL instance file name - string" );
	ret.addOption("seed", true, "random seed - long" );
	ret.addOption("actionVars", true, "use action variables in ADDs" );
	ret.addOption("constraintPruning", true, "use ADD pruning for constraints" );
	ret.addOption("doApricodd", true, "enable APRICODD for value functions" );
	ret.addOption("apricoddError", true, "error for ADD approximation - double" );
	ret.addOption("apricoddType", true, "APRICODD type - " + Arrays.toString( DDManager.APPROX_TYPE.values() ) );

	ret.addOption("heuristicType", true, "backups to use for computing heuristic " +
	"- " + Arrays.toString( BACKUP_TYPE.values() ) );
	ret.addOption("heuristicMins",  true, "max. minutes to run heuristic computation - double" );
	ret.addOption("heuristicSteps", true, "number of steps of VI for heuristic computation - int" );
	ret.addOption("memoryBoundNodes", true, "memory bound for MBFAR - long" );
	ret.addOption("initialStateConf", true, "IID initial state distribution " +
				Arrays.toString( INITIAL_STATE_CONF.values() ) );
	ret.addOption("initialStateProb", true, "IID parameter - double in [0,1]" );
	ret.addOption("backupType", true, "backups for RTDP " +  Arrays.toString( BACKUP_TYPE.values() ) );
	ret.addOption("numTrajectories", true, "number of trajectories to sample - int");
	ret.addOption("timeOutMins", true, "time in mins for making one decision - use -1 to disable ");
	ret.addOption("stepsLookahead", true, "number of states in trajectories - int");
	ret.addOption("generalizeStates", true, "whether to generalize states in trajectory" );
	ret.addOption("generalizeActions", true, "whether to generalize actions in trajectory" );
	ret.addOption("limitGeneralizedStates", true, "sample size for number of " +
	"states in generalized states - int" );
	ret.addOption("limitGeneralizedActions", true, "sample size for number of " + 
	"actions in generalized actions - int" );
	ret.addOption("generalization", true, "type of generalization - value/action/reward/EBL/off");
	ret.addOption("exploration", true, "exploration for trajectory - epsilon/off");
	ret.addOption("generalizationRule", true, "rule for generalizing within an ADD - " + 
			Arrays.toString( GENERALIZE_PATH.values() ) );

	ret.addOption( OptionBuilder.withArgName("consistencyRule").withValueSeparator(',').hasArgs()
		.withDescription("comma separated rules for generalizing trajectory - " +
				Arrays.toString( Consistency.values() ) )
		.create("consistencyRule") );
	ret.addOption("enableLabelling", true, "enable labelling states as solved" );
	ret.addOption("convergenceTest", true, "convergence test for labelling nodes - double" );
	ret.addOption("onPolicyDepth", true, "depth from which policy constraint is added");
	ret.addOption("learningRule", true, "Learning rule - DecisionList");
	ret.addOption("maxRules", true, "max num rules" );
	ret.addOption("learningMode", true, "whether ONLINE/BATCH for learning with mx rules");
	ret.addOption("do_Xion", true, "invariance of abstract states" );
	ret.addOption("stat_vfn", true, "stationary value fn" );

//	ret.addOption( "initGen", true, "initialization for generalized state " 
//			+ Arrays.toString( START_STATE.values() ) );
//	ret.addOption("stopGen", true, "stoping condition for generalization" 
//			+ Arrays.toString( STOPPING_CONDITION.values() ) );
//	ret.addOption( "nextGen", true, "how to generate subsequent generalizations - " + 
//			Arrays.toString( REMOVE_VAR_CONDITION.values()) );
//	ret.addOption( "maxIgnoreDepth", true, "maximum variables that can be ignored" );
//	ret.addOption( "timePerState", true, "time(mins) per state" );
	
	ret.addOption( "global_init", true, "Global initialization " + GLOBAL_INITIALIZATION.values() );
	ret.addOption( "local_init", true, "time(mins) per state " + LOCAL_INITIALIZATION.values() );
	ret.addOption("truncate_trials", true, "whether to truncate trial on new state" );
	ret.addOption("mark_visited", true, "whether to maintain BDD of visitation");
	ret.addOption("mark_solved", true, "whether to label BDD" );
	ret.addOption("remember_mode", true, "remember across decisions " + 
			Arrays.toString( REMEMBER_MODE.values() ) );
	ret.addOption("init_reward", true, "whether to start with reward function" );
	ret.addOption("prune_limit", true, "long to trigger pruning" );
	
	return ret;
    }   

    public static CommandLine parseOptions(String[] args, Options opts) {
	try {
	    return new GnuParser().parse(opts, args);
	} catch (ParseException e) {

	    HelpFormatter helper = new HelpFormatter();
	    helper.printHelp("symbolicRTDP", opts);

	    e.printStackTrace();

	    System.exit(1);
	} 
	return null;
    }
    
    
}
