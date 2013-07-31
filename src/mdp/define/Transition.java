package mdp.define;

public interface Transition<S extends StateSpace, A extends ActionSpace<S>> {
	public <T extends State<S>, U extends Action<S,A>> 
		T sample(final T state, final U action);
	public <T extends State<S>> T randomState();
}
