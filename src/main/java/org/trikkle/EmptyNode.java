package org.trikkle;

import java.util.Collections;
import java.util.Set;

public class EmptyNode extends Node {
	private static final Set<String> EMPTY_SET = Collections.emptySet();

	private EmptyNode() {
		super(EMPTY_SET);
	}

	public static EmptyNode of() {
		return fromNodespace(Nodespace.instance);
	}

	static EmptyNode fromNodespace(Nodespace nodespace) {
		if (nodespace.nodeCache.containsKey(EMPTY_SET)) {
			return (EmptyNode) nodespace.nodeCache.get(EMPTY_SET);
		} else {
			EmptyNode node = new EmptyNode();
			nodespace.nodeCache.put(EMPTY_SET, node);
			return node;
		}
	}

	@Override
	protected void uncheckedAddDatum(String datumName, Object datum) {
		throw new UnsupportedOperationException("EmptyNode cannot have any datums!");
	}

	@Override
	public void setUsable() {
		super.setUsable();
		overseer.unsafeTicktock();
	}
}
