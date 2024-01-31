package org.trikkle.structs;

import java.util.Map;
import java.util.Set;

public interface MultiMap<K, V> extends Map<K, Set<V>> {
	boolean putOne(K key, V value);
}
