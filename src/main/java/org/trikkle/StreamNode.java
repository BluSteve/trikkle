package org.trikkle;

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class StreamNode extends Node {
	private int limit = -1;
	private int count = 0;

	private StreamNode(String datumName) {
		super(Collections.singleton(datumName));
	}

	public static StreamNode of(String datumName) {
		return of(Nodespace.instance, datumName);
	}

	static StreamNode of(Nodespace nodespace, String datumName) {
		Set<String> singleton = Collections.singleton(datumName);
		Map<Set<String>, Node> nodeCache = nodespace.nodeCache;
		if (nodeCache.containsKey(singleton)) {
			return (StreamNode) nodeCache.get(singleton);
		} else {
			StreamNode node = new StreamNode(datumName);
			nodeCache.put(node.datumNames, node);
			return node;
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	// Assumes that all datums of a particular name are of the same type
	protected synchronized void uncheckedAddDatum(String datumName, Object datum) {
		if (getProgress() == 1) {
			throw new IllegalStateException("StreamNode is already full!");
		}

		Map<String, Object> cache = overseer.getCache();
		if (cache.containsKey(datumName)) {
			((Queue) cache.get(datumName)).add(datum);
		} else {
			Queue queue = new ConcurrentLinkedQueue();
			queue.add(datum);
			cache.put(datumName, queue);
		}

		usable = true;
		count++;
		if (limit != -1) {
			setProgress((double) count / limit);
		}
		overseer.ticktock(this);
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		if (limit <= 0) {
			throw new IllegalArgumentException("Limit must be positive!");
		}
		this.limit = limit;
	}

	@Override
	protected void onDone() {
		overseer.ticktock(this);
	}
}
