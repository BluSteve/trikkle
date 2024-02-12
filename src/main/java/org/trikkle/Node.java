package org.trikkle;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Node implements Primable, Congruent<Node> { // Generics are too restricting for this class
	public final Set<String> datumNames; // unique identifies a node
	private final ReentrantLock lock = new ReentrantLock();
	protected Overseer overseer;
	private boolean usable = false; // once true cannot be false
	private double progress = 0; // monotonically increasing

	protected Node(Set<String> datumNames) {
		for (String datumName : datumNames) {
			if (datumName == null) {
				throw new NullPointerException("Datum name cannot be null!");
			}
		}
		this.datumNames = datumNames;
	}

	public void addDatum(String datumName, Object datum) {
		if (!datumNames.contains(datumName)) {
			throw new IllegalArgumentException("Datum " + datumName + " was not declared by this node!");
		}
		uncheckedAddDatum(datumName, datum);
	}

	protected abstract void uncheckedAddDatum(String datumName, Object datum);

	public boolean isUsable() {
		return usable;
	}

	public void setUsable() {
		this.usable = true;
	}

	public double getProgress() {
		return progress;
	}

	public final boolean setProgress(double progress) {
		if (progress < 0 || progress > 1) {
			throw new IllegalArgumentException("Progress " + progress + " not between 0 and 1!");
		}
		if (progress < this.progress) {
			return false;
		}

		this.progress = progress;
		if (progress == 1) setUsable(); // IF progress is 1 THEN usable is true
		return true;
	}

	@Override
	public boolean congruentTo(Node node) {
		return datumNames.equals(node.datumNames);
	}

	@Override
	public void primeWith(Overseer overseer) {
		this.overseer = overseer;
	}

	@Override
	public void reset() {
		usable = false;
		progress = 0;
		overseer = null;
	}

	@Override
	public ReentrantLock getLock() {
		return lock;
	}

	@Override
	public String toString() {
		return "Node" + datumNames;
	}
}
