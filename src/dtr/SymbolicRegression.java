package dtr;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;

import add.ADDRNode;

import util.UnorderedPair;

import dd.DDINode;
import dd.DDLeaf;
import dd.DDNode;
import dd.DDRNode;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredStateSpace;

public interface SymbolicRegression<D extends DDNode, DR extends DDRNode<D>,
DI extends DDINode<D,DR,? extends Collection<DR> >, 
		DL extends DDLeaf<?>, S extends FactoredStateSpace, 
		A extends FactoredActionSpace<S> > {

	public <T extends SymbolicValueFunction<D,DR,DI,DL,S,A>,
			U extends SymbolicPolicy<D,DR,DI,DL,S,A> > UnorderedPair<T,U>
		regress( final DR input, final boolean withActionVars, final boolean keepQ, 
				final boolean makePolicy , final boolean constraint_naively );
	
	public DR 
		applyMDPConstraints( final DR input, NavigableMap<String, Boolean> action, DR violate,
				final boolean constraint_naively );
	
	public DR
		regressAction( final DR primed, final NavigableMap<String, Boolean> action,
				final boolean constraint_naively );
	
	public DR
		regressPolicy( final DR initial_value_func, final DR policy, final boolean withActionVars ,
				final boolean constraint_naively );
	
	public UnorderedPair<DR, Integer>
		evaluatePolicy( final DR initial_value_func, final DR policy, final int nSteps, 
			final double epsilon, final boolean withActionVars ,
			final boolean constraint_naively );
	
}
