package rddl.mdp;

import java.util.List;
import java.util.Set;

import factored.mdp.define.FactoredActionSpace;

public class RDDLFactoredActionSpace implements FactoredActionSpace<RDDLFactoredStateSpace> {

	private static Set<String> actionVariables;

	public void setActionVariables(Set<String> actionVariables) {
		RDDLFactoredActionSpace.actionVariables = actionVariables;
	}
	
	@Override
	public Set<String> getActionVariables() {
		return actionVariables;
	}

}
