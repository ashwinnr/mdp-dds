package mdp.solve.online.thts;

import java.util.NavigableMap;

import factored.mdp.define.FactoredState;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;

public class RDDLStateNode 
	extends FactoredState<RDDLFactoredStateSpace>
	implements Node<RDDLFactoredStateSpace, RDDLFactoredActionSpace>{

    @Override
    public <T extends NodeVisitor<RDDLFactoredStateSpace, RDDLFactoredActionSpace>> void accept(
	    T visitor) {
	visitor.do_visit(this);
    }
    
    public RDDLStateNode(final FactoredState<RDDLFactoredStateSpace> state) {
	super();
	setFactoredState(state.getFactoredState());
    }

    public RDDLStateNode(final NavigableMap<String, Boolean> state_assign) {
	super();
	setFactoredState(state_assign);
    }
}
