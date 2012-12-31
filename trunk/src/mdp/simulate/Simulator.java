package mdp.simulate;

import mdp.define.Action;
import mdp.define.ActionSpace;
import mdp.define.State;
import mdp.define.StateSpace;

public interface Simulator<S extends StateSpace, A extends ActionSpace<S> > {
	//generic interface for mdp simulation
	public State<S> takeAction( State<S> currentState, Action<S,A> action );
	public double getReward( State<S> currentState, Action<S,A> action );
}
