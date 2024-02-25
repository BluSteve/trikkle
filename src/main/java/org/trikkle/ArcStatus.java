package org.trikkle;

/**
 * The status of an arc.
 *
 * @see Arc
 * @see AutoArc
 * @since 0.1.0
 */
public enum ArcStatus {
	/**
	 * Only idle arcs can be run by the overseer.
	 */
	IDLE(0),
	/**
	 * Once the overseer selects the arc for execution, it will be temporarily set to stand by so another thread cannot
	 * execute it simultaneously.
	 */
	STAND_BY(1),
	/**
	 * Once the arc begins to run, it will be in progress.
	 */
	IN_PROGRESS(2),
	/**
	 * Once the arc finishes, it cannot be set to any lower status. Finished is final. It can never be run again.
	 */
	FINISHED(3);

	private final int stage;

	ArcStatus(int stage) {
		this.stage = stage;
	}

	/**
	 * The stage of the status. The higher the stage, the more "finished" the status is.
	 *
	 * @return the stage of the status
	 */
	public int stage() {
		return stage;
	}
}
