package org.trikkle;

public abstract class Arc implements Primable {
	protected Overseer overseer;
	protected ArcStatus status = ArcStatus.IDLE;
	protected Node outputNode;
	private String name;

	public abstract void run(); // lambda won't work because it won't allow for multiple parameter inputs

	void runWrapper() {
		outputNode = overseer.getOutputNodeOfArc(this);
		run();
	}

	protected Object getDatum(String datumName) {
		return overseer.getCache().get(datumName);
	}

	protected void returnDatum(String datumName, Object datum) {
		outputNode.addDatum(datumName, datum);
	}

	public ArcStatus getStatus() {
		return status;
	}

	@Override
	public void primeWith(Overseer overseer) {
		this.overseer = overseer;
	}

	@Override
	public Overseer getOverseer() {
		return overseer;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name.isEmpty()) this.name = null;
		else this.name = name;
	}

	public static abstract class AutoArc extends Arc {
		@Override
		void runWrapper() {
			outputNode = overseer.getOutputNodeOfArc(this);
			status = ArcStatus.IN_PROGRESS;
			run();
			status = ArcStatus.FINISHED;
		}
	}
}
