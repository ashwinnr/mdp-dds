package mdp.generalize.trajectory.parameters;

import java.util.Random;

import add.ADDManager;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredStateSpace;
import mdp.generalize.trajectory.Generalization;
import mdp.generalize.trajectory.type.GeneralizationType;
import mdp.generalize.trajectory.type.GenericTransitionType;

public class GenericTransitionParameters< T extends GeneralizationType, P extends GeneralizationParameters<T> ,
	S extends FactoredStateSpace, A extends FactoredActionSpace<S> > extends GeneralizationParameters<GenericTransitionType<T>> {
	protected boolean fix_start_state;
	protected boolean fix_action;

	protected int num_actions;
	protected int num_states;
	
	protected Generalization<S, A, T, P> inner_generalizer;
	protected P inner_parameters;
	
	public GenericTransitionParameters(
		ADDManager _manager,
		mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH _genRule,
		Random _rand, boolean fix_start_state, boolean fix_action,
		int num_actions, int num_states,
		Generalization<S, A, T, P> inner_generalizer, P inner_parameters) {
	    super(_manager, _genRule, _rand);
	    this.fix_start_state = fix_start_state;
	    this.fix_action = fix_action;
	    this.num_actions = num_actions;
	    this.num_states = num_states;
	    this.inner_generalizer = inner_generalizer;
	    this.inner_parameters = inner_parameters;
	}
	public Generalization<S, A, T, P> getGeneralizer() {
	    return this.inner_generalizer;
	}
	public void setGeneralizer(Generalization<S, A, T, P> generalizer) {
	    this.inner_generalizer = generalizer;
	}
	public P getParameters() {
	    return this.inner_parameters;
	}
	public void setParameters(P parameters) {
	    this.inner_parameters = parameters;
	}
	public void setFix_action(final boolean fix_action) {
		this.fix_action = fix_action;
	}
	public void setFix_start_state(final boolean fix_start_state) {
		this.fix_start_state = fix_start_state;
	}
	public void setNum_actions(final int num_actions) {
		this.num_actions = num_actions;
	}
	public void setNum_states(int num_states) {
	    this.num_states = num_states;
	}
	public boolean getFix_start_state() {
	    return this.fix_start_state;
	}
	public boolean getFix_action() {
	    return this.fix_action;
	}
	public int getNum_actions() {
	    return this.num_actions;
	}
	public int getNum_states() {
	    return this.num_states;
	}
	public void set_manager(final ADDManager manager) {
	    this._manager = manager;
	    this.inner_parameters.set_manager( manager );
	}
	
}
