package factored.mdp.define;

import java.util.List;
import java.util.NavigableMap;
import java.util.Set;

import mdp.define.ActionSpace;

public interface FactoredActionSpace<S extends FactoredStateSpace> extends ActionSpace<S>{

	public Set<String> getActionVariables( );
	
}
