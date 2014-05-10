package util;

import java.util.ArrayList;
import java.util.Iterator;

public class InternedArrayList<T> extends ArrayList<T> {
    	public InternedArrayList(ArrayList<T> ordering) {
    	    super();
    	    addAll(ordering);
    	}
    	
    	public InternedArrayList() {
    		super();
    	}

		@Override
    	public int indexOf(Object o) {
    	    Iterator<T> it = super.iterator();
    	    int idx = 0;
    	    
    	    while( it.hasNext() ){
    		if( ((T)o) == it.next() ){
    		    return idx;
    		}
    		++idx;
    	    }
    	    
    	    return -1;
    	    
    	}
}
