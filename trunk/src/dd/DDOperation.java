package dd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import util.UnorderedPair;

//this class defines the structure of common unary and binary DD operations
//implementing classes must define recurse, split and merge methods
//implementing classes must take care of caching


public abstract class DDOperation<D extends DDNode, DR extends DDRNode<D>,
DI extends DDINode<D, DR, ? extends Collection<DR> >, 
		DL extends DDLeaf<?>, 
		DM extends DDManager<D, DR, DI, DL> > {

	public abstract boolean recurse( DR node );
	
//	public abstract DR merge( List<DR> nodes );
	
	public DR doUnaryOp( DR node ){
		
		//recurse also checs cache
		if( recurse(node) ){
			
			DI n = ((DI)(node.getNode()));
			
			Iterator<DR> list = n.children.iterator();
			
			List<DR> newlist = new ArrayList<DR>();
			
			while( list.hasNext() ){
				DR res = doUnaryOp(list.next());
				if( res != null ){
					newlist.add( res );
				}
			}
			
			return merge( ((DDINode)(node.getNode())).testVariable, node.isNegated(), newlist);
			
		}
		
		return finishUpLeaf( node );
		
	}
	
	public abstract DR finishUpLeaf( DR node );
	
	public abstract boolean recurse( DR node1, DR node2 );
	
	//to apply DR to every DR in list<DR>
	//and merge under String
	public abstract UnorderedPair<UnorderedPair<String, Boolean>, 
		UnorderedPair< Iterator<DR>,Iterator<DR> > > split( DR node1, DR node2 ); 
	
	//binaryOp - split must return two iterators 
	public DR doBinaryOp( DR node1, DR node2 ){
		
		if( recurse(node1, node2 ) ){
			
			UnorderedPair<UnorderedPair<String, Boolean>, UnorderedPair< Iterator<DR>,Iterator<DR> > > splots = 
					split( node1, node2 );
			
			List<DR> newlist = null;
			
			if( splots != null ){
				
				newlist = new ArrayList<DR>();
				
				DR one, two;
				
				Iterator<DR> i1 = splots._o2._o1, i2 = splots._o2._o2;
				
				while( i1.hasNext() && i2.hasNext() ){

					DR res = doBinaryOp(splots._o2._o1.next(), splots._o2._o2.next() );
					
					if( res != null ){
						newlist.add( res );
					}

				}

				return merge( splots._o1._o1, splots._o1._o2, newlist );
			}
			
		}
		
		return finishUpLeaf( node1, node2 );
		
	}
	
	public abstract DR merge(String _o1, boolean negate, List<DR> newlist);

	public abstract DR finishUpLeaf( DR node1, DR node2 );
	
}
