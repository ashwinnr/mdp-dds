package mdp.abstraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mdp.abstraction.parameters.ADDAbstractionParameters;
import mdp.abstraction.types.ADDAbstractionType;
import add.ADDManager;
import add.ADDRNode;

public abstract class MDPAbstraction< T extends ADDAbstractionType ,  U extends ADDAbstractionParameters<T> > implements ADDAbstraction<T, U>{

	
	public Map<String, ADDRNode> abstract_dynamics(
			final Map<String, ADDRNode> dynamics,
			final U params, ADDManager manager) {
		final Map<String, ADDRNode> ret = new HashMap<String, ADDRNode>();
		for( Entry<String, ADDRNode> entry : dynamics.entrySet() ){
			ret.put( entry.getKey(), doAbstract(entry.getValue(), params, manager));
		}
		return Collections.unmodifiableMap(ret);
	}

	public List<ADDRNode> abstract_rewards(List<ADDRNode> reward,
			U params, ADDManager manager) {
		final List<ADDRNode> ret = new ArrayList<ADDRNode>();
		for( final ADDRNode rew : reward ){
			ret.add( doAbstract(rew, params, manager));
		}
		return ret;
	}
	
}
