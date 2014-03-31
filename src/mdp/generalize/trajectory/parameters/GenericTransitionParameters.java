package mdp.generalize.trajectory.parameters;

import mdp.generalize.trajectory.type.GenericTransitionType;

public class GenericTransitionParameters extends GeneralizationParameters<GenericTransitionType> {
	protected boolean fix_start_state;
	protected boolean fix_next_state;
	protected boolean fix_action;
	protected int num_next_states;
	protected int num_actions;
	protected int num_states;
	
	public void setFix_action(final boolean fix_action) {
		this.fix_action = fix_action;
	}
	public void setFix_next_state(final boolean fix_next_state) {
		this.fix_next_state = fix_next_state;
	}
	public void setFix_start_state(final boolean fix_start_state) {
		this.fix_start_state = fix_start_state;
	}
	public void setNum_actions(final int num_actions) {
		this.num_actions = num_actions;
	}
	public void setNum_next_states(final int num_next_states) {
		this.num_next_states = num_next_states;
	}
}
