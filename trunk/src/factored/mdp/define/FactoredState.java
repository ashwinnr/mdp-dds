package factored.mdp.define;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.Maps;

import rddl.mdp.RDDLFactoredStateSpace;

import mdp.define.State;

public class FactoredState<S extends FactoredStateSpace> extends State<S> {

	protected static String[] stateVariablesOrder = null;
	protected NavigableMap<String, Boolean> factoredState = null;
	
	public NavigableMap<String, Boolean> getFactoredState( ){
		return factoredState == null ? null :Maps.unmodifiableNavigableMap( factoredState );
	}

	@Override
	public int hashCode() {
		if( factoredState == null ){
			return 0;
		}
		return factoredState.values().hashCode();
	}
	
	public FactoredState<S> setStateVariablesOrder(String[] stateVariablesOrder) {
		FactoredState.stateVariablesOrder = stateVariablesOrder;
		return this;
	}
	
	public String[] getStateVariablesOrder() {
		return stateVariablesOrder;
	}

	public FactoredState<S> setFactoredState(NavigableMap<String, Boolean> factoredState) {
		this.factoredState = Maps.newTreeMap( factoredState );
		return this;
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
		return new FactoredState<S>().setFactoredState( getFactoredState() );
	}
}
