package mdp.solve.solver;

import java.util.Set;

import rddl.EvalException;

import add.ADDManager;
import add.ADDRNode;
import dtr.add.ADDDecisionTheoreticRegression;

public class HandCodedPolicies {

    public static ADDRNode get( final String domain_file, final ADDDecisionTheoreticRegression dtr, ADDManager manager, Set<String> actionVars ){
	if( domain_file.contains("sysadmin") ){
	    try {
		return ADDDecisionTheoreticRegression.getRebootDeadPolicy(manager, dtr, actionVars);
	    } catch (EvalException e) {
		e.printStackTrace();
		System.exit(1);
	    }
	}
	return null;
    }
}
