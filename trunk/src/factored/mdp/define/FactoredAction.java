package factored.mdp.define;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;

import com.google.common.collect.Maps;

import mdp.define.Action;

//this is where the problems lie
public class FactoredAction<S extends FactoredStateSpace, A extends FactoredActionSpace<S>>
	extends Action<S,A> implements Comparable<FactoredAction<S,A>>{
	
	protected static String[] actionVarOrder = null;
	protected NavigableMap<String, Boolean> factoredAction = null;
	
	@Override
	public boolean equals(Object obj) {
		return factoredAction.equals( ((FactoredAction<S,A>)obj).getFactoredAction() );
	}
	
	public FactoredAction<S, A> setFactoredAction(NavigableMap<String, Boolean> factoredAction) {
		this.factoredAction = Maps.newTreeMap( factoredAction );
		return this;
	}
	
	public NavigableMap<String, Boolean> getFactoredAction( ) {
		return factoredAction == null ? null : Maps.unmodifiableNavigableMap( factoredAction );
	}

	@Override
	public int hashCode() {
		if( factoredAction == null ){
			return 0;
		}
		return factoredAction.values().hashCode();
	}
	
	@Override
	public String toString() {
		return factoredAction.toString();
	}

	public int compareTo(FactoredAction<S, A> arg0) {
		NavigableMap<String, Boolean> otherAction = arg0.getFactoredAction();
		
		Integer thisSize = factoredAction.size();
		Integer otherSize = otherAction.size();
		int comp = 0;
		comp = thisSize.compareTo(otherSize);
		if( comp != 0 ){
			return comp;
		}
		
		for( Map.Entry<String, Boolean> entry : factoredAction.entrySet() ){
			String thisString = entry.getKey();
			Boolean thisVal = entry.getValue();
			Boolean otherVal = otherAction.get( thisString );
			comp = thisVal.compareTo(otherVal);
			if( comp != 0 ){
				return comp;
			}
		}
		return 0;
	}
}
