package mdp.generalize.trajectory.parameters;

import java.util.NavigableMap;
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
	protected ADDRNode[] value_fn = null;
	protected APPROX_TYPE apricodd_type = null;
	protected int num_states = -1;
	
	public ValueGeneralizationParameters set_genRule(GENERALIZE_PATH _genRule) {
		this._genRule = _genRule;
		return this;
	}
	public ValueGeneralizationParameters set_manager(ADDManager _manager) {
		this._manager = _manager;
		return this;
	}
	
	public GeneralizationParameters<ValueGeneralizationType> setNum_states(int num_states) {
		this.num_states = num_states;
		return this;
	}
	
	public int getNum_states() {
		return num_states;
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
	
	public GeneralizationParameters<ValueGeneralizationType> setValue_fn(final ADDRNode[] value_fn) {
		this.value_fn = value_fn;
		return this;
	}
	
	public double[] getEpsilons_t() {
		return epsilons_t;
	}
	
	public ADDRNode[] getValue_fn() {
		return value_fn;
	}
	
}
