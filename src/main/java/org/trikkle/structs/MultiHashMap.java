package org.trikkle.structs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link HashMap} that allows multiple values to be associated with a single key.
 *
 * @author Steve Cao
 * @since 0.1.0
 */
public class MultiHashMap<K, V> extends HashMap<K, Set<V>> implements MultiMap<K, V> {
	@Override
	public boolean putOne(K key, V value) {
		Set<V> set;
		if (super.containsKey(key)) {
			set = super.get(key);
			return set.add(value);
		} else {
			set = new HashSet<>();
			set.add(value);
			super.put(key, set);
			return true;
		}
	}
}
