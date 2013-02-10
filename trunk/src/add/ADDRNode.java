package add;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

import javax.naming.ldap.HasControls;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import dd.DDNode;
import dd.DDRNode;

public class ADDRNode extends DDRNode<ADDNode> implements Comparable<ADDRNode> {

//	private static final int HASH_SHIFT = 3;
//	private static final int HASH_INIT = 19;
//	private static final int HASH_INIT = 23;
//	private static final int HASH_MULT = 11;
	private long GLOBAL_ID;
	private static long GLOBAL_ID_COUNTER = 1;
	
	public long getID(){
		return GLOBAL_ID;
	}
	
	public <T extends ADDNode> ADDRNode(T theNode) {
		if( GLOBAL_ID_COUNTER == 0 ){
			try{
				throw new Exception("ID wrapped around");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		this.GLOBAL_ID = GLOBAL_ID_COUNTER++;
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
		return ((Long)GLOBAL_ID).hashCode();
//		if( !hashSet ){
//			
//			int child = this.theNode.hashCode();
//			HashCodeBuilder hb = new HashCodeBuilder(HASH_INIT, HASH_MULT);
//			_nHash = hb.append(negated).append(child).
//					append( child*child ).hashCode();
////			System.out.println( "RNode hashcode : " + negated + " " + 
////					theNode + " " + _nHash );
////			ret = (( ret << HASH_SHIFT ) - ret ) + ( negated ? 1 : 0 );
////			ret =  (( ret << HASH_SHIFT ) - ret ) + child;
////			ret =  (( ret << HASH_SHIFT ) - ret ) + child*child;
////			
//////			int negRet = ret;
//////			ret = child;
////			ret =  ( ( ret << HASH_SHIFT ) - ret ) + ( negated ? (int)(child*0.13) : (int)(child*2.51) );
////			
//////			if( negated ){
//////				ret = -ret;
//////			}
//			
////			negRet = ( ( negRet << HASH_SHIFT ) - ret ) 
////					+ ( !negated ? (int)(child*1.5) : (int)(child*0.25) ); 
//			
////			_nNegHash = negRet;
//			
//			hashSet = true;
//			
//		}
//		
//		return _nHash;
		
	}
	
	@Override
	public String toString() {
		return ( negated ? "! " : "" ) + GLOBAL_ID;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if( obj instanceof ADDRNode ){
			
			ADDRNode o = (ADDRNode)obj;
			
			if( this.theNode instanceof ADDLeaf && o.theNode instanceof ADDLeaf ){
				return ((ADDLeaf)this.theNode).equals( (ADDLeaf)o.theNode );
			}else if( this.theNode instanceof ADDINode && o.theNode instanceof ADDINode ){
				final boolean negated_equals = this.negated == o.negated; 
				if( negated_equals ){
					final boolean node_equals = this.theNode.equals( o.theNode );
//					System.out.println("node equals " + node_equals );
					final boolean id_equals = getID() == o.getID();
					if( node_equals && !id_equals ){
						try{
							throw new Exception( "duplicate RNodes!");
						}catch( Exception e  ){
							e.printStackTrace();
							System.exit(1);	
						}
					}
					return node_equals;
				}else{
					final boolean negated_node_equals 
						= (( ADDINode )this.theNode).negatedEquals( (ADDINode) o.theNode );
//					System.out.println("node equals " + node_equals );
//					final boolean id_equals = getID() == o.getID();
					return negated_node_equals;
				}
//				System.out.println("negated equals " + negated_equals );
				
//				return negated_equals && node_equals;
//				//probably here because this and o have same hashcode
//				//quick way to resolve this is to check if true and false child 
//				//have same ahshcode
//				//return false if not
//				//otherwise recursively check
//				if( getTestVariable() != o.getTestVariable() ){
//					//pruning by test var
//					return false;
//				}
//				
//				ADDRNode this_true_child = this.getTrueChild();
//				ADDRNode this_false_child = this.getFalseChild();
//				ADDRNode o_true_child = o.getTrueChild();
//				ADDRNode o_false_child = o.getFalseChild();
//				if( this_true_child.hashCode() != o_true_child.hashCode() ||
//						this_false_child.hashCode() != o_false_child.hashCode() ){
////					System.out.println( "pruning equals by hashcode" );
//					return false;
//				}
//				
//				if( this.getMax() != o.getMax() || this.getMin() != o.getMin() ){
//					//pruning by bounds
//					return false;
//				}
//				
//				boolean true_equals = getTrueChild().equals( o.getTrueChild() );
//				if( !true_equals ){
//					return false;
//				}else{
//					boolean false_equals = getFalseChild().equals( o.getFalseChild() );
////					if( false_equals && ( getTrueChild().hashCode() != 
////							getTrueChild().hashCode() || 
////							getFalseChild().hashCode() != 
////							getFalseChild().hashCode() ) ){
////						System.err.println( "DDs are equal but unequal hashcodes" );
////						System.err.println( hashCode() + " " + getTrueChild().hashCode() 
////								+ " " + getFalseChild().hashCode() );
////					}
//					return false_equals;
//				}
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

//	@Override
//	public void nullify() {
//
//		this.theNode.nullify();
//		
//	}

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
