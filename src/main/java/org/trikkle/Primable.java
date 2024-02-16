package org.trikkle;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A class that can be primed with an {@link Overseer}. Used for {@link Arc}s and {@link Node}s.
 *
 * @since 0.1.0
 */
public interface Primable {
	void primeWith(Overseer overseer);

	/**
	 * Reset the Primable to its initial state.
	 */
	void reset();

	ReentrantLock getLock();
}
