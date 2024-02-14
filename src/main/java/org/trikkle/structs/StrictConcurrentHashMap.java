package org.trikkle.structs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link ConcurrentHashMap} that throws exceptions when a key is already present in the map.
 *
 * @author Steve Cao
 * @since 0.1.0
 */
public class StrictConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {
	@Override
	public V put(K key, V value) {
		V rvalue = super.put(key, value);
		if (rvalue != null) {
			throw new IllegalArgumentException("Key \"" + key + "\" already present in map!");
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (K key : m.keySet()) {
			if (containsKey(key)) {
				throw new IllegalArgumentException("Key \"" + key + "\" already present in map!");
			}
		}
		super.putAll(m);
	}

	// This is needed because otherwise containsKey would call this.get(key) which would throw an exception
	@Override
	public boolean containsKey(Object key) {
		return super.get(key) != null;
	}

	@Override
	public V get(Object key) {
		V value = super.get(key);
		if (value == null) throw new IllegalArgumentException("Key \"" + key + "\" not present in map!");
		return value;
	}
}
