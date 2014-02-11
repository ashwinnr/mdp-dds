package rddl.mdp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import factored.mdp.define.FactoredStateSpace;

public class RDDLFactoredStateSpace implements FactoredStateSpace {

	private static Set<String> stateVariables;
	private static Set<String> nextStateVariables;
	
	public void setStateVariables(Set<String> _stateVars) {
		RDDLFactoredStateSpace.stateVariables = _stateVars;
	}
	
	public Set<String> getNextStateVars() {
		if( nextStateVariables == null ){
			nextStateVariables = new TreeSet<String>();
			for( final String s : stateVariables ){
				nextStateVariables.add( (s + "'").intern() );
			}
		}
		return nextStateVariables;
	}
	
	@Override
	public Set<String> getStateVariables() {
		return stateVariables;
	}
	
}
