package org.trikkle;

public class Arc implements Primable {
	private int index;
	private Overseer overseer;
	private ArcStatus status;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
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
