package org.trikkle.utils;

import java.util.Map;
import java.util.Set;

public interface MultiMap<K, V> extends Map<K, Set<V>> {
	Set<V> putOne(K key, V value);
}
