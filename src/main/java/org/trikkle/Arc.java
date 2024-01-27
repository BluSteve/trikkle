package org.trikkle;

public abstract class Arc implements Primable {
	private Overseer overseer;
	protected ArcStatus status = ArcStatus.IDLE;

	public abstract void run(); // lambda won't work because it won't allow for multiple parameter inputs

	void runWrapper() {
		status = ArcStatus.IN_PROGRESS;
		run();
		status = ArcStatus.FINISHED;
	}

	protected Object getDatum(String datumName) {
		return overseer.getCache().get(datumName);
	}

	protected void returnDatum(String datumName, Object datum) {
		overseer.getOutputNode(this).addDatum(datumName, datum);
	}

	@Override
	public void primeWith(Overseer overseer) {
		this.overseer = overseer;
	}

	@Override
	public Overseer getOverseer() {
		return overseer;
	}
}
