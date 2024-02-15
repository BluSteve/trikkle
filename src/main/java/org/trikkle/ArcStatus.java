package org.trikkle;

/**
 * The status of an arc.
 *
 * @author Steve Cao
 * @see Arc
 * @see AutoArc
 * @since 0.1.0
 */
public enum ArcStatus {
	IDLE(0),
	STAND_BY(1),
	IN_PROGRESS(2),
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
