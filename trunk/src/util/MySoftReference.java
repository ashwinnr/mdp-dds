package util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;


public class MySoftReference< T extends Comparable<T> > extends SoftReference<T> implements Comparable< MySoftReference<T> > {

	public MySoftReference(T referent) {
		super(referent);
	}
	
	public MySoftReference(T referent, ReferenceQueue<? super T> q) {
		super(referent, q);
	}

	@Override
	public int hashCode() {
		return this.get().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if( obj instanceof MySoftReference ){
			return this.get().equals( ((MySoftReference<T>)obj).get() );	
		}
		
		return false;
		
	}

	@Override
	public int compareTo(MySoftReference<T> o) {
		
		if( equals(o) ){
			return 0;
		}else{
			return this.get().compareTo( o.get() );
		}
		
		
	}
}
