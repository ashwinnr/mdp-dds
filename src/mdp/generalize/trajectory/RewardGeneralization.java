package mdp.generalize.trajectory;

import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;

import dd.DDManager.DDOper;

import add.ADDManager;
import add.ADDRNode;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;
import mdp.generalize.trajectory.parameters.RewardGeneralizationParameters;
import mdp.generalize.trajectory.parameters.ValueGeneralizationParameters;
import mdp.generalize.trajectory.type.RewardGeneralizationType;
import mdp.generalize.trajectory.type.ValueGeneralizationType;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;

public class RewardGeneralization  extends 
Generalization<RDDLFactoredStateSpace, RDDLFactoredActionSpace, 
RewardGeneralizationType, RewardGeneralizationParameters>{

	@Override
	public ADDRNode generalize_state(
			FactoredState<RDDLFactoredStateSpace> state,
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
			FactoredState<RDDLFactoredStateSpace> next_state,
			RewardGeneralizationParameters parameters, int depth) {
		final ADDManager manager = parameters.get_manager();
		

		final NavigableMap<String, Boolean> state_assign = state.getFactoredState();
		final NavigableMap<String, Boolean> action_assign = 
				action != null ? action.getFactoredAction() : null;
		
		final List<ADDRNode> rewards = parameters.get_rewards();
		final List<ADDRNode> source = depth == parameters.get_valueDD().length-1 ? 
				parameters.get_max_rewards() : parameters.get_rewards();

		ADDRNode ret = manager.DD_ONE; 
		for( final ADDRNode rew :  source ){
			final ADDRNode gen_path = 
					generalize( rew, parameters.get_genRule(), manager, state_assign, 
							action_assign );
			ret = manager.BDDIntersection(gen_path, ret);
		}
		
		return ret;//generalize(sum, parameters.get_genRule(), manager, state_assign, action_assign );
				
	}

	@Override
	public ADDRNode generalize_action(
			FactoredState<RDDLFactoredStateSpace> state,
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
			FactoredState<RDDLFactoredStateSpace> next_state,
			RewardGeneralizationParameters parameters, int depth) {
		return parameters.get_manager().DD_ONE;
	}


}
