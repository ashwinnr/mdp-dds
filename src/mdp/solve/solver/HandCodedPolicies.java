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
		}else if( domain_file.contains("academic_advising" ) ){
			//noop
			return dtr.getNoOpPolicy(actionVars, manager);
		}
		else {
			//random
			final ADDRNode random = manager.DD_ONE;
			final ADDRNode random_constrained = dtr.applyMDPConstraintsNaively(random, null, manager.DD_ZERO, null );
			final ADDRNode random_constrained_ties = manager.breakTiesInBDD(random_constrained, actionVars, false);
			return random_constrained_ties;
		}
    }
}
