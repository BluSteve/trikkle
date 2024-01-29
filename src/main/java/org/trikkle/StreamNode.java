package org.trikkle;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class StreamNode extends Node {
	public StreamNode(Set<String> datumNames) {
		super(datumNames);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	// Assumes that all datums of a particular name are of the same type
	protected void uncheckedAddDatum(String datumName, Object datum) {
		Map<String, Object> cache = overseer.getCache();
		if (cache.containsKey(datumName)) {
			((Queue) cache.get(datumName)).add(datum);
		}
		else {
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
