package mdp.define;

public interface RandomStateGenerator<S extends StateSpace> {
	public State<S> randomState();
}
