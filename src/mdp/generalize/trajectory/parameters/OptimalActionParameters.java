package mdp.generalize.trajectory.parameters;

import java.util.Random;

import add.ADDManager;
import add.ADDRNode;
import mdp.generalize.trajectory.type.OptimalActionType;

public class OptimalActionParameters extends GeneralizationParameters<OptimalActionType>{

	protected boolean all_depth = false;
	
	public OptimalActionParameters(
			ADDManager _manager,
			mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH _genRule,
			Random _rand, ADDRNode[] policies, boolean all_depth) {
		super(_manager, _genRule, _rand);
		this.all_depth = all_depth;
	}


	public OptimalActionParameters setAll_depth(boolean all_depth) {
		this.all_depth = all_depth;
		return this;
	}
	
	public boolean getAll_depth(){
		return all_depth;
	}
	
}
