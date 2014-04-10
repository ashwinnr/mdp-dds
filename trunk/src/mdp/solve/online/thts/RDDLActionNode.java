package mdp.solve.online.thts;
import java.util.NavigableMap;

import factored.mdp.define.FactoredAction;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;

public class RDDLActionNode 
	extends FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>
	implements Node<RDDLFactoredStateSpace, RDDLFactoredActionSpace>{

    public RDDLActionNode(final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action) {
	super();
	setFactoredAction(action.getFactoredAction());
    }
    
    @Override
    public <T extends NodeVisitor<RDDLFactoredStateSpace, RDDLFactoredActionSpace>> void accept(
	    T visitor) {
	visitor.do_visit(this);
    }
    
    public RDDLActionNode( final NavigableMap<String,Boolean> action_assign) {
	setFactoredAction(action_assign);
    }

}
