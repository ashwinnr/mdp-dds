package mdp.define;

public interface Reward<S extends StateSpace, A extends ActionSpace<S>> {
	public <T extends State<S>, U extends Action<S,A>> double sample( T state, U action );
}
