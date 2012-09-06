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


	@Override
	public MySoftReference<ADDINode> getNullDD() {
		
		this.children = new UniPair<ADDRNode>( new ADDRNode(), new ADDRNode() );
		
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

}
