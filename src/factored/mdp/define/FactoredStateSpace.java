package factored.mdp.define;

import java.util.List;
import java.util.NavigableMap;
import java.util.Set;

import mdp.define.StateSpace;

public interface FactoredStateSpace extends StateSpace {
	public Set<String> getStateVariables( );
}
