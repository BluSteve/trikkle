package org.trikkle;

import org.trikkle.annotations.Input;
import org.trikkle.annotations.Output;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Steve Cao
 * @since 0.1.0
 */
public abstract class Arc implements Primable {
	private final ReentrantLock lock = new ReentrantLock();
	private final boolean safe;
	protected Overseer overseer;
	private Link link;
	private ArcStatus status = ArcStatus.IDLE;
	private String name;

	// wow overriding this is really clean
	// without this there are ways to get a ticktock by for example having a phantom stream node it outputs to
	public Arc(boolean safe) {
		this.safe = safe;
	}

	public abstract void run(); // lambda won't work because it won't allow for multiple parameter inputs

	void runWrapper() {
		autoFill();
		run();
		autoReturn();
	}

	protected Object getDatum(String datumName) {
		return overseer.getCache().get(datumName);
	}

	protected void returnDatum(String datumName, Object datum) {
		Node node = overseer.getNodeOfDatum(datumName);
		if (node == null) {
			throw new IllegalArgumentException("No node is associated with datum " + datumName + "!");
		}

		if (getOutputNodes().contains(node)) {
			node.addDatum(datumName, datum);
		} else {
			throw new IllegalArgumentException(
					"Arc " + this + " cannot return datum " + datumName + " because it is not an output of this arc!");
		}
	}

	private void autoFill() {
		Field[] fields = getClass().getDeclaredFields();
		try {
			for (Field field : fields) {
				if (field.isAnnotationPresent(Input.class)) {
					String name = field.getAnnotation(Input.class).name();
					if (name.isEmpty()) {
						name = field.getName();
					}
					field.set(this, getDatum(name));
				}
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void autoReturn() {
		Field[] fields = getClass().getDeclaredFields();
		try {
			for (Field field : fields) {
				if (field.isAnnotationPresent(Output.class)) {
					String name = field.getAnnotation(Output.class).name();
					if (name.isEmpty()) {
						name = field.getName();
					}
					returnDatum(name, field.get(this));
				}
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
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
