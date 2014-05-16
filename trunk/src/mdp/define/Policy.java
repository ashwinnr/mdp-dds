package mdp.define;

import java.util.Random;

public interface Policy<S extends StateSpace, A extends ActionSpace<S>> {
	public <T extends State<S>, U extends Action<S,A>> U getAction( T state );
	public PolicyStatistics executePolicy( final int numRounds, final int numStates, 
			final boolean useDiscounting, final int horizon, final double discount,
			final Random initialStateRandom,
			final Random stateTransitionRandom, 
			final Random rewardSampleRandom );
}
