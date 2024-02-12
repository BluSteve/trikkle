package org.trikkle;

import java.util.concurrent.locks.ReentrantLock;

public interface Primable {
	void primeWith(Overseer overseer);

	/**
	 * Reset the Primable to its initial state. If you have any Arcs dependent on state, you MUST reset those
	 * fields.
	 */
	void reset();

	ReentrantLock getLock();
}
