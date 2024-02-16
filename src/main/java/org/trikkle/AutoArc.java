package org.trikkle;

/**
 * An arc that automatically sets its status to {@link ArcStatus#IN_PROGRESS} before running and to
 * {@link ArcStatus#FINISHED} after running. Guaranteed to be safe (see {@link Arc#isSafe()}).
 *
 * @since 0.1.0
 */
public abstract class AutoArc extends Arc {
	/**
	 * Create an auto arc.
	 */
	public AutoArc() {
		super(true);
	}

	/**
	 * Create an auto arc with the given name.
	 *
	 * @param name the name of the arc
	 */
	public AutoArc(String name) {
		super(name, true);
	}

	@Override
	void runWrapper() {
		setStatus(ArcStatus.IN_PROGRESS);
		super.runWrapper();
		setStatus(ArcStatus.FINISHED);
	}
}
