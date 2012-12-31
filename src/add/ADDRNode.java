package add;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import dd.DDNode;
import dd.DDRNode;

public class ADDRNode extends DDRNode<ADDNode> implements Comparable<ADDRNode> {

//	private static final int HASH_SHIFT = 3;
//	private static final int HASH_INIT = 19;
	private static final int HASH_INIT = 23;
	private static final int HASH_MULT = 11;
	
	public <T extends ADDNode> ADDRNode(T theNode) {
		
		this.theNode = theNode;
		this.negated = false;
		
	}

	@Override
	public int compareTo(ADDRNode o) {
		
		if( o.negated == this.negated ){
			
			if( o.theNode instanceof ADDLeaf && this.theNode instanceof ADDLeaf ){
				
				return ((ADDLeaf)o.theNode).compareTo( (ADDLeaf) this.theNode );
				
			}else if( this.theNode instanceof ADDINode && o.theNode instanceof ADDINode ){
				
				return ((ADDINode)this.theNode).compareTo( (ADDINode) o.theNode );
			}else{
				
				return -1;
				
			}
			
		}
		
		if( this.negated == true ){
			return 1;
		}
		
		return -1;
		
	}
	
	@Override
	public int hashCode() {
		
		if( !hashSet ){
			
			int child = this.theNode.hashCode();
			HashCodeBuilder hb = new HashCodeBuilder(HASH_INIT, HASH_MULT);
			_nHash = hb.append(negated).append(child).hashCode();
//			System.out.println( "RNode hashcode : " + negated + " " + 
//					theNode + " " + _nHash );
//			ret = (( ret << HASH_SHIFT ) - ret ) + ( negated ? 1 : 0 );
//			ret =  (( ret << HASH_SHIFT ) - ret ) + child;
//			ret =  (( ret << HASH_SHIFT ) - ret ) + child*child;
//			
////			int negRet = ret;
////			ret = child;
//			ret =  ( ( ret << HASH_SHIFT ) - ret ) + ( negated ? (int)(child*0.13) : (int)(child*2.51) );
//			
////			if( negated ){
////				ret = -ret;
////			}
			
//			negRet = ( ( negRet << HASH_SHIFT ) - ret ) 
//					+ ( !negated ? (int)(child*1.5) : (int)(child*0.25) ); 
			
//			_nNegHash = negRet;
			
			hashSet = true;
			
		}
		
		return _nHash;
		
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if( obj instanceof ADDRNode ){
			
			ADDRNode o = (ADDRNode)obj;
			if( this.getMax() != o.getMax() || this.getMin() != o.getMin() ){
				return false;
			}
			
			if( this.theNode instanceof ADDLeaf && o.theNode instanceof ADDLeaf ){
				return ((ADDLeaf)this.theNode).equals( (ADDLeaf)o.theNode );
			}else if( this.theNode instanceof ADDINode && o.theNode instanceof ADDINode ){
				
				if( this.negated == o.negated ){
					return ((ADDINode)this.theNode).equals( ( (ADDINode) o.theNode ) );
				}else{
					return ((ADDINode)this.theNode).getNegatedNode().equals(
							(ADDINode) o.theNode );
				}
			}
			return false;
		}
		
		return false;
		
	}

//	@Override
//	public int getComplementedHash() {
//
//		return _nNegHash;
//		
////		if( theNode instanceof ADDLeaf ){
////			return hashCode();
////		}else if( theNode instanceof ADDINode ){
////			
////			int oldINodeHash = ((ADDINode) theNode).getNegatedHashCode();
////			
////			oldINodeHash = ( oldINodeHash << HASH_SHIFT - oldINodeHash ) + ( !negated  ? HASH_ADD : 0 );
////			
////			return oldINodeHash;
////			
////		}
////		
////		return -1;
////		
//	}

	public ADDRNode getNegatedNode() {
		
		ADDRNode ret = new ADDRNode(this.theNode);
		ret.negated = !this.negated;
		
		return ret;
		
	}

	@Override
	public void nullify() {

		this.theNode.nullify();
		
	}

	public ADDRNode getFalseChild(){
		
		if( this.theNode instanceof ADDLeaf ){
			try {
				throw new Exception("get True child called on leaf");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		ADDINode inode = (ADDINode)theNode;
		
		return ( negated ? inode.getTrueChild() : inode.getFalseChild() );
		
	}
	
	public ADDRNode getTrueChild() {
		
		if( this.theNode instanceof ADDLeaf ){
			try {
				throw new Exception("get True child called on leaf");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		ADDINode inode = (ADDINode)theNode;
		
		return ( negated ? inode.getFalseChild() : inode.getTrueChild() );
	}

}
