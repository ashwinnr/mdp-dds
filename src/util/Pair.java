/**
 * Utilities: A simple object Pair.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 9/1/03
 *
 **/

package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Logger;

public class  Pair<K1 extends Comparable<K1> , K2 extends Comparable<K2> > 
		implements Comparable< Pair<K1,K2> >  {

	private static final int HASH_PRIME1 = 31;
	private static final int HASH_PRIME2 = 53;
	
	//HASH_PRIME1, HASH_PRIME2);
	
	public K1 _o1 = null;
	public K2 _o2 = null;
	protected static Logger LOGGER = Logger.getLogger(Pair.class.getName());

//	public int HASH_SHIFT = 3;
//	public int HASH_INIT = 13;

	public Pair(K1 o1, K2 o2) {
		_o1 = o1;
		_o2 = o2;
	}

	public Pair() {
		_o1 = null;
		_o2 = null;
	}

//	@Override
//	public int hashCode() {
//	//sixth degree polynolmial
//		
//		int ret = HASH_INIT;
//		int h1 = _o1.hashCode();
//		int h2 = _o2.hashCode();
////		if( h1 == h2 ){
////			ret = h1;
////		}else{
//			ret = ( ( ret << HASH_SHIFT ) - ret ) + h1;
//			if( h1 > 0 ){
//				ret = (int) (( ( ret << HASH_SHIFT ) - ret ) + ( h1 > 0 ? Math.log(h1) : 0 ));
//			}
//			ret = ( ( ret << HASH_SHIFT) - ret ) + h2;
//			if( h2 > 0 ){
//				ret = (int) (( ( ret << HASH_SHIFT ) - ret ) + ( h2 > 0 ? Math.log(h2) : 0 ));
//			}
////		}
//		
////		System.out.println( "hash for " + this + " " + h1 + " " + h2 + " " + ret );
//
//		return ret;
////		int ret = (new int[]{h1, h2}).hashCode();
////		return ret;
//	}
	
//	
//	@Override
//	public boolean equals(Object thing) {
//		if (thing instanceof Pair) {
//			Pair<K1,K2> p = (Pair<K1,K2>) thing;
//			return (_o1.equals(p._o1) && _o2.equals(p._o2));
//		} else {
//			return false;
//		}
//	}
//

	@Override
	public String toString() {
		return "<" + _o1.toString() + ", " + _o2.toString() + ">";
	}

	@Override
	public int hashCode() {
//		return com.google.common.base.Objects.hashCode( _o1, _o2 );
//		return Objects.hash( _o1.hashCode(), _o2.hashCode() );
		int h1 = _o1.hashCode();
		int h2 = _o2.hashCode();
//		System.out.println( h1 + " " + h2  + " " + result );
		return com.google.common.base.Objects.hashCode( h1+h2, h1 );
		
		//hcb.append( h1+h2 ).
//				//append( h2 ).hashCode();//.append( h1*h1 ).append( h1*h2 ).hashCode();
//		System.out.println( "Pair hashcode: " + " " + this + " " + result );
//		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if( obj instanceof Pair ){
			Pair other = (Pair) obj;
			if (_o1 == null) {
				if (other._o1 != null)
					return false;
			} else if (!_o1.equals(other._o1))
				return false;
			if (_o2 == null) {
				if (other._o2 != null)
					return false;
			} else if (!_o2.equals(other._o2))
				return false;
			return true;
		}
		return false;
	}

	public void copy(Pair<K1, K2> leafVals) {

		_o1 = leafVals._o1;
		_o2 = leafVals._o2;
		
	}

	@Override
	public int compareTo(Pair<K1, K2> arg0) {
		
		int comp_o1 = _o1.compareTo(arg0._o1);
		if (comp_o1 != 0) {
			return comp_o1;
		}

		return _o2.compareTo(arg0._o2);
		
	}

	public void copy(K1 o1, K2 o2) {
		
		_o1 = o1;
		_o2 = o2;
		
	}

}
