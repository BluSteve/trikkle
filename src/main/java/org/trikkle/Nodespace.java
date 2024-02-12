package org.trikkle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Nodespace {
	public final static Nodespace instance = new Nodespace();
	public final String prefix;
	final Map<Set<String>, Node> nodeCache = new HashMap<>();

	public Nodespace() {
		this.prefix = "";
	}

	public Nodespace(String prefix) {
		this.prefix = prefix + ".";
	}

	public DiscreteNode discreteOf(Set<String> datumNames) {
		Set<String> prefixedDatumNames = new HashSet<>();
		for (String datumName : datumNames) {
			prefixedDatumNames.add(prefix + datumName);
		}
		return DiscreteNode.of(this, prefixedDatumNames);
	}

	public DiscreteNode discreteOf(String... datumNames) {
		String[] prefixedDatumNames = new String[datumNames.length];
		for (int i = 0; i < datumNames.length; i++) {
			prefixedDatumNames[i] = prefix + datumNames[i];
		}
		return DiscreteNode.of(this, prefixedDatumNames);
	}

	public StreamNode streamOf(String datumName) {
		return StreamNode.of(this, prefix + datumName);
	}
}
