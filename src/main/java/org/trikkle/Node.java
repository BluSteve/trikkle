package org.trikkle;

import java.util.Set;

public abstract class Node implements Primable { // Generics are too restricting for this class
	protected Overseer overseer;
	private double progress; // monotonically increasing
	protected boolean usable; // can be true then false
	protected Set<String> datumNames;

	public Node(Set<String> datumNames) {
		this.datumNames = datumNames;
	}

	public void addDatum(String datumName, Object datum) {
		if (!datumNames.contains(datumName)) {
			throw new IllegalArgumentException("Datum " + datumName + " was not declared by this Node!");
		}
		uncheckedAddDatum(datumName, datum);
	}

	protected abstract void uncheckedAddDatum(String datumName, Object datum);

	protected void onDone() {
	}

	public boolean isUsable() {
		return usable;
	}

	public void setUsable(boolean usable) {
		this.usable = usable;
	}

	public double getProgress() { // 1 = usable, !1 = unusable. Might convert to boolean in the future.
		return progress;
	}

	public void setProgress(double progress) {
		this.progress = progress;
		if (progress == 1) onDone();
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
