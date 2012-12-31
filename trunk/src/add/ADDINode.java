package add;

import graph.Graph;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

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
	private static final int HASH_INIT_NEG = 19;
	private static final int HASH_MULT_NEG = 29;
	private int _nNegHash;

	@Override
	//returns soft reference to itself
	public MySoftReference<ADDINode> getNullDD() {
		this.children = new UniPair<ADDRNode>( new ADDRNode(null), new ADDRNode(null) );
		this.testVariable = "".intern();
		return new MySoftReference<ADDINode>(this);
	}

	@Override
	public ADDINode plugIn(final String testVar, final UniPair< ADDRNode> child) throws Exception {

//		if( child.isDistinct() ){
//			throw new Exception("attempt to construct INode with identical children. Not reduced!");
//		}
		this.testVariable = testVar;//.intern();
		children = child;
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
	
	public void swapChildren(){
		hashSet = false;
		this.children.swap();
	}

	@Override
	public void setHashCode(int numb) {
		_nHashCode = numb;
		hashSet = true;
	}

	public int hashCode() {
		
		if( hashSet ){
			return _nHashCode;
		}

		int h0 = testVariable.hashCode();
		int h_true = children._o1.hashCode();
		int h_false = children._o2.hashCode();
		
		HashCodeBuilder hb = new HashCodeBuilder( HASH_INIT, HASH_MULT );
		_nHashCode = hb.append(h0).append( h_true + h_false ).append( h_false ).hashCode();

		hb = null;
		hb = new HashCodeBuilder( HASH_INIT_NEG, HASH_MULT_NEG );
		_nNegHash = hb.append(h0).append(h_false + h_true ).append(h_true).hashCode();

//		System.out.println( "Inode hashcode : " + this + " " + this.children + 
//				" " + h0 + " " + h_true + " " + h_false 
//				+ " " + _nHashCode + " " + _nNegHash );
		hashSet = true;
		return _nHashCode;
		
	}

	public int getNegatedHashCode() {
		if( !hashSet ){
			hashCode();
		}
		return _nNegHash;
	}

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

	public ADDINode getNegatedNode() {
		try {
			ADDINode ret = new ADDINode();
			ret.plugIn(testVariable, new UniPair<ADDRNode>( children._o2, children._o1 ) );
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	

}
