package org.trikkle;

/**
 * An arc that automatically sets its status to {@link ArcStatus#IN_PROGRESS} before running and to
 * {@link ArcStatus#FINISHED} after running. Guaranteed to be safe (see {@link Arc#isSafe()}).
 *
 * @author Steve Cao
 * @since 0.1.0
 */
public abstract class AutoArc extends Arc {
	public AutoArc() {
		super(true);
	}

	@Override
	void runWrapper() {
		setStatus(ArcStatus.IN_PROGRESS);
		run();
		setStatus(ArcStatus.FINISHED);
	}
}
