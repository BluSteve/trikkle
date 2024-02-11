package org.trikkle;

public abstract class AutoArc extends Arc {
	@Override
	void runWrapper() {
		setStatus(ArcStatus.IN_PROGRESS);
		super.runWrapper();
		setStatus(ArcStatus.FINISHED);
	}
}
