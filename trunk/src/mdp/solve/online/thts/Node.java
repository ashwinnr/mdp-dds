package mdp.solve.online.thts;

import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredStateSpace;

public interface Node< S extends FactoredStateSpace, A extends FactoredActionSpace<S> > {
    public <T extends NodeVisitor<S,A> > void accept( T visitor );
}
