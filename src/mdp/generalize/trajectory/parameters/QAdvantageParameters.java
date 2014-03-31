package mdp.generalize.trajectory.parameters;

import mdp.generalize.trajectory.type.OptimalActionType;
import mdp.generalize.trajectory.type.QAdvantageType;

public class QAdvantageParameters  implements GeneralizationParameters<QAdvantageType>{
	protected double delta;
	public void setDelta(double delta) {
		this.delta = delta;
	}
	public double getDelta() {
		return delta;
	}
}
