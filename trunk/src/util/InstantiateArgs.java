package util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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
	ret.addOption("apricoddType", true, "APRICODD type - UPPER/LOWER/AVERAGE/NONE/RANGE");
	ret.addOption("apricoddGP", true, "geometric error progression with depth > 1" );
	ret.addOption("heuristicType", true, "backups to use for computing heuristic " +
	"- VI_FAR/VI_SPUDD/VI_MBFAR/VMAX" );
	ret.addOption("heuristicMins",  true, "max. minutes to run heuristic computation - double" );
	ret.addOption("heuristicSteps", true, "number of steps of VI for heuristic computation - int" );
	ret.addOption("memoryBoundNodes", true, "memory bound for MBFAR - long" );
	ret.addOption("initialStateConf", true, "IID initial state distribution " +
	"- CONJUNCTIVE/BERNOULLI/UNIFORM" );
	ret.addOption("initialStateProb", true, "IID parameter - double in [0,1]" );
	ret.addOption("backupType", true, "backups for RTDP - VI_FAR/VI_SPUDD/VI_MBFAR" );
	ret.addOption("numTrajectories", true, "number of trajectories to sample - int");
	ret.addOption("stepsDP", true, "number of trajectory replays -int " );
	ret.addOption("stepsLookahead", true, "number of states in trajectories - int");
	ret.addOption("generalizeStates", true, "whether to generalize states in trajectory" );
	ret.addOption("generalizeActions", true, "whether to generalize actions in trajectory" );
	ret.addOption("limitGeneralizedStates", true, "sample size for number of " +
	"states in generalized states - int" );
	ret.addOption("limitGeneralizedActions", true, "sample size for number of " + 
	"actions in generalized actions - int" );
	ret.addOption("generalization", true, "type of generalization - value/action/off");
	ret.addOption("exploration", true, "exploration for trajectory - epsilon/off");
	ret.addOption("generalizationRule", true, "rule for generalizing within an ADD - ALL_PATHS/SHARED_PATHS/NONE" );
	ret.addOption("consistencyRule", true, "rule for generalizing trajectory - WEAK_ACTION/WEAK_POLICY/STRONG_X" );
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
