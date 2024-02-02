package org.trikkle;

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class StreamNode extends Node {
	private StreamNode(String datumName) {
		super(Collections.singleton(datumName));
	}

	public static StreamNode of(String datumName) {
		Set<String> singleton = Collections.singleton(datumName);
		if (Node.nodeCache.containsKey(singleton)) {
			return (StreamNode) Node.nodeCache.get(singleton);
		} else {
			StreamNode node = new StreamNode(datumName);
			Node.nodeCache.put(node.datumNames, node);
			return node;
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	// Assumes that all datums of a particular name are of the same type
	protected void uncheckedAddDatum(String datumName, Object datum) {
		Map<String, Object> cache = overseer.getCache();
		if (cache.containsKey(datumName)) {
			((Queue) cache.get(datumName)).add(datum);
		} else {
			Queue queue = new ConcurrentLinkedQueue();
			queue.add(datum);
			cache.put(datumName, queue);
		}

		// usable could have been changed from true false before lock on queue was lifted

		usable = true;
		overseer.ticktock(this);
	}

	@Override
	protected void onDone() {
		overseer.ticktock(this);
	}
}
