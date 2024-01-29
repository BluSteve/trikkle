package org.trikkle.utils;

import java.util.concurrent.ConcurrentHashMap;

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
	public V putIfAbsent(K key, V value) {
		V rvalue = super.putIfAbsent(key, value);
		if (rvalue != null) {
			throw new IllegalArgumentException("Key \"" + key + "\" already present in map!");
		}
		return null;
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
