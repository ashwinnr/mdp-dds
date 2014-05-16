package factored.mdp.define;

import java.util.Random;

import rddl.mdp.RDDLFactoredStateSpace;
import mdp.define.State;
import mdp.define.Transition;

public interface FactoredTransition<S extends FactoredStateSpace, A extends FactoredActionSpace<S> >
	extends Transition<S,A> {
	
	public FactoredState<S> sampleFactored( FactoredState<S> state, FactoredAction<S,A> action,
			final Random rand );

}
