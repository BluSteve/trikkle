package org.trikkle;

import java.util.HashMap;
import java.util.Map;

public class DiscreteNode extends Node {

	private Map<String, Boolean> isDatumDone = new HashMap<>();
	public DiscreteNode(String[] datumNames) {
		super(datumNames);
		for (String datumName : datumNames) {
			isDatumDone.put(datumName, false);
		}
	}

	@Override
	public void addDatum(String datumName, Object datum) {
		Map<String, Object> cache = overseer.getCache();
		if (cache.containsKey(datumName)) {
			throw new IllegalStateException("Datum already present in cache!");
		}
		else {
			cache.put(datumName, datum);
			isDatumDone.put(datumName, true);

			if (!isDatumDone.containsValue(false)) { // all datums filled
				progress = 1;
				overseer.ticktock();
			}
		}
	}
}
