package dtr;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;

import add.ADDRNode;

import dd.DDINode;
import dd.DDLeaf;
import dd.DDNode;
import dd.DDRNode;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredPolicy;
import factored.mdp.define.FactoredStateSpace;

public abstract class SymbolicPolicy<D extends DDNode, DR extends DDRNode<D>,
	DI extends DDINode<D,DR,? extends Collection<DR> >, 
		DL extends DDLeaf<?>, 
		S extends FactoredStateSpace, A extends FactoredActionSpace<S> > 
		implements FactoredPolicy<S,A> {
	
	public DR _bddPolicy;//with action vars
	public DR _addPolicy;//leaves are action IDs
	protected static Map<Integer, NavigableMap<String, Boolean> > leafMapping
		= new HashMap<Integer, NavigableMap<String,Boolean>>();
	protected static Map< NavigableMap<String, Boolean>, Integer > actToIntMap
		= new HashMap< NavigableMap<String, Boolean>, Integer >();
	
	protected abstract 
		void updateADDPolicy( final ADDRNode v_func, final ADDRNode q_func, final NavigableMap<String, Boolean> this_action );
	
	protected abstract void updateBDDPolicy( final ADDRNode v_func, final ADDRNode jointQFunc );
}
