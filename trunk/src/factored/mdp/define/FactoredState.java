package factored.mdp.define;

import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import rddl.mdp.RDDLFactoredStateSpace;

import mdp.define.State;

public class FactoredState<S extends FactoredStateSpace> extends State<S> {

	protected static String[] stateVariablesOrder = null;
	protected NavigableMap<String, Boolean> factoredState = null;
	
	public NavigableMap<String, Boolean> getFactoredState( ){
		return factoredState;
	}

	@Override
	public int hashCode() {
		if( factoredState == null ){
			return 0;
		}
		return factoredState.values().hashCode();
	}
	
	public void setStateVariablesOrder(String[] stateVariablesOrder) {
		FactoredState.stateVariablesOrder = stateVariablesOrder;
	}
	
	public String[] getStateVariablesOrder() {
		return stateVariablesOrder;
	}

	public void setStateVariable(String varName, boolean value) {
		if( factoredState == null ){
			factoredState = new TreeMap<String, Boolean>();
		}
		factoredState.put(varName, value);
	}
	
	public void setFactoredState(NavigableMap<String, Boolean> factoredState) {
		this.factoredState = factoredState;
	}
	
	@Override
	public boolean equals(Object obj) {
		return factoredState.equals(((FactoredState<S>)obj).getFactoredState());
	}
	
	@Override
	public String toString() {
		return factoredState.toString();
	}

	public FactoredState<S> copy() {
		FactoredState<S> ret = new FactoredState<S>();
		ret.setFactoredState( new TreeMap<String, Boolean>( factoredState ) );
		return ret;
	}
}
