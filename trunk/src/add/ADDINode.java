package add;

import graph.Graph;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import util.MySoftReference;
import util.Pair;
import util.UniPair;

import dd.DDINode;
import dd.DDNode;
import dd.DDRNode;

public class ADDINode extends DDINode<ADDNode, ADDRNode, UniPair<ADDRNode> > implements ADDNode, Comparable<ADDINode> {


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

		if( child.isDistinct() ){
			throw new Exception("attempt to construct INode with identical children. Not reduced!");
		}
		
		this.testVariable = testVar.intern();
		
		this.children.clear();
		
		this.children.addAll(child);
		
		this._nHashCode = hashCode();
		
		return this;
	
	}

	@Override
	public String toGraph(Graph g) {

		String str = "Inode " + hashCode();
		
		g.addNodeLabel(str, this.testVariable);
		
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
		
		int res = HASH_INIT;
		
		int h1 = children._o1.hashCode();
		
		int h2 = children._o2.hashCode();
		
		int h12 = h1*h1, h22 = h2*h2;
		
		//m^5 s + m^4 h1^2 + m^3 h1 + m^2 h2^2 + m h2 
		
		res = ( res << HASH_SHIFT - res) + h12;
		
		res = ( res << HASH_SHIFT - res) + h1;
		
		res = ( res << HASH_SHIFT - res ) + h22;
		
		res = ( res << HASH_SHIFT - res) + h2;
		
		_nHashCode = res;
		
		hashSet = true;
		
		int negHash = HASH_INIT;
		
		negHash = ( negHash << HASH_SHIFT - negHash ) + h22;
		
		negHash = ( negHash << HASH_SHIFT - negHash ) + h2;
		
		negHash = ( negHash << HASH_SHIFT - negHash ) + h12;
		
		negHash = ( negHash << HASH_SHIFT - negHash ) + h1;
		
		_nNegHash = negHash;
		
		return res;
		
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
	

}
