package org.trikkle;

public abstract class AutoArc extends Arc {
	@Override
	void runWrapper() {
		setStatus(ArcStatus.IN_PROGRESS);
		run();
		setStatus(ArcStatus.FINISHED);
	}
}
