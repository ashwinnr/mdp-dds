package mdp.generalize.trajectory.parameters;

import add.ADDManager;
import add.ADDRNode;
import mdp.generalize.trajectory.type.OptimalActionType;

public class OptimalActionParameters extends GeneralizationParameters<OptimalActionType>{
	protected int num_states;
	protected ADDRNode[] policies;
	protected boolean all_depth = false;
	public ADDManager get_manager() {
		return _manager;
	}
	
	public OptimalActionParameters set_manager(ADDManager _manager) {
		this._manager = _manager;
		return this;
	}

	public OptimalActionParameters setAll_depth(boolean all_depth) {
		this.all_depth = all_depth;
		return this;
	}
	
	public boolean getAll_depth(){
		return all_depth;
	}
	
	public ADDRNode[] getPolicies() {
		return policies;
	}
	
	public OptimalActionParameters setPolicies(ADDRNode[] policies) {
		this.policies = policies;
		return this;
	}
	
	public int getNum_states() {
		return num_states;
	}
	public OptimalActionParameters setNum_states(int num_states) {
		this.num_states = num_states;
		return this;
	}
}
