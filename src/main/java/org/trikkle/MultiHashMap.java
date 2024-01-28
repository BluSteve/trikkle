package org.trikkle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MultiHashMap<K, V> extends HashMap<K, Set<V>> implements MultiMap<K, V> {
	@Override
	public Set<V> putOne(K key, V value) {
		Set<V> set;
		if (super.containsKey(key)) {
			set = super.get(key);
			set.add(value);
		}
		else {
			set = new HashSet<>();
			set.add(value);
			super.put(key, set);
		}
		return set;
	}
}
