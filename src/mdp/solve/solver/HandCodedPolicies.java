package mdp.solve.solver;

import java.util.Set;

import rddl.EvalException;

import add.ADDManager;
import add.ADDRNode;
import dtr.add.ADDDecisionTheoreticRegression;

public class HandCodedPolicies {

    public static ADDRNode get( final String domain_file, 
    		final ADDDecisionTheoreticRegression dtr, ADDManager manager, 
    		Set<String> actionVars ){
    	
	if( domain_file.contains("crossing_traffic") ){
		return manager.getIndicatorDiagram("delta_y_2".intern(), true);
	}
	else if( domain_file.contains("sysadmin") ){
		return dtr.applyMDPConstraintsNaively(manager.DD_ONE, null, manager.DD_ZERO, null );
	}
	else if( domain_file.contains("skill_teaching") ){
		return dtr.applyMDPConstraintsNaively(manager.DD_ONE, null, manager.DD_ZERO, null );
	}
//	else if( domain_file.contains("grid") ){
//		return ADDDecisionTheoreticRegression.getNoOpPolicy(actionVars, manager);
//	}
	return ADDDecisionTheoreticRegression.getNoOpPolicy(actionVars, manager);
    }
}
