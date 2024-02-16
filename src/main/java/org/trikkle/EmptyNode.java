package org.trikkle;

import java.util.Collections;
import java.util.Set;

/**
 * A {@link Node} that has no datums. Useful for when you want to connect two arcs without any datums.
 *
 * @see Arc
 * @see Link
 * @see Graph
 * @since 0.1.0
 */
public final class EmptyNode extends Node {
	static final Set<String> EMPTY_SET = Collections.emptySet();

	public EmptyNode() {
		super(EMPTY_SET);
	}

	@Override
	protected void uncheckedAddDatum(String datumName, Object datum) {
		throw new UnsupportedOperationException("EmptyNode cannot have any datums!");
	}

	@Override
	public void setUsable() {
		if (!isUsable()) {
			super.setUsable();
			setProgress(1);
			if (!overseer.g.endingNodes.contains(this)) {
				overseer.unsafeTicktock(this);
			}
		}
	}
}
