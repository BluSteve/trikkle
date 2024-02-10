package org.trikkle;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Arc implements Primable {
	private final Lock lock = new ReentrantLock();
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name.isEmpty()) this.name = null;
		else this.name = name;
	}

	@Override
	public void primeWith(Overseer overseer) {
		this.overseer = overseer;
	}

	@Override
	public void reset() {
		overseer = null;
		status = ArcStatus.IDLE;
		outputNode = null;
	}

	@Override
	public Lock getLock() {
		return lock;
	}
}
