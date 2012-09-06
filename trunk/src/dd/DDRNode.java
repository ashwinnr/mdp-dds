package dd;

import graph.Graph;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;


/*
 * Reference to a DDNode
 * Handles negated edges
 * 
 * Double negation has to 
 * done by reduction 
 * 
 * All computation is in terms  of DDNode this class is only for user interface and handling negation
 * 
 */

public abstract class DDRNode<D extends DDNode>{
	
	private static int NOT_SIZE = 5;

	private static final int HASH_MULT = 23;

	private static final int HASH_INIT = 51;

	protected  Boolean negated = null;
	
	protected D theNode = null;
	
	public void makeNegated(){
		
		int negHash = getNegatedHashCode();
		
		negated = !negated;
		
		_nHash = negHash;
		
	}
	
	protected Number _nHash = null;
			
	//reduces underlying DDNode
	//then takes care of double negation
//	protected abstract DDRNode<D> reduce();
	
	@Override
	public int hashCode(){
		
		if( _nHash == null ){
			
			int res = HASH_INIT;
			
			res = theNode.hashCode() + res*HASH_MULT;
			
			res = res*HASH_MULT + (negated ? 1 : 0);
		
			_nHash = new Integer(res); 
			
			return res;
			
		}else{
			
			return _nHash.intValue();
			
		}
		
	}
	
	public String toGraph(Graph g){
		//call toGraph on node
		//put circle on edge in to it
		
		String root = this.theNode.toGraph(g);
		
		if( negated ){
		
			String circleNode = "circle to " + root;
			
			g.addNode(circleNode, NOT_SIZE);
			g.addNodeLabel(circleNode, "!");
			g.addNodeStyle(circleNode, "diamond");
			g.addNodeColor(circleNode, "plum");
			
			g.addUniLink(circleNode, root, "black", "solid", "");
			
			return circleNode;
			
		}else{
			return root;
		}
		
	}

	public int getNegatedHashCode(){
		
		if( _nHash == null ){
			_nHash = new Integer( this.hashCode() );
			
		}
		
		return ( ( _nHash.intValue() - (negated ? 1 : 0 ) ) / HASH_MULT );
		
	}

	@Override
	public boolean equals(Object obj) {
		
		if( obj instanceof DDRNode ){
			
			DDRNode<D> thing = (DDRNode<D>)obj;
			
			return this.negated.equals(this.negated) && this.theNode.equals(thing.theNode);
			
		}
		
		return false;
		
		
	}

	public D getNode() {
		return theNode;
	}
	
	private DDRNode<D> plugin(final D aNode) {
		negated = false;
		this.theNode = aNode;
		return this;
	}
	
}
