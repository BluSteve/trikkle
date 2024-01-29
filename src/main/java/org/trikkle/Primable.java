package org.trikkle;

public interface Primable {
	void primeWith(Overseer overseer);

	/**
	 * The use of this method is highly discouraged.
	 * Overseer should generally be allowed to do its own thing.
	 *
	 * @return Overseer belonging to this Primable
	 */
	Overseer getOverseer();
}
