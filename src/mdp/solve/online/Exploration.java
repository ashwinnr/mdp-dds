package mdp.solve.online;

import add.ADDRNode;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredState;
import factored.mdp.define.FactoredStateSpace;

public interface Exploration<S extends FactoredStateSpace, A extends FactoredActionSpace<S>> {
    public boolean is_explore( final FactoredState<S> state, final FactoredAction<S,A> action, final ADDRNode value_fn , final ADDRNode policy_fn );
}
