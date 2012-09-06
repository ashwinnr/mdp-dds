/**
 * Utilities: A simple object Pair.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 9/1/03
 *
 **/

package util;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

public class  Pair<K1 extends Comparable<K1> , K2 extends Comparable<K2> > 
		implements Comparable< Pair<K1,K2> >  {

	public K1 _o1 = null;
	public K2 _o2 = null;
	protected static Logger LOGGER = Logger.getLogger(Pair.class.getName());

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
		return _o1.hashCode() - _o2.hashCode();
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
