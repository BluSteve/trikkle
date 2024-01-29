package org.trikkle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DiscreteNode extends Node {
	private final Map<String, Boolean> isDatumDone = new HashMap<>();
	public DiscreteNode(Set<String> datumNames) {
		super(datumNames);
		for (String datumName : datumNames) {
			isDatumDone.put(datumName, false);
		}
	}

	@Override
	protected void uncheckedAddDatum(String datumName, Object datum) {
		Map<String, Object> cache = overseer.getCache();
		if (cache.containsKey(datumName)) {
			// todo test and remove this
			throw new IllegalStateException("Datum already present in cache!");
		}
		else {
			cache.put(datumName, datum);
			isDatumDone.put(datumName, true);

			if (!isDatumDone.containsValue(false)) { // all datums filled
				setProgress(1);
				overseer.ticktock(this);
			}
		}
	}
}
