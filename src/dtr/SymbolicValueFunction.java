package dtr;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;

import dd.DDINode;
import dd.DDLeaf;
import dd.DDManager;
import dd.DDNode;
import dd.DDRNode;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredStateSpace;
import factored.mdp.define.FactoredValueFunction;

public abstract class SymbolicValueFunction<D extends DDNode, DR extends DDRNode<D>,
	DI extends DDINode<D,DR,? extends Collection<DR> >, 
		DL extends DDLeaf<?>, 
		S extends FactoredStateSpace, A extends FactoredActionSpace<S> > 
		implements FactoredValueFunction<S,A>{
	
	private DR _valueFn;
	protected NavigableMap< ? extends FactoredAction<S,A>, DR> _qFn = null;
	protected  DDManager<D, DR, DI, DL> _manager;
	private DR _jointQFn = null;
	
	public abstract void showValueFunctions();
	public abstract void throwAwayQFunctions();
	public DR get_valueFn() {
		return _valueFn;
	}
	public void set_valueFn(DR _valueFn) {
		this._valueFn = _valueFn;
	}
	public DR get_jointQFn() {
		return _jointQFn;
	}
	public void set_jointQFn(DR _jointQFn) {
		this._jointQFn = _jointQFn;
	}

}
