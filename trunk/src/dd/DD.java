/*
 * 
 * Defines the math for a DD kind.
 * 
 * Interface for any DD. Must support basic operations - reduce, apply, opOut, approximate, 
 * showGraph, , , restrict, evaluate, remapVars, 
 *, negate, scalar operations, count nodes, get nodes, get vars, 
 * unroll to table, comapre DDs,  
 * 
 *  push to implementing class variable handling : varname <-> ID
 * get/set min/max, invert
 *   
 * 
 *  
 */

package dd;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import util.Pair;

public interface DD<D extends DDNode, DR extends DDRNode<D>,
	DI extends DDINode<D, DR, ? extends Collection<DR> >, 
			DL extends DDLeaf<?>, 
			M extends DDManager<D, DR, DI, DL> > {
	
	public enum DDOper{
		ARITH_PLUS, ARITH_MINUS, ARITH_PROD, ARITH_DIV
	};
	
	public enum APPROX_TYPE{
		LOWER, UPPER, AVERAGE, RANGE
	};

	
	//@returns reduced(canonical) DD 
	public DR reduce(DR input);
	
	public DR apply(DR inputA, DR inputB, DDOper oper);
	
	public DR opOut(DR input, DDOper oper);
	
	public DR approximate(DR input, double epsilon, APPROX_TYPE type);
	
	public DR restrict(DR input);
	
	public DR remapVars(DR input, Map<?,?> remap); //remaps key.toString() to value.toString()
	
	public DR evaluate(DR input, Map<?, Boolean> remap); //evaluates key.toString() to boolean
	
	public DR scalarOp(DR input, DDOper op, double scalar);
	
	public Pair<Integer, Integer> countLeaves(DR input);//returns <internal node count, leaf count>
	
//	public Set<DR> getNodes(DR input);
	
	public Set<String> getVars(DR input);
	
	public void showTable(DR input);
	
	public DR enumeratePaths(DR input);
	
	public boolean compare(DR input1, DR input2);
	
	//all operations seem to be defined by 2. node check. 3. recursive calls.
	//4. merge 5. some operation of leaf and inode
	
	//this method must traverse a DD in some order
	//use recurse? to recurse or not
	//recursive traverse children
	//use merge() to get result
	
	public DR doUnaryOp( DR input, DDOperation<D,DR,DI,DL,M> leafOp );
	
}
