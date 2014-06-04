package util;

import java.util.Arrays;

import mdp.generalize.trajectory.GenericTransitionGeneralization.Consistency;
import mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH;
import mdp.generalize.trajectory.parameters.OptimalActionParameters.UTYPE;
import mdp.generalize.trajectory.parameters.GenericTransitionParameters;

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
	ret.addOption("apricoddGP", true, "geometric error progression with depth > 1" );
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
	ret.addOption("stepsDP", true, "number of trajectory replays -int " );
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
	ret.addOption("truncateTrials", true, "whether to truncate trial on new state" );
	ret.addOption( OptionBuilder.withArgName("consistencyRule").withValueSeparator(',').hasArgs()
		.withDescription("comma separated rules for generalizing trajectory - " +
				Arrays.toString( Consistency.values() ) )
		.create("consistencyRule") );
	ret.addOption("enableLabelling", true, "enable labelling states as solved" );
	ret.addOption("convergenceTest", true, "convergence test for labelling nodes - double" );
	ret.addOption("actionAllDepth", true, "whether to aggrate over level as well - boolean" );
	ret.addOption("actionType", true, "type of aggregate - " + Arrays.toString( UTYPE.values() ) );
	ret.addOption("EBLPolicy", true, "EBL region on policy- bool" );
	ret.addOption("onPolicyDepth", true, "depth from which policy constraint is added");
	
	ret.addOption("backChainThreshold", true, 
			"Whether to remeber the updates for univisted nodes- double - change in value wrt heuristic" );
	
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
