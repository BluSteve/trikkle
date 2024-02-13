package org.trikkle;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class DiscreteNode extends Node {
	private final AtomicInteger datumsFilled = new AtomicInteger(0);

	public DiscreteNode(Set<String> datumNames) {
		super(datumNames);
	}

	@Deprecated
	public static DiscreteNode of(Set<String> datumNames) {
		return fromNodespace(Nodespace.DEFAULT, datumNames);
	}

	@Deprecated
	public static DiscreteNode of(String... datumNames) {
		return fromNodespace(Nodespace.DEFAULT, new HashSet<>(Arrays.asList(datumNames)));
	}

	static DiscreteNode fromNodespace(Nodespace nodespace, Set<String> datumNames) {
		if (datumNames.isEmpty()) {
			throw new IllegalArgumentException("DiscreteNode must have at least one datum");
		}
		Map<Set<String>, Node> nodeCache = nodespace.nodeCache;
		if (nodeCache.containsKey(datumNames)) {
			return (DiscreteNode) nodeCache.get(datumNames);
		} else {
			DiscreteNode node = new DiscreteNode(datumNames);
			nodeCache.put(node.datumNames, node);
			return node;
		}
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
			throw new IllegalStateException("DiscreteNode is not yet done!");
		}
		super.setUsable();
	}

	@Override
	public void reset() {
		super.reset();
		datumsFilled.set(0);
	}
}
