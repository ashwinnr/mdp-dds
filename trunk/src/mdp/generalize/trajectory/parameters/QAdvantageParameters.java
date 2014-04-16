package mdp.generalize.trajectory.parameters;

import java.util.Random;

import add.ADDManager;
import mdp.generalize.trajectory.type.OptimalActionType;
import mdp.generalize.trajectory.type.QAdvantageType;

public class QAdvantageParameters  extends  GeneralizationParameters<QAdvantageType>{
	protected double delta;
	public void setDelta(double delta) {
		this.delta = delta;
	}

	
	
	public QAdvantageParameters(
			ADDManager _manager,
			mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH _genRule,
			Random _rand, double delta, final boolean constrain_naively ) {
		super(_manager, _genRule, _rand, constrain_naively);
		this.delta = delta;
	}



	public double getDelta() {
		return delta;
	}
}
