package org.trikkle;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Steve Cao
 * @since 0.1.0
 */
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
		if (overseer == null) {
			throw new IllegalStateException("Node " + this + " is not primed with an overseer!");
		}
		if (!datumNames.contains(datumName)) {
			throw new IllegalArgumentException("Datum " + datumName + " was not declared by this node!");
		}
		uncheckedAddDatum(datumName, datum);
	}

	protected abstract void uncheckedAddDatum(String datumName, Object datum);

	public boolean isUsable() {
		return usable;
	}

	/**
	 * Irreversibly sets the node to {@code usable}.
	 */
	public void setUsable() {
		this.usable = true;
	}

	public double getProgress() {
		return progress;
	}

	/**
	 * Sets the progress of the node. Must be between 0 and 1. If the progress is less than the current progress, the
	 * progress will not be updated.
	 * <p>
	 * If the progress is set to 1, the node will be set to {@code usable}.
	 *
	 * @param progress the progress of the node
	 * @return true if the progress was updated
	 * @throws IllegalArgumentException if the progress is not between 0 and 1
	 * @see #setUsable()
	 */
	public final boolean setProgress(double progress) {
		if (progress < 0 || progress > 1) {
			throw new IllegalArgumentException("Progress " + progress + " not between 0 and 1!");
		}
		if (progress < this.progress) {
			return false;
		}

		this.progress = progress;
		if (progress == 1) setUsable();
		return true;
	}

	/**
	 * Two nodes are equal if they are the same object. They are congruent if they have the same datum names.
	 *
	 * @param node the node to compare to
	 * @return true if the nodes are congruent
	 */
	@Override
	public boolean congruentTo(Node node) {
		return datumNames.equals(node.datumNames);
	}

	@Override
	public void primeWith(Overseer overseer) {
		this.overseer = overseer;
	}

	/**
	 * Resets the node to its initial state. The node will be set to not {@code usable} and have progress of 0. The
	 * overseer will be
	 * set to null.
	 */
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
