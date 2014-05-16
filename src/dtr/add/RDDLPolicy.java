package dtr.add;

import java.util.Random;

import dtr.add.ADDDecisionTheoreticRegression.INITIAL_STATE_CONF;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import rddl.viz.StateViz;
import mdp.define.Action;
import mdp.define.Policy;
import mdp.define.PolicyStatistics;
import mdp.define.State;

public interface RDDLPolicy extends Policy<RDDLFactoredStateSpace, RDDLFactoredActionSpace> {

	public <T extends State<RDDLFactoredStateSpace>, U extends Action<RDDLFactoredStateSpace, RDDLFactoredActionSpace>> U getAction(
			T state) ;
	public PolicyStatistics executePolicy(int numRounds, int numStates,
			boolean useDiscounting, int horizon, double discount, 
			final Random initial_state_random, 
			final Random state_transtion_random,
			final Random reward_sample_random );
	public PolicyStatistics executePolicy(int numRounds, int numStates,
			boolean useDiscounting, int horizon, double discount, final
			StateViz visualizer, 
			final Random initial_state_random, 
			final Random state_transtion_random,
			final Random reward_sample_random );
}
