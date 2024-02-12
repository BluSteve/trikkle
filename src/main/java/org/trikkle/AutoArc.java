package org.trikkle;

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
