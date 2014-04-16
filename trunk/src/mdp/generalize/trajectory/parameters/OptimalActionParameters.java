package mdp.generalize.trajectory.parameters;

import java.util.Random;

import add.ADDManager;
import add.ADDRNode;
import mdp.generalize.trajectory.type.OptimalActionType;

public class OptimalActionParameters extends GeneralizationParameters<OptimalActionType>{

	public OptimalActionParameters(
			ADDManager _manager,
			mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH _genRule,
			Random _rand ,
			final boolean constrain_naively ){
		super(_manager, _genRule, _rand, constrain_naively );
	}
}
