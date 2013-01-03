package dd;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

/*
 * interface for DD memory management
 * must implement
 * -object store of DDs
 * 	-set of soft references(when something is removed, remove it from cache)
 * 
 * -maps with weak references to DDs 
 * 		-reduce : weak-weak
 * 		-apply : weak, weak, op -> weak
 * 		-approx : weak -> weak
 * 		-align cache : weak -> weak
 *		-constraint cache : weak-> weak
 * 
 * -strong references to permenants
 * 
 * -methods for adding permenants
 * -provision for temp saves
 *  
 * tree creation
 * 
 *  flushcaches
 * 
 * create null DD,
 * 
 * use a reference queue and a set of ADDNodes
 * when an ADDNode is deleted, remove entries from cache
 * 
 */

public interface DDManager<D extends DDNode, DR extends DDRNode<D>,
		DI extends DDINode<D,DR,? extends Collection<DR> >, 
		DL extends DDLeaf<?> > {
	
	public void createStore( final int NumDDs  );
	
	public void addPermenant( final DR... input );
	
	public D getOneNullDD( final boolean leaf );
	
	public void addToStore( final int soManyDDs,  final boolean leaf, final boolean node );
	
	public void nullifyDD( final D input );
	
	public void flushCaches(boolean clearDeadMaps);
	
	public DR restrict( final DR input, final String var, final boolean assign );
	
	public DR marginalize( final DR input, final String var, final DDMarginalize oper);
	
	public enum DDOper{
		ARITH_PLUS, ARITH_MINUS, ARITH_PROD, ARITH_DIV, ARITH_MAX, ARITH_MIN
	};
	
	public enum DDMarginalize{
		MARGINALIZE_SUM, MARGINALIZE_MAX
	};
	
	public enum APPROX_TYPE{
		LOWER, UPPER, AVERAGE, RANGE
	};

	public DR approximate(DR input, double epsilon, APPROX_TYPE type);
	
	public DR remapVars(DR input, Map<String, String> remap); //remaps key.toString() to value.toString()
	
	public DR evaluate(DR input, Map<?, Boolean> remap); //evaluates key.toString() to boolean
	
	public Set<DR> getNodes(DR input);
	
	public Set<DL> getLeaves(DR input);
	
	public Set<String> getVars(DR input);
	
	public List<NavigableMap<String,Boolean>> enumeratePaths(DR input, boolean leaf,
			 final boolean leafValSpecified, 
				final double leafVal);
	
	public boolean compare(DR input1, DR input2);
	
	public DR scalarMultiply( DR input, double scalar );
	
	public void showGraph( final DR... input );

	public boolean removePermenant( final DR... arr);

	NavigableMap<String, Boolean> findFirstOneLeaf(DR input);
	
}
