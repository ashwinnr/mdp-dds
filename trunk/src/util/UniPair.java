package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import add.ADDRNode;

public class UniPair<K1 extends Comparable<K1> > extends Pair<K1,K1> implements Collection<K1> {

	public UniPair(K1 o1, K1 o2) {
		super(o1, o2);
	}

	public UniPair() {
		super();
	}

	@Override
	public int compareTo(Pair<K1,K1> arg0) {
		
		if( arg0 instanceof UniPair ){
			return super.compareTo(arg0);	
		}
		
		try{
			throw new UnsupportedOperationException("unipair comared to " + arg0.getClass().getName() );
			
		}catch( UnsupportedOperationException e ){
			e.printStackTrace();
			LOGGER.severe(e.getMessage());
		}
		
		return -1;
		
	}

	@Override
	public boolean add(K1 e) {

		if( _o1 == null ){
			_o1 = e;
		}else if( _o2 == null ){
			_o2 = e;
		}else{
			return false;
		}
		
		return true;
	
	}

	@Override
	public boolean addAll(Collection<? extends K1> c) {

		if( c.size() != 2 || size() != 0){
			try{
				throw new UnsupportedOperationException("Unsopprted colelction operation or Pair is not empty " 
						+ this.toString()  );
				
			}catch( UnsupportedOperationException e ){
				e.printStackTrace();
				LOGGER.severe(e.getMessage());
				return false;
			}
		}
		
		if( _o1 == null && _o2 == null ){
			
			for( K1 item : c ){
				if( _o1 == null && _o2 == null ){
					_o1 = item;
				}else if( _o2 == null ){
					_o2 = item;
				}
			}
			
		}
		
		return true;
	}

	@Override
	public void clear() {
		_o1 = null;
		_o2 = null;
	}

	@Override
	public boolean contains(Object o) {

		return _o1.equals(o) || _o2.equals(o);

	}

	@Override
	public boolean containsAll(Collection<?> c) {
		
		for( Object item : c ){
			
			if( !contains(item) ){
				return false;
			}
			
		}
		
		return true;

	}

	@Override
	public boolean isEmpty() {
		
		return _o1 == null && _o2 == null;
		
	}

	@Override
	public Iterator<K1> iterator() {
		
		ArrayList<K1> _temp = new ArrayList<K1>();
		_temp.add(_o1);
		_temp.add(_o2);
		
		return _temp.iterator();
				
	}

	@Override
	public boolean remove(Object o) {
		
		if( _o1.equals(o) ){
			_o1 = null;
			return true;
		}else if( _o2.equals(o) ){
			_o2 = null;
			return true;
		}
		
		return false;
		
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		
		boolean remoed = false;
		
		for( Object item : c ){
			
			remoed = remoed || remove(item);
			
		}
		
		return remoed;

	}

	@Override
	public boolean retainAll(Collection<?> c) {
		
		boolean remoed = false;
		
		if( !c.contains(_o1) ){
			remoed = remove(_o1);
		}
		
		if( !c.contains(_o2) ){
			remoed = remove(_o2);
		}
		
		return remoed;
	}

	@Override
	public int size() {
		return ( (_o1 == null)?0:1 ) + ( (_o2 == null )?0:1 );
	}

	@Override
	public Object[] toArray() {
		
		Object[] ret = new Object[2];
		ret[0] = _o1;
		ret[1] = _o2;
		
		return ret;
		
	}

	@Override
	public <T> T[] toArray(T[] a) {
		
		try{
			throw new UnsupportedOperationException("Unsopprted colelction operation " );
			
		}catch( UnsupportedOperationException e ){
			e.printStackTrace();
			LOGGER.severe(e.getMessage());
		}
		
		return null;
	}

	public boolean isEqual() {
		return _o1.equals(_o2);
	}

	public void swap() {

		K1 temp = _o1;
		
		_o1 = _o2;
		
		_o2 = temp;
		
	}

	public UniPair<K1> getInvertedPair() {
		return new UniPair<K1>( _o2, _o1 );
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
