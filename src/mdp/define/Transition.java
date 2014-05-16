package mdp.define;

import java.util.Random;

public interface Transition<S extends StateSpace, A extends ActionSpace<S>> {
	public <T extends State<S>, U extends Action<S,A>> 
		T sample(final T state, final U action, final Random rand );
	public <T extends State<S>> T randomState( final Random rand );
}
