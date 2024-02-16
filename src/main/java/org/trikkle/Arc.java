package org.trikkle;

import org.trikkle.annotations.Input;
import org.trikkle.annotations.Output;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A wrapper for a function that can be run by an overseer.
 *
 * @since 0.1.0
 */
public abstract class Arc implements Primable {
	final Map<String, Field> inputFields, outputFields;
	private final ReentrantLock lock = new ReentrantLock();
	private final boolean safe;
	protected Overseer overseer;
	private Link link;
	private ArcStatus status = ArcStatus.IDLE;
	private String name;

	/**
	 * A safe arc is one that cannot be set to a status that is less than its current status. This means it can only be
	 * run once. This is useful for preventing deadlocks and livelocks, as even though arcs cannot directly
	 * {@link Overseer#ticktock(Node)} an overseer, it can easily do so indirectly by leveraging nodes and safe arcs.
	 *
	 * @param safe true if this arc is safe
	 */
	public Arc(boolean safe) {
		this.safe = safe;

		Field[] fields = getClass().getDeclaredFields();
		inputFields = new HashMap<>();
		outputFields = new HashMap<>();
		for (Field field : fields) {
			if (field.isAnnotationPresent(Input.class)) {
				String name = field.getAnnotation(Input.class).name();
				if (!name.isEmpty()) {
					inputFields.put(name, field);
				} else {
					inputFields.put(field.getName(), field);
				}
			} else if (field.isAnnotationPresent(Output.class)) {
				String name = field.getAnnotation(Output.class).name();
				if (!name.isEmpty()) {
					outputFields.put(name, field);
				} else {
					outputFields.put(field.getName(), field);
				}
			}
		}
	}

	/**
	 * A safe arc is one that cannot be set to a status that is less than its current status. This means it can only be
	 * run once. This is useful for preventing deadlocks and livelocks, as even though arcs cannot directly
	 * {@link Overseer#ticktock(Node)} an overseer, it can easily do so indirectly by leveraging nodes and safe arcs.
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
		autoFill();
		run();
		autoReturn();
	}

	/**
	 * Gets the datum with the given name from the overseer's cache.
	 *
	 * @param datumName the name of the datum
	 * @return the datum with the given name
	 * @throws NullPointerException if there is no datum with the given name in the cache
	 */
	protected Object getDatum(String datumName) {
		if (!overseer.getCache().containsKey(datumName)) {
			throw new NullPointerException("No datum with name " + datumName + " is in the cache!");
		}
		return overseer.getCache().get(datumName);
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
	protected void returnDatum(String datumName, Object datum) {
		Node node = overseer.getNodeOfDatum(datumName);
		if (node == null) {
			throw new NullPointerException("No node is associated with datum " + datumName + "!");
		}

		if (getOutputNodes().contains(node)) {
			node.addDatum(datumName, datum);
		} else {
			throw new IllegalArgumentException(
					"Arc " + this + " cannot return datum " + datumName + " because it is not an output of this arc!");
		}
	}

	private void autoFill() {
		for (Map.Entry<String, Field> inputEntry : inputFields.entrySet()) {
			String datumName = inputEntry.getKey();
			Field field = inputEntry.getValue();
			Object datum = getDatum(datumName);
			try {
				field.set(this, datum);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void autoReturn() {
		for (Map.Entry<String, Field> outputEntry : outputFields.entrySet()) {
			String datumName = outputEntry.getKey();
			Field field = outputEntry.getValue();
			Object datum;
			try {
				datum = field.get(this);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			returnDatum(datumName, datum);
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
	 * Gets the dependencies of this arc. Same as {@link Link#getDependencies()}. Only available after the arc is primed.
	 *
	 * @return the dependencies of this arc
	 */
	protected Set<Node> getDependencies() {
		return link.getDependencies();
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

	/**
	 * Resets the arc to its initial state. If you declare any persistent fields in an arc, you should override this and
	 * provide a way to reset them.
	 */
	@Override
	public void reset() {
		overseer = null;
		status = ArcStatus.IDLE;
		link = null;
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
