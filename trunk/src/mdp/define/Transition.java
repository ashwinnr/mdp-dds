package mdp.define;

public interface Transition<S extends StateSpace, A extends ActionSpace<S>> {
	public State<S> sample(final State<S> state, final Action<S,A> action);
	public State<S> randomState();
}
