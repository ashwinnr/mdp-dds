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
		 //get path of the state
		 
		 NavigableMap<String, Boolean> state_path = state.getFactoredState();
		 ADDRNode output_path = null;
		 switch( parameters.get_genRule() ){
		 case  PATH : output_path = manager.get_path(parameters.get_max_rewards()
				 , state_path);
		 	break;
		 case ALL_PATHS : 
			 for( final ADDRNode rew : parameters.get_max_rewards() ){
				 final ADDRNode this_r = manager.evaluate(rew, state_path);
				 final ADDRNode this_paths = manager.all_paths_to_leaf(rew, this_r);
				 output_path = manager.BDDUnion(output_path == null ?
						 manager.DD_ZERO : output_path, this_paths );
			 }
			 break;
		 default :
			 try{
				 throw new Exception("not implemented reward gen type");
			 }catch(Exception e ){
				 e.printStackTrace();
				 System.exit(1);
			 }
		 }
		 
		 return output_path;
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
