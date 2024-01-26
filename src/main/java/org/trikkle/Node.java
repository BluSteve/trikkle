package org.trikkle;

public abstract class Node implements Primable {
	protected int index;
	protected Overseer overseer;
	protected double progress;
	protected String[] datumNames;

	@Override
	public void primeWith(Overseer overseer) {
		this.overseer = overseer;
	}

	@Override
	public Overseer getOverseer() {
		return overseer;
	}

	public void setDatum(String datumName, Object datum) {
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public double getProgress() {
		return progress;
	}
}
