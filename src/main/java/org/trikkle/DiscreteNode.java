package org.trikkle;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Steve Cao
 * @since 0.1.0
 */
public final class DiscreteNode extends Node {
	private final AtomicInteger datumsFilled = new AtomicInteger(0);

	public DiscreteNode(Set<String> datumNames) {
		super(datumNames);
		if (datumNames.isEmpty()) {
			throw new IllegalArgumentException("DiscreteNode must have at least one datum");
		}
	}

	public DiscreteNode(String... datumNames) {
		this(new HashSet<>(Arrays.asList(datumNames)));
	}

	@Override
	protected void uncheckedAddDatum(String datumName, Object datum) {
		overseer.getCache().put(datumName, datum);

		int i = datumsFilled.incrementAndGet();
		if (i == datumNames.size()) { // all datums filled
			setProgress(1);
			if (!overseer.g.endingNodes.contains(this)) {
				overseer.unsafeTicktock(this);
			}
		} else {
			setProgress((double) i / datumNames.size());
		}
	}

	@Override
	public void setUsable() {
		if (datumsFilled.get() < datumNames.size()) {
			throw new IllegalStateException("DiscreteNode " + this + " is not fully filled and cannot be set to usable.");
		}
		super.setUsable();
	}

	@Override
	public void reset() {
		super.reset();
		datumsFilled.set(0);
	}
}
