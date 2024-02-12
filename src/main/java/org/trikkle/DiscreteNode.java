package org.trikkle;

import java.util.*;

public final class DiscreteNode extends Node {
	private final Map<String, Boolean> isDatumDone = new HashMap<>();

	private DiscreteNode(Set<String> datumNames) {
		super(datumNames);
		for (String datumName : datumNames) {
			isDatumDone.put(datumName, false);
		}
	}

	public static DiscreteNode of(Set<String> datumNames) {
		return fromNodespace(Nodespace.instance, datumNames);
	}

	public static DiscreteNode of(String... datumNames) {
		return fromNodespace(Nodespace.instance, new HashSet<>(Arrays.asList(datumNames)));
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
		isDatumDone.put(datumName, true);

		if (!isDatumDone.containsValue(false)) { // all datums filled
			setProgress(1);
			setUsable();
			if (!overseer.g.endingNodes.contains(this)) {
				overseer.unsafeTicktock();
			}
		}
	}

	@Override
	public void reset() {
		super.reset();
		for (String datumName : datumNames) {
			isDatumDone.put(datumName, false);
		}
	}
}
