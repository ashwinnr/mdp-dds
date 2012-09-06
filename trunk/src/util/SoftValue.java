package util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

public class SoftValue<K,V> extends SoftReference<V> {

	public K key;

	public SoftValue(V referent, ReferenceQueue<? super V> q, K key) {
		super(referent, q);
		this.key = key;
	}

}
