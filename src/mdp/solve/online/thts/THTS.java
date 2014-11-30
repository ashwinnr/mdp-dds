package mdp.solve.online.thts;

import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredState;
import factored.mdp.define.FactoredStateSpace;

public interface THTS<S extends FactoredStateSpace,A extends FactoredActionSpace<S>> {
    //generic trial based heuristic search
    public boolean is_node_visited( FactoredState<S> state , int depth );
    public boolean is_node_solved( FactoredState<S> state , int depth );

    public void initialize_node( FactoredState<S> state , int depth );
    
    public FactoredState<S> pick_successor_node( FactoredState<S> state, 
    		FactoredAction<S,A> action , int depth );
    public FactoredAction<S,A> pick_successor_node( FactoredState<S> state , int depth );
    
//    public void update_node( FactoredState<S> state, int depth  );
//    public void update_node( FactoredState<S> state, FactoredAction<S,A> action , int depth );
    
    public void visit_node( FactoredState<S> state , int depth );
}
