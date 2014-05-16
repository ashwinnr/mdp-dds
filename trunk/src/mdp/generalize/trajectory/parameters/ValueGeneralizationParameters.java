package mdp.generalize.trajectory.parameters;

import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;

import dd.DDManager.APPROX_TYPE;
import dd.DDManager.DDOper;
import add.ADDLeaf;
import add.ADDManager;
import add.ADDRNode;
import mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH;
import mdp.generalize.trajectory.type.ValueGeneralizationType;

public class ValueGeneralizationParameters extends GeneralizationParameters<ValueGeneralizationType> {
	
	public ValueGeneralizationParameters(
			ADDManager _manager,
			mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH _genRule,
			final boolean constrain_naively ){
		super(_manager, _genRule, constrain_naively );
	}

}
