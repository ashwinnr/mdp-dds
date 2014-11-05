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
		
		public static void main(String[] args) {
			testFind();
		}

		private static void testFind() {
			ArrayList<String> ord = new ArrayList<String>();
			ord.add("A".intern());
			ord.add("B".intern());
			ord.add("C".intern());
			
			InternedArrayList<String> ial = new InternedArrayList<>(ord);
			System.out.println( ial.indexOf("B".intern()) );
			System.out.println( ial.indexOf(new String("B") ) );
		}
}
