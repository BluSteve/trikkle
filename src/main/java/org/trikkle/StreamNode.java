package org.trikkle;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;

public class StreamNode extends Node {
	public StreamNode(Set<String> datumNames) {
		super(datumNames);
	}

	@Override
	public void addDatum(String datumName, Object datum) {
		Map<String, Object> cache = overseer.getCache();
		try {
			if (cache.containsKey(datumName)) {
				((SynchronousQueue) cache.get(datumName)).put(datum);
			}
			else {
				SynchronousQueue syncQueue = new SynchronousQueue();
				syncQueue.put(datum);
				cache.put(datumName, syncQueue);
			}

			progress = 1;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
