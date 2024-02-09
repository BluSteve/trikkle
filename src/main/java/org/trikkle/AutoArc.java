package org.trikkle;

public abstract class AutoArc extends Arc {
	@Override
	void runWrapper() {
		status = ArcStatus.IN_PROGRESS;
		super.runWrapper();
		status = ArcStatus.FINISHED;
	}
}
