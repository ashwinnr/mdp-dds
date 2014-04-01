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
	protected double[] epsilons_t = null;
	protected APPROX_TYPE apricodd_type = null;
	
	public ValueGeneralizationParameters set_genRule(GENERALIZE_PATH _genRule) {
		this._genRule = _genRule;
		return this;
	}

	public ValueGeneralizationParameters(
			ADDManager _manager,
			mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH _genRule,
			Random _rand, double[] epsilons_t, ADDRNode[] value_fn,
			APPROX_TYPE apricodd_type, int num_states) {
		super(_manager, _genRule, _rand);
		this.epsilons_t = epsilons_t;
		this.apricodd_type = apricodd_type;
	}

	public APPROX_TYPE getApricodd_type() {
		return apricodd_type;
	}
	
	public void setApricodd_type(APPROX_TYPE apricodd_type) {
		this.apricodd_type = apricodd_type;
	}
	
	public GeneralizationParameters<ValueGeneralizationType> setEpsilons_t(final double[] finalEpsilons_t) {
		epsilons_t = finalEpsilons_t;
		return this;
	}
	
	public double[] getEpsilons_t() {
		return epsilons_t;
	}
	
}
