package org.trikkle;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public abstract class Node implements Primable, Congruent<Node> { // Generics are too restricting for this class
	public final Set<String> datumNames; // unique identifies a node
	protected Overseer overseer;
	private final AtomicBoolean usable = new AtomicBoolean(false); // can be true then false
	private final AtomicLong progress = new AtomicLong(0); // monotonically increasing

	protected Node(Set<String> datumNames) {
		this.datumNames = datumNames;
	}

	public void addDatum(String datumName, Object datum) {
		if (!datumNames.contains(datumName)) {
			throw new IllegalArgumentException("Datum \"" + datumName + "\" was not declared by this Node!");
		}
		uncheckedAddDatum(datumName, datum);
	}

	protected abstract void uncheckedAddDatum(String datumName, Object datum);

	protected void onDone() {
	}

	public boolean isUsable() {
		return usable.get();
	}

	public void setUsable(boolean usable) {
		this.usable.set(usable);
	}

	public double getProgress() {
		return Double.longBitsToDouble(progress.get());
	}

	public void setProgress(double progress) {
		if (progress < 0 || progress > 1) {
			throw new IllegalArgumentException("Progress not between 0 and 1!");
		}
		if (progress < Double.longBitsToDouble(this.progress.get())) {
			throw new IllegalArgumentException("Progress cannot decrease!");
		}

		this.progress.set(Double.doubleToLongBits(progress));
		if (progress == 1) {
			this.setUsable(true); // usable declares that this node is ready to be used
			onDone();
		}
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
		usable.set(false);
		progress.set(0);
		overseer = null;
	}

	@Override
	public String toString() {
		return "Node" + datumNames.toString();
	}
}
