package mdp.solve.online.thts;

import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredStateSpace;

public interface NodeVisitor<S extends FactoredStateSpace, A extends FactoredActionSpace<S>>{
    public <T extends Node<S,A> > void  do_visit( T node );
}
