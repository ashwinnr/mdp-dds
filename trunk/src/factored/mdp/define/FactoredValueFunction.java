package factored.mdp.define;

import util.Pair;
import mdp.define.Action;
import mdp.define.State;
import mdp.define.ValueFunction;

public interface FactoredValueFunction<S extends FactoredStateSpace, 
	A extends FactoredActionSpace<S> > extends ValueFunction<S,A>{

	public <T extends FactoredState<S>> Pair<Double, Double> 
		getFactoredStateValue( T state );//upper, lower bound
	
	//	upper, lower bound
	public <T extends FactoredState<S>, U extends FactoredAction<S,A> >
		Pair<Double, Double> getFactoredStateActionValue( T state, U action );

}
