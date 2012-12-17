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
import java.util.logging.Logger;

public class  Pair<K1 extends Comparable<K1> , K2 extends Comparable<K2> > 
		implements Comparable< Pair<K1,K2> >  {

	public K1 _o1 = null;
	public K2 _o2 = null;
	protected static Logger LOGGER = Logger.getLogger(Pair.class.getName());

	public int HASH_SHIFT = 3;
	public int HASH_INIT = 13;

	public Pair(K1 o1, K2 o2) {
		_o1 = o1;
		_o2 = o2;
	}

	public Pair() {
		_o1 = null;
		_o2 = null;
	}

	@Override
	public int hashCode() {
	//sixth degree polynolmial
		
		int ret = HASH_INIT;
		int h1 = _o1.hashCode();
		int h2 = _o2.hashCode();

		ret = ( ( ret << HASH_SHIFT ) - ret ) + h1;
		ret = ( ( ret << HASH_SHIFT) - ret ) + h2;
		int h22 = h2*h2, h12 = h1*h1;
		ret = ( ( ret << HASH_SHIFT) - ret ) + (h12);
		ret = ( ( ret << HASH_SHIFT ) - ret ) + (h22);
		ret = ( ( ret << HASH_SHIFT ) - ret ) + (h12*h1);
		ret = ( ( ret << HASH_SHIFT ) - ret ) + (h22*h2);
		
		System.out.println( "hash for " + this + " " + h1 + " " + h2 + " " + ret );

		return ret;
//		int ret = (new int[]{h1, h2}).hashCode();
//		return ret;
	}
	
	@Override
	public boolean equals(Object thing) {
		if (thing instanceof Pair) {
			Pair<K1,K2> p = (Pair<K1,K2>) thing;
			return (_o1.equals(p._o1) && _o2.equals(p._o2));
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return "<" + _o1.toString() + ", " + _o2.toString() + ">";
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
