package mdp.solve.online.thts;

import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredState;
import factored.mdp.define.FactoredStateSpace;

public interface THTS<S extends FactoredStateSpace,A extends FactoredActionSpace<S>> {
    //generic trial based heuristic search
    public <T extends Node<S,A>> boolean is_node_visited( T node );
    public <T extends Node<S,A>> void initilialize_node( T node );
    public <T extends Node<S,A>, X extends Node<S,A>> X pick_successor_node( T node );
    public <T extends Node<S,A>> void update_node( T node );
    public <T extends Node<S,A>> void visit_node( T node );
    public <T extends Node<S,A>> boolean is_solved_node( T node );
}
