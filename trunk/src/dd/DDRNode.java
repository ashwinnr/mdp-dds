package dd;

import graph.Graph;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import add.ADDLeaf;


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
	
//	private static final int HASH_MULT = 23;
//
//	private static final int HASH_INIT = 51;

//	protected  boolean negated = false;
	
	protected D theNode = null;
	
	public String getTestVariable() {
		
		if( this.theNode instanceof DDINode ){
			return ((DDINode)this.theNode).getTestVariable();
		}else{
			try {
				throw new Exception("get testvariable called on leaf");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			
		}
		
		return null;
		
	}
	

	public double getMax() {
		return theNode.getMax();
	}

	public double getMin() {
		return theNode.getMin();
	}

//	public void makeNegated(){
//		
//		int negHash = getNegatedHashCode();
//		
//		negated = !negated;
//		
//		_nHash = negHash;
//		
//	}
//	protected int _nHash;
//	protected int _nNegHash;
//	protected boolean hashSet = false;
//	protected abstract DDRNode<D> reduce();
//	public abstract int getComplementedHash();
	
	public abstract int hashCode();
	
	public String toGraph(Graph g){
		//call toGraph on node
		//put circle on edge in to it
		
		String root = this.theNode.toGraph(g);
//		String circleNode = root;
//		String circleNode;// = ( negated ) ? "not circle to " : "circle to ";
//		circleNode = circleNode + root;
//		
//		g.addNode(circleNode);
//		, NOT_SIZE);
		
//		if( negated ){
//			g.addNodeLabel(circleNode, "!");
//			g.addNodeStyle(circleNode, "diamond");
//			g.addNodeColor(circleNode, "plum");
			
//		}else{
//			g.addNodeLabel(circleNode, "");
//		}
		
//		g.addUniLink(circleNode, root, "black", "solid", "");
		
		return root;
		
	}

	@Override
	public String toString() {
		return // ( negated ? "! " : "" ) + 
				theNode.toString();
	}

//	public int getNegatedHashCode(){
//		
//		if( !hashSet ){
//			this.hashCode();
//		}
//		
//		return ( ( _nHash - (negated ? 1 : 0 ) ) / HASH_MULT );
//		
//	}

	@Override
	public boolean equals(Object obj) {
		
		if( obj instanceof DDRNode ){
			
			DDRNode<D> thing = (DDRNode<D>)obj;
			
			return this.theNode.equals(thing.theNode);//this.negated == (this.negated) && 
			
		}
		
		return false;
		
		
	}

	public D getNode() {
		return theNode;
	}
	
	public DDRNode<D> plugin(final D aNode) {
//		negated = false;
		this.theNode = aNode;
		return this;
	}
	
//	public void negate(){
//
//		
//		if( theNode instanceof ADDLeaf ){
//			System.err.println("warning : negating a leaf.");
//		}
//		
//		this.negated = !this.negated;
//		
//	}

//	public boolean isNegated() {
//		return negated;
//	}
//
//	public void setNegated(boolean negated) {
//		this.negated = negated;
//	}

	public void nullify() {
		this.theNode = null;
		
	}
	
	
	
}
