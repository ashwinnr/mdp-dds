package mdp.define;

public abstract class Action<S extends StateSpace, A extends ActionSpace<S>> {
	public abstract int hashCode();
}
