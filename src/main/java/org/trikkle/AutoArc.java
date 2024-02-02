package org.trikkle;

public abstract class AutoArc extends Arc {
	@Override
	void runWrapper() {
		super.runWrapper();
		status = ArcStatus.FINISHED;
	}
}
