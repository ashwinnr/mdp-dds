package dtr.add;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import dd.DDManager.DDOper;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredActionSpace;

import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredReward;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.mdp.RDDLFactoredTransition;
import add.ADDINode;
import add.ADDLeaf;
import add.ADDManager;
import add.ADDNode;
import add.ADDRNode;

public class NoOpPolicy extends ADDPolicy {
	
	protected final static NavigableMap<String, Boolean> 
		_nullMap = new TreeMap<String, Boolean>();
	public NoOpPolicy(ADDManager man, RDDLFactoredStateSpace stateSpace,
			RDDLFactoredTransition transition, RDDLFactoredReward reward,
			long seed ) {
		super(man, stateSpace, transition, reward);
	}
	
	public <T extends factored.mdp.define.FactoredState<RDDLFactoredStateSpace>, 
	U extends factored.mdp.define.FactoredAction<RDDLFactoredStateSpace,rddl.mdp.RDDLFactoredActionSpace>>
		U getFactoredAction(T state) {
		return (U) new FactoredAction<RDDLFactoredStateSpace, 
				RDDLFactoredActionSpace>( ).setFactoredAction( _nullMap);
	}

}