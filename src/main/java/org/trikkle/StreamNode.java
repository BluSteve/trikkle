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
