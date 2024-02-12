package org.trikkle;

public enum ArcStatus {
	IDLE(0),
	STAND_BY(1),
	IN_PROGRESS(2),
	FINISHED(3);

	private final int stage;

	ArcStatus(int stage) {
		this.stage = stage;
	}

	public int stage() {
		return stage;
	}
}
