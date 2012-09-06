package add;

import java.lang.ref.SoftReference;

import dd.DDNode;
import dd.DDRNode;

public class ADDRNode extends DDRNode<ADDNode> {

	@Override
	public int compareTo(ADDRNode o) {
		
		if( o.negated == this.negated ){
			
			if( o.theNode instanceof Comparable ){
				return theNode.compareTo( ( (Comparable)o.theNode) );	
			}
			
		}
		
		if( this.negated == true ){
			return 1;
		}
		
		return -1;
		
	}
	
}
