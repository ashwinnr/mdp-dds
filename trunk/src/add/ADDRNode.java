package add;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

import dd.DDNode;
import dd.DDRNode;

public class ADDRNode extends DDRNode<ADDNode> implements Comparable<ADDRNode> {

	private static final int HASH_SHIFT = 3;
	private static final int HASH_INIT = 19;

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
			
			int ret = HASH_INIT;
					
			int child = this.theNode.hashCode();
			
			ret =  (( ret << HASH_SHIFT ) - ret ) + child;
			
			ret =  (( ret << HASH_SHIFT ) - ret ) + child*child;
			
			int negRet = ret;
			
			ret =  ( ( ret << HASH_SHIFT ) - ret ) + ( negated ? (int)(child*1.5) : (int)(child*0.25) );
			
			_nHash = ret;
			
			negRet = ( ( negRet << HASH_SHIFT ) - ret ) 
					+ ( !negated ? (int)(child*1.5) : (int)(child*0.25) ); 
			
			_nNegHash = negRet;
			
			hashSet = true;
			
		}
		
		return _nHash;
		
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if( obj instanceof ADDRNode ){
			
			ADDRNode o = (ADDRNode)obj;
			
			if( this.negated == o.negated ){
				
				if( this.theNode instanceof ADDLeaf && o.theNode instanceof ADDLeaf ){
					
					return ((ADDLeaf)this.theNode).equals( (ADDLeaf)o.theNode );
					
				}else if( this.theNode instanceof ADDINode && o.theNode instanceof ADDINode ){
					
					return ((ADDINode)this.theNode).hashCode() == ( (ADDINode) o.theNode ).hashCode();
					
				}else{
					return false;
				}
				
			}
			
			return false;
			
		}
		
		return false;
		
	}

	@Override
	public int getComplementedHash() {

		return _nNegHash;
		
//		if( theNode instanceof ADDLeaf ){
//			return hashCode();
//		}else if( theNode instanceof ADDINode ){
//			
//			int oldINodeHash = ((ADDINode) theNode).getNegatedHashCode();
//			
//			oldINodeHash = ( oldINodeHash << HASH_SHIFT - oldINodeHash ) + ( !negated  ? HASH_ADD : 0 );
//			
//			return oldINodeHash;
//			
//		}
//		
//		return -1;
//		
	}

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

	public double getMax() {
		return theNode.getMax();
	}

	public double getMin() {
		return theNode.getMin();
	}

}
