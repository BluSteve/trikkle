package org.trikkle.structs;

import java.util.Map;
import java.util.Set;

/**
 * A {@link Map} that allows multiple values to be associated with a single key.
 *
 * @since 0.1.0
 */
public interface MultiMap<K, V> extends Map<K, Set<V>> {
	boolean putOne(K key, V value);
}
