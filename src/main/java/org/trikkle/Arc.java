package org.trikkle;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A wrapper for a function that can be run by an overseer.
 *
 * @since 0.1.0
 */
public abstract class Arc implements Primable {
	private final ReentrantLock lock = new ReentrantLock();
	private final boolean safe;
	private String name;

	private long startTime = -1, endTime = -1;
	private final Set<String> inputDatumNames, outputDatumNames;
	private ArcStatus status = ArcStatus.IDLE;
	private Overseer overseer;
	private Link link;
	private Set<Node> outputNodesRemaining; // could be stale

	/**
	 * A safe arc is one that cannot be set to a status that is less than its current status. This means it can only be
	 * run once. This is useful for preventing deadlocks and livelocks, as even though arcs cannot directly
	 * ticktock an overseer, it can easily do so indirectly by leveraging nodes and safe arcs.
	 *
	 * @param safe true if this arc is safe
	 */
	public Arc(boolean safe) {
		this.safe = safe;

		inputDatumNames = new HashSet<>();
		outputDatumNames = new HashSet<>();
		for (Field field : this.getClass().getDeclaredFields()) {
			boolean in = field.getName().endsWith("$in");
			boolean out = field.getName().endsWith("$out");

			if (in || out) {
				try {
					String s = (String) field.get(this);
					if (in) inputDatumNames.add(s);
					if (out) outputDatumNames.add(s);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	/**
	 * A safe arc is one that cannot be set to a status that is less than its current status. This means it can only be
	 * run once. This is useful for preventing deadlocks and livelocks, as even though arcs cannot directly
	 * ticktock an overseer, it can easily do so indirectly by leveraging nodes and safe arcs.
	 *
	 * @param name the name of this arc
	 * @param safe true if this arc is safe
	 */
	public Arc(String name, boolean safe) {
		this(safe);
		setName(name);
	}

	/**
	 * Override this method to specify what functions to run when this arc is run. This method is called by the overseer
	 * if the link containing this arc is {@link Link#runnable()}.
	 *
	 * @see Link
	 * @see Overseer
	 */
	protected abstract void run(); // lambda won't work because it won't allow for multiple parameter inputs

	void runWrapper() {
		run();
	}

	/**
	 * Gets the datum with the given name from the overseer's cache.
	 *
	 * @param datumName the name of the datum
	 * @return the datum with the given name
	 * @throws NullPointerException if there is no datum with the given name in the cache
	 */
	protected <T> T getDatum(String datumName) {
		if (!overseer.getCache().containsKey(datumName)) {
			throw new NullPointerException("No datum with name " + datumName + " is in the cache!");
		}
		//noinspection unchecked
		return (T) overseer.getCache().get(datumName);
	}

	/**
	 * Gets the datum with the given name from the overseer's cache. If the datum is not in the cache, the default value
	 * is returned.
	 *
	 * @param datumName the name of the datum
	 * @return the datum with the given name, or the default value if the datum is not in the cache
	 */
	protected <T> T getDatum(String datumName, T defaultValue) {
		if (!overseer.getCache().containsKey(datumName)) {
			return defaultValue;
		}
		//noinspection unchecked
		return (T) overseer.getCache().get(datumName);
	}

	/**
	 * Returns the datum with the given name to the overseer's cache by calling {@link Node#addDatum(String, Object)}.
	 * The datum can be null.
	 *
	 * @param datumName the name of the datum
	 * @param datum     the datum to return
	 * @throws NullPointerException     if the datum name is not associated with any node
	 * @throws IllegalArgumentException if the node with this datum is not an output of this arc
	 */
	protected final void returnDatum(String datumName, Object datum) {
		Node node = overseer.getNodeOfDatum(datumName);
		if (node == null) {
			throw new NullPointerException("No output node is associated with datum " + datumName + "!");
		}

		if (getOutputNodes().contains(node)) {
			node.addDatum(datumName, datum);
		} else {
			throw new IllegalArgumentException(
					"Arc " + this + " cannot return datum " + datumName + " because it is not an output of this arc!");
		}
	}

	public ArcStatus getStatus() {
		return status;
	}

	/**
	 * Sets the status of the arc. The status cannot be set to null. The status cannot be changed if the arc is already
	 * finished. If the arc is safe, the status cannot be set to a status that is less (see {@link ArcStatus#stage()})
	 * than the current status.
	 *
	 * @param status the new status of the arc
	 * @throws NullPointerException     if the status is null
	 * @throws IllegalStateException    if the arc is already finished
	 * @throws IllegalArgumentException if the arc is safe and the status is less than the current status
	 */
	protected synchronized void setStatus(ArcStatus status) {
		if (status == null) {
			throw new NullPointerException("Status cannot be null!");
		}
		if (status == this.status) return; // no change don't do anything
		if (this.status == ArcStatus.FINISHED) {
			throw new IllegalStateException("Arc " + this + " is already finished!");
		}
		if (safe && status.stage() < this.status.stage()) {
			throw new IllegalArgumentException(
					"Safe arc " + this + " cannot be set to " + status + " from " + this.status + "!");
		}

		this.status = status;
		if (this.status == ArcStatus.IN_PROGRESS && startTime == -1) {
			startTime = System.nanoTime();
		} else if (this.status == ArcStatus.FINISHED) {
			endTime = System.nanoTime();
		}
	}

	synchronized int getOutputNodesRemaining() { // this can determine if there are no more undone output nodes in O(n)
		// time
		outputNodesRemaining.removeIf((node) -> node.getProgress() == 1);
		return outputNodesRemaining.size();
	}

	public String getName() {
		return name;
	}

	/**
	 * Sets the name of this arc. If the name is empty, the name is set to null.
	 *
	 * @param name the new name of this arc
	 */
	public void setName(String name) {
		if (name.isEmpty()) {
			this.name = null;
		} else {
			this.name = name;
		}
	}

	/**
	 * A safe arc is one that cannot be set to a status that is less than its current status. This means it can only be
	 * run once.
	 *
	 * @return true if this arc is safe
	 */
	public boolean isSafe() {
		return safe;
	}

	@Override
	public void primeWith(Overseer overseer) { // aka initialize
		this.overseer = overseer;
		link = overseer.g.arcMap.get(this);
		outputNodesRemaining = new HashSet<>(link.getOutputNodes());
	}

	public Set<String> getInputDatumNames() { // returns object itself, not a copy
		return inputDatumNames;
	}

	public Set<String> getOutputDatumNames() {
		return outputDatumNames;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	/**
	 * Gets the link this arc is a part of. Only available after the arc is primed.
	 *
	 * @return the link this arc is a part of
	 */
	protected Link getLink() {
		return link;
	}

	/**
	 * Gets the input nodes of this arc. Same as {@link Link#getInputNodes()}. Only available after the arc is primed.
	 *
	 * @return the input nodes of this arc
	 */
	protected Set<Node> getInputNodes() {
		return link.getInputNodes();
	}

	/**
	 * Gets the output nodes of this arc. Same as {@link Link#getOutputNodes()}. Only available after the arc is primed.
	 *
	 * @return the output nodes of this arc
	 */
	protected Set<Node> getOutputNodes() {
		return link.getOutputNodes();
	}

	/**
	 * Gets the output node of this arc. Same as {@link Link#getOutputNode()}. Only available after the arc is primed.
	 *
	 * @return the output node of this arc
	 */
	protected Node getOutputNode() {
		return link.getOutputNode();
	}

	protected Overseer getOverseer() {
		return overseer;
	}

	/**
	 * Resets the arc to its initial state. If you declare any persistent fields in an arc, you should override this and
	 * provide a way to reset them.
	 */
	@Override
	public void reset() {
		overseer = null;
		status = ArcStatus.IDLE;
		link = null;
		startTime = -1;
		endTime = -1;
		outputNodesRemaining = null;
	}

	@Override
	public ReentrantLock getLock() {
		return lock;
	}

	@Override
	public String toString() {
		if (name == null) {
			return super.toString();
		} else {
			return name;
		}
	}
}
