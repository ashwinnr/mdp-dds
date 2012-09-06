package util;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import add.ADDINode;
import add.ADDLeaf;

public class ConcurrentSetPair<K1, K2> extends SetPair<K1, K2> {

	public ConcurrentSetPair() {
		super();
		_s1 = Collections.newSetFromMap( new ConcurrentHashMap< K1 , Boolean>() );
		_s2 = Collections.newSetFromMap( new ConcurrentHashMap< K2 , Boolean>() );
	}
	
}
