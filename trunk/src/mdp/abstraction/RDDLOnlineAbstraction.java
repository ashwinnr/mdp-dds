package mdp.abstraction;

import java.util.List;
import java.util.Map;

import add.ADDRNode;
import rddl.mdp.RDDL2DD.DEBUG_LEVEL;
import rddl.mdp.RDDL2DD.ORDER;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;
import mdp.abstraction.parameters.ADDAbstractionParameters;
import mdp.abstraction.types.ADDAbstractionType;
import mdp.solve.online.RDDLOnlineActor;

//make this class abstract so it does not depend on solution algorithm ?
public class RDDLOnlineAbstraction< T extends ADDAbstractionType ,  U extends ADDAbstractionParameters<T>  > 
	extends RDDLOnlineActor implements MDPAbstraction<T, U> {

	public RDDLOnlineAbstraction(String domain, String instance,
			boolean actionVars, DEBUG_LEVEL debug, ORDER order, long seed,
			boolean useDiscounting, int numStates, int numRounds,
			INITIAL_STATE_CONF init_state_conf, double init_state_prob) {
		super(domain, instance, actionVars, debug, order, seed, useDiscounting,
				numStates, numRounds, init_state_conf, init_state_prob);
	}

	@Override
	public ADDRNode doAbstract(ADDRNode some_dd, T abstraction_type, U params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, ADDRNode> abstract_dynamics(
			Map<String, ADDRNode> dynamics) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ADDRNode> abstract_rewards(List<ADDRNode> reward) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> act(
			FactoredState<RDDLFactoredStateSpace> state) {
		// TODO Auto-generated method stub
		return null;
	}

}
