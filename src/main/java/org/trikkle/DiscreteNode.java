package org.trikkle;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link Node} that contains a defined positive number of datums.
 *
 * @see Arc
 * @see Link
 * @see Graph
 * @since 0.1.0
 */
public final class DiscreteNode extends Node {
	private final AtomicInteger datumsFilled = new AtomicInteger(0);

	/**
	 * Creates a new DiscreteNode with the given pointers.
	 *
	 * @param pointers the pointers to the datums
	 * @throws IllegalArgumentException if the set of pointers is empty
	 */
	public DiscreteNode(Set<String> pointers) {
		super(pointers);
		if (pointers.isEmpty()) {
			throw new IllegalArgumentException("DiscreteNode must have at least one datum");
		}
	}

	public DiscreteNode(String... pointers) {
		this(new HashSet<>(Arrays.asList(pointers)));
	}

	@Override
	protected void uncheckedAddDatum(String pointer, Object datum) {
		overseer.getCache().put(pointer, datum);

		int i = datumsFilled.incrementAndGet();
		if (i == pointers.size()) { // all datums filled
			setProgress(1);
		} else {
			setProgress((double) i / pointers.size());
		}
	}

	/**
	 * Irreversibly sets the node to {@code usable}. Also ticktocks the overseer if this node is not an ending node.
	 *
	 * @throws IllegalStateException if the node is not fully filled
	 */
	@Override
	public void setUsable() {
		if (datumsFilled.get() < pointers.size()) {
			throw new IllegalStateException("DiscreteNode " + this + " is not fully filled and cannot be set to usable.
					");
		}
		super.setUsable();
		if (!overseer.g.endingNodes.contains(this)) {
			overseer.unsafeTicktock(this);
		}
	}

	@Override
	public void reset() {
		super.reset();
		datumsFilled.set(0);
	}
}
