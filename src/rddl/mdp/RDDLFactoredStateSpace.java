package rddl.mdp;

import java.util.List;
import java.util.Set;

import factored.mdp.define.FactoredStateSpace;

public class RDDLFactoredStateSpace implements FactoredStateSpace {

	private static Set<String> stateVariables;
	
	public void setStateVariables(Set<String> _stateVars) {
		RDDLFactoredStateSpace.stateVariables = _stateVars;
	}
	
	@Override
	public Set<String> getStateVariables() {
		return stateVariables;
	}
	
}
