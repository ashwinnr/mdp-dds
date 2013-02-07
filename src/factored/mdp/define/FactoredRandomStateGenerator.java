package factored.mdp.define;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_NAME;
import rddl.mdp.RDDLConstrainedMDP;
import util.UnorderedPair;

import mdp.define.RandomStateGenerator;
import mdp.define.State;

public class FactoredRandomStateGenerator<S extends FactoredStateSpace> 
	implements RandomStateGenerator<S>{

	private static Set<String> _stateVars;
	private Random _rand;

	public FactoredRandomStateGenerator(S stateSpace, final long seed ) {
		_stateVars = stateSpace.getStateVariables();
		_rand = new Random( seed );
	}
	
	@Override
	public State<S> randomState() {
		final NavigableMap<String, Boolean> ret = new TreeMap<String, Boolean>();
		for( final String s : _stateVars ){
			final boolean value = _rand.nextBoolean();
			ret.put( s, value );
		}
		FactoredState<S> fas = new FactoredState<S>();
		fas.setFactoredState(ret);
		return fas;
	}
	
}
