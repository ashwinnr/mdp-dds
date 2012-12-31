package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

import add.ADDINode;
import add.ADDRNode;

public class  UnorderedPair<K1 , K2 >  {

	public K1 _o1 = null;
	public K2 _o2 = null;
	protected static Logger LOGGER = Logger.getLogger(Pair.class.getName());

	public UnorderedPair(K1 o1, K2 o2) {
		_o1 = o1;
		_o2 = o2;
	}

	public UnorderedPair() {
		_o1 = null;
		_o2 = null;
	}

	@Override
	public int hashCode() {
		return ( _o1.hashCode() << 8 ) + _o2.hashCode();
	}
	
	@Override
	public boolean equals(Object thing) {
		
		if (thing instanceof UnorderedPair) {
			UnorderedPair<K1,K2> p = (UnorderedPair<K1,K2>) thing;
			return (_o1.equals(p._o1) && _o2.equals(p._o2));
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return "<" + _o1.toString() + ", " + _o2.toString() + ">";
	}

	public void copy(UnorderedPair<K1, K2> leafVals) {

		copy( leafVals._o1, leafVals._o2 );
		
	}

	public void copy(K1 o1, K2 o2) {
		
		_o1 = o1;
		_o2 = o2;
		
	}

	public static <T> Iterator<T> makeTwoIterator(T n1) {
		ArrayList<T> ao = new ArrayList<T>();
		ao.add(n1);
		ao.add(n1);
		return ao.iterator();
	}


}
