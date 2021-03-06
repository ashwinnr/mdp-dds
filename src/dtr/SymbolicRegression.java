package dtr;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import add.ADDRNode;

import util.UnorderedPair;

import dd.DDINode;
import dd.DDLeaf;
import dd.DDNode;
import dd.DDRNode;
import dd.DDManager.APPROX_TYPE;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredStateSpace;

public interface SymbolicRegression<D extends DDNode, DR extends DDRNode<D>,
DI extends DDINode<D,DR,? extends Collection<DR> >, 
		DL extends DDLeaf<?>, S extends FactoredStateSpace, 
		A extends FactoredActionSpace<S> > {

	public <T extends SymbolicValueFunction<D,DR,DI,DL,S,A>,
			U extends SymbolicPolicy<D,DR,DI,DL,S,A> > UnorderedPair<T,U>
		regress( final DR input, final boolean withActionVars, final boolean keepQ, 
				final boolean makePolicy , final boolean constraint_naively,
				final List<Long> size_change,
				final boolean do_apricodd,
				final double apricodd_epsilon,
				final APPROX_TYPE apricodd_type );
	
	public DR 
		applyMDPConstraints( final DR input, NavigableMap<String, Boolean> action, DR violate,
				final boolean constraint_naively , final List<Long> size_change );
	
	public DR
		regressAction( final DR primed, final NavigableMap<String, Boolean> action,
				final boolean constraint_naively, final List<Long> size_change , 
				final boolean do_apricodd, 
				final double apricodd_epsilon,
				final APPROX_TYPE apricodd_type );
	
	public DR
		regressPolicy( final DR initial_value_func, final DR policy, final boolean withActionVars ,
				final boolean constraint_naively, final List<Long> size_change , 
				final boolean do_apricodd,
				final double apricodd_epsilon,
				final APPROX_TYPE apricodd_type );
	
	public UnorderedPair<DR, Integer>
		evaluatePolicy( final DR initial_value_func, final DR policy, final int nSteps, 
			final double epsilon, final boolean withActionVars ,
			final boolean constraint_naively, final List<Long> size_change ,
			final boolean do_apricodd, 
			final double apricodd_epsilon,
			final APPROX_TYPE apricodd_type );
	
}
