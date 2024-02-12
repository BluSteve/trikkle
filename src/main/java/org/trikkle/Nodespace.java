package org.trikkle;

import java.util.*;

public final class Nodespace {
	public final static Nodespace DEFAULT = new Nodespace();
	final Map<Set<String>, Node> nodeCache = new HashMap<>();

	public DiscreteNode discreteOf(Set<String> datumNames) {
		return DiscreteNode.fromNodespace(this, datumNames);
	}

	public DiscreteNode discreteOf(String... datumNames) {
		return DiscreteNode.fromNodespace(this, new HashSet<>(Arrays.asList(datumNames)));
	}

	public StreamNode streamOf(String datumName) {
		return StreamNode.fromNodespace(this, datumName);
	}

	public EmptyNode emptyOf() {
		return EmptyNode.fromNodespace(this);
	}
}
