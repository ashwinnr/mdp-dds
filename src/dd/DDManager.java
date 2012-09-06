package dd;

import java.util.Collection;
import java.util.logging.Logger;

import org.apache.commons.collections.map.ReferenceMap;

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
	
	public void addPermenant( final D input );
	
	public D getOneNullDD( final boolean leaf );
	
	public void addToStore( final int soManyDDs,  final boolean leaf, final boolean node );
	
	public void nullifyDD( final D input );
	
	public void flushCaches();
	
}
