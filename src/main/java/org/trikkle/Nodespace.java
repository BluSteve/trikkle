package org.trikkle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class Nodespace {
	final Map<Set<String>, Node> nodeCache = new HashMap<>();
	public final static Nodespace instance = new Nodespace();

	public DiscreteNode discreteOf(Set<String> datumNames) {
		return DiscreteNode.of(this, datumNames);
	}

	public DiscreteNode discreteOf(String... datumNames) {
		return DiscreteNode.of(this, datumNames);
	}

	public StreamNode streamOf(String datumName) {
		return StreamNode.of(this, datumName);
	}
}
