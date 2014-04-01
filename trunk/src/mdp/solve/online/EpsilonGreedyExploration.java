package mdp.solve.online;

import java.util.Random;

import add.ADDRNode;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredState;
import factored.mdp.define.FactoredStateSpace;

public class EpsilonGreedyExploration<S extends FactoredStateSpace, A extends FactoredActionSpace<S> > implements Exploration<S,A> {

    protected double epsilon = 0;
    protected Random _rand;
    protected double alpha = 0;
    
    public EpsilonGreedyExploration(final double epsilon, final long seed , final double alpha ) {
	this.epsilon =  epsilon;
	_rand = new Random( seed );
	this.alpha = alpha;
    }
    
    @Override
    public boolean is_explore(FactoredState<S> state,
	    FactoredAction<S,A> action, ADDRNode value_fn, ADDRNode policy_fn) {
	final double rand = _rand.nextDouble();
	final boolean ret = rand < epsilon;
	epsilon = epsilon * alpha;
	return ret; 
    }

}
