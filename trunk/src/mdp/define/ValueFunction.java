package mdp.define;

import java.util.NavigableMap;

import util.Pair;

public interface ValueFunction<S extends StateSpace, A extends ActionSpace<S>> {

	public <T extends State<S>> Double 
		getStateValue( T state );//upper, lower bound
	
	//upper, lower bound
	public <T extends State<S>, U extends Action<S,A> >
		Double getStateActionValue( T state, U action );
	
}
