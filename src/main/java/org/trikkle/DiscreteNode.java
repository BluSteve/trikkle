package org.trikkle;

import java.util.*;

public class DiscreteNode extends Node {
	private final Map<String, Boolean> isDatumDone = new HashMap<>();

	private DiscreteNode(Set<String> datumNames) {
		super(datumNames);
		for (String datumName : datumNames) {
			isDatumDone.put(datumName, false);
		}
	}

	public static DiscreteNode of(Set<String> datumNames) {
		if (Node.nodeCache.containsKey(datumNames)) {
			return (DiscreteNode) Node.nodeCache.get(datumNames);
		} else {
			DiscreteNode node = new DiscreteNode(datumNames);
			Node.nodeCache.put(node.datumNames, node);
			return node;
		}
	}

	public static DiscreteNode of(String... datumNames) {
		return of(new HashSet<>(Arrays.asList(datumNames)));
	}

	@Override
	protected void uncheckedAddDatum(String datumName, Object datum) {
		overseer.getCache().put(datumName, datum);
		isDatumDone.put(datumName, true);

		if (!isDatumDone.containsValue(false)) { // all datums filled
			setProgress(1);
			overseer.ticktock(this);
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
