package org.trikkle;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A vertex in the execution graph representing datums to be filled.
 *
 * @see Arc
 * @see Link
 * @see Graph
 * @since 0.1.0
 */
public abstract class Node implements Primable, Congruent<Node> { // Generics are too restricting for this class
	public final Set<String> pointers; // unique identifies a node
	private final ReentrantLock lock = new ReentrantLock();
	protected Overseer overseer;
	private boolean usable = false; // once true cannot be false
	private double progress = 0; // monotonically increasing

	protected Node(Set<String> pointers) {
		for (String pointer : pointers) {
			if (pointer == null) {
				throw new NullPointerException("Pointer cannot be null!");
			}
		}
		this.pointers = pointers;
	}

	/**
	 * Adds a datum to the overseer cache of this node. The datum must have been declared by this node.
	 *
	 * @param pointer the pointer to the datum
	 * @param datum   the datum
	 * @throws IllegalStateException    if the node is not primed with an overseer
	 * @throws IllegalArgumentException if the datum was not declared by the node
	 */
	public final void addDatum(String pointer, Object datum) {
		if (overseer == null) {
			throw new IllegalStateException("Node " + this + " is not primed with an overseer!");
		}
		if (!pointers.contains(pointer)) {
			throw new IllegalArgumentException("Datum " + pointer + " was not declared by this node!");
		}
		uncheckedAddDatum(pointer, datum);
	}

	protected abstract void uncheckedAddDatum(String pointer, Object datum);

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
	 * Two nodes are equal if they are the same object. They are congruent if they have the same pointers.
	 *
	 * @param node the node to compare to
	 * @return true if the nodes are congruent
	 */
	@Override
	public boolean congruentTo(Node node) {
		return pointers.equals(node.pointers);
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
		return "Node" + pointers;
	}
}
