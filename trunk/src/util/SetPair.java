package util;

import java.util.Set;

public abstract class SetPair< K1, K2 > {

	protected Set<K1> _s1;
	protected Set<K2> _s2;

	public boolean addFirst(K1 k1 ){

		return _s1.add(k1);
		
	}
	
	public boolean addSecond( K2 k2 ){
		
		return _s2.add(k2);
		
	}
	
	public boolean containsFirst( K1 k1 ){
		return _s1.contains(k1);
	}
	
	public boolean containsSecond( K2 k2 ){
		return _s2.contains(k2);
	}
	
	
}
