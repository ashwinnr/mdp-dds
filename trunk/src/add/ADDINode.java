package add;

import graph.Graph;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import util.MySoftReference;
import util.Pair;
import util.UniPair;

import dd.DDINode;
import dd.DDNode;
import dd.DDRNode;

public class ADDINode extends DDINode<ADDNode, ADDRNode, UniPair<ADDRNode> > 
	implements ADDNode, Comparable<ADDINode> {


//	private static final int HASH_PRIME_CHILD = 29;
	private static final int HASH_INIT = 17;
//	private static final int HASH_PRIME_TRUE = 23;
	private static final int HASH_MULT = 31;
//	private static final int HASH_INIT_NEG = 19;
//	private static final int HASH_MULT_NEG = 29;
//	private int _nNegHash;
//	private static long INODE_COUNTER = 0;
//	private long INODE_ID = 0;

	@Override
	//returns soft reference to itself
	public MySoftReference<ADDINode> getNullDD() {
		this.children = new UniPair<ADDRNode>( new ADDRNode(null), new ADDRNode(null) );
		this.testVariable = null;//"";//.intern();
		return new MySoftReference<ADDINode>(this);
	}

	@Override
	public ADDINode plugIn(final String testVar, final ADDRNode true_child,
			final ADDRNode false_child ) throws Exception {

		if( true_child.equals( false_child ) ){
			throw new Exception("attempt to construct INode with identical children. Not reduced!");
		}
		this.testVariable = testVar;//.intern();
		children._o1 = true_child;
		children._o2 = false_child;
//		this._nHashCode = hashCode();
		return this;
	
	}

	@Override
	public String toGraph(Graph g) {

		String str = "Inode " + hashCode();
		
		g.addNodeLabel(str, this.testVariable + " ( " + 
				getMin() + ", " + getMax() + ")" );
		
		ADDRNode trueNode = children._o1;

		String trueGraph = trueNode.toGraph(g);
		
		g.addUniLink(str, trueGraph, "black", "solid", "");
		
		ADDRNode falseNode = children._o2;
		
		String falseGraph = falseNode.toGraph(g);
		
		g.addUniLink(str, falseGraph, "black", "dashed", "");
		
		if( trueGraph.equals( falseGraph ) ){
			System.err.println("unreduced DD");
			System.err.println("Hashcodes " + trueNode.hashCode() + " " 
					+ falseNode.hashCode() );
			System.err.println("Equals " + trueNode.equals( falseNode ) );		
			System.exit(1);
		}
		
		return str;
				
	}

	@Override
	public int compareTo(ADDINode o) {
		
		if( o instanceof ADDINode ){
			
			ADDINode inode = (ADDINode)(o);  
			int res1 = this.testVariable.compareTo( inode.testVariable );
			if( res1 != 0 ){
				return res1;
			}else{
				return this.children.compareTo(inode.children);
			}
			
		}
		
		try{
			throw new UnsupportedOperationException("ADD inode comparing to not one " + o.getClass().getName() );
		}catch( UnsupportedOperationException e ){
			e.printStackTrace();
			LOGGER.severe(e.getMessage());
		}
		
		return -1;
		
	}
	
//	public void swapChildren(){
//		hashSet = false;
//		this.children.swap();
//	}

	@Override
	public boolean equals(Object obj) {
		if( obj instanceof ADDINode ){
			ADDINode thing = (ADDINode)obj;
			//internalized, so use ==
			//WARNING : internalization
			final boolean var_equals = this.testVariable.equals( thing.testVariable );
			final boolean true_id_equals =
					getTrueChild().getID() == thing.getTrueChild().getID(); 
			final boolean false_id_equals =
					getFalseChild().getID() == thing.getFalseChild().getID();
//			System.out.println( var_equals + " " + true_id_equals + " " 
//					+ false_id_equals );
			return var_equals 
					&& true_id_equals && false_id_equals;
		}
		return false;
	}

	public int hashCode() {
		HashCodeBuilder hb = new HashCodeBuilder( HASH_INIT, HASH_MULT );
		long true_id = getTrueChild().getID();
		long false_id = getFalseChild().getID();
		return hb.append( testVariable ).append( true_id + false_id ).
			append( false_id ).hashCode();
		
//		if( hashSet ){
//			return _nHashCode;
//		}
//
//		int h0 = testVariable.hashCode();
//		int h_true = children._o1.hashCode();
//		int h_false = children._o2.hashCode();
//		
//		HashCodeBuilder hb = new HashCodeBuilder( HASH_INIT, HASH_MULT );
//		_nHashCode = hb.append(h0).append( h_true + h_false ).append( h_false ).hashCode();
//
//		hb = null;
//		hb = new HashCodeBuilder( HASH_INIT_NEG, HASH_MULT_NEG );
//		_nNegHash = hb.append(h0).append(h_false + h_true ).append(h_true).
//				append( h_true*h_true ).append( h_false*h_true ).hashCode();
//
////		System.out.println( "Inode hashcode : " + this + " " + this.children + 
////				" " + h0 + " " + h_true + " " + h_false 
////				+ " " + _nHashCode + " " + _nNegHash );
//		hashSet = true;
//		return _nHashCode;
		
	}

//	public int getNegatedHashCode() {
//		if( !hashSet ){
//			hashCode();
//		}
//		return _nNegHash;
//	}

	public ADDRNode getTrueChild() {
		return this.children._o1;
	}
	
	public ADDRNode getFalseChild(){
		return this.children._o2;
	}

	public boolean negatedEquals(ADDINode other) {
		return this.testVariable == other.testVariable && 
				this.children._o1.equals( other.children._o2 )
				&& this.children._o2.equals( other.children._o1 );
	}

	public ADDINode getNegatedNode( final ADDINode null_inode ) {
		Objects.requireNonNull( null_inode );
		try {
			null_inode.plugIn(testVariable, children._o2, children._o1 );
			return null_inode;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
}
