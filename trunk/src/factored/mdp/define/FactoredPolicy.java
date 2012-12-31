package factored.mdp.define;

import mdp.define.Policy;

public interface FactoredPolicy<S extends FactoredStateSpace, A extends FactoredActionSpace<S>>
	extends Policy<S,A>{
	public <T extends FactoredState<S>, U extends FactoredAction<S,A>> U getFactoredAction(T state);
}
