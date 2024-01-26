package org.trikkle;

import java.util.Set;

public abstract class Node implements Primable {
	protected Overseer overseer;
	protected double progress;
	protected Set<String> datumNames;

	public Node(Set<String> datumNames) {
		this.datumNames = datumNames;
	}

	public abstract void addDatum(String datumName, Object datum);

//	@Override
//	public boolean equals(Object obj) {
//		if (obj == this) return true;
//		if (!(obj instanceof Node)) return false;
//		return this.index == ((Node) obj).index;
//	}

	public double getProgress() { // 1 = usable, !1 = unusable. Might convert to boolean in the future.
		return progress;
	}

	@Override
	public void primeWith(Overseer overseer) {
		this.overseer = overseer;
	}

	@Override
	public Overseer getOverseer() {
		return overseer;
	}

	@Override
	public String toString() {
		return "Node{" +
				"progress=" + progress +
				", datumNames=" + datumNames +
				'}';
	}
}
