package org.trikkle;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link Node} that can have an unlimited number of datums of one name added to it. Upon the first datum added,
 * the {@code StreamNode} will be irreversibly set to {@code usable}. ({@link Node#setUsable()}) Arcs dependent on
 * this node will thus be called every single tick after the first datum is added.
 * <p>
 * The recipient arc of a {@code StreamNode} cannot be an {@link AutoArc} because the recipient arcs will usually
 * be run multiple times as more data streams into this node.
 * <p>
 * An attempt was made to allow {@code usable} to be unset for {@code StreamNode}s, but it led to many concurrency
 * issues.
 *
 * @see AutoArc
 * @see Arc
 * @see Link
 * @see Graph
 * @since 0.1.0
 */
public final class StreamNode extends Node {
	private final AtomicInteger count = new AtomicInteger(0);
	private int limit = -1;

	public StreamNode(String datumName) {
		super(Collections.singleton(datumName));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	// Assumes that all datums of a particular name are of the same type
	protected void uncheckedAddDatum(String datumName, Object datum) {
		((Queue) overseer.getCache().get(datumName)).add(datum);
		setUsable();
		if (limit != -1) {
			int c = count.getAndIncrement();
			if (c == limit) {
				throw new IllegalStateException("StreamNode is already full!");
			}
			setProgress((double) (c + 1) / limit);
		}
		if (!overseer.g.endingNodes.contains(this)) {
			overseer.unsafeTicktock(this);
		}
	}

	public int getLimit() {
		return limit;
	}

	/**
	 * Sets the maximum number of datums that can be added to this node. The progress of this node will be set to the
	 * current number of datums divided by the limit after each datum is added.
	 * <p>
	 * By default, the limit is -1, which means that there is no limit. In that case, you may manually set the progress
	 * of the node in the input arc.
	 *
	 * @param limit the maximum number of datums that can be added to this node
	 */
	public void setLimit(int limit) {
		if (limit <= 0) {
			throw new IllegalArgumentException("Limit must be positive!");
		}
		this.limit = limit;
	}

	@Override
	public void primeWith(Overseer overseer) {
		super.primeWith(overseer);
		overseer.getCache().putIfAbsent(datumNames.iterator().next(), new ConcurrentLinkedQueue<>());
	}

	@Override
	public void reset() {
		super.reset();
		count.set(0);
	}

	@Override
	public String toString() {
		return "StreamNode" + datumNames;
	}
}
