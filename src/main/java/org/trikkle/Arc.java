package org.trikkle;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Arc implements Primable {
	private final ReentrantLock lock = new ReentrantLock();
	private final boolean safe;
	protected Overseer overseer;
	private Set<Node> dependencies, outputNodes;
	private Node outputNode;
	private ArcStatus status = ArcStatus.IDLE;
	private String name;

	// wow overriding this is really clean
	// without this there are ways to get a ticktock by for example having a phantom stream node it outputs to
	public Arc(boolean safe) {
		this.safe = safe;
	}

	public abstract void run(); // lambda won't work because it won't allow for multiple parameter inputs

	void runWrapper() {
		run();
	}

	protected Object getDatum(String datumName) {
		return overseer.getCache().get(datumName);
	}

	protected void returnDatum(String datumName, Object datum) {
		Node node = overseer.getNodeOfDatum(datumName);
		if (node == null) {
			throw new IllegalArgumentException("No node is associated with datum " + datumName + "!");
		}

		if (outputNodes.contains(node)) {
			node.addDatum(datumName, datum);
		} else {
			throw new IllegalArgumentException(
					"Arc " + this + " cannot return datum " + datumName + " because it is not an output of this arc!");
		}
	}

	public ArcStatus getStatus() {
		return status;
	}

	protected synchronized void setStatus(ArcStatus status) {
		if (status == null) {
			throw new NullPointerException("Status cannot be null!");
		}
		if (status == this.status) return; // no change don't do anything
		if (this.status == ArcStatus.FINISHED) {
			throw new IllegalStateException("Arc " + this + " is already finished!");
		}
		if (safe && status.stage() < this.status.stage()) {
			throw new IllegalArgumentException(
					"Safe arc " + this + " cannot be set to " + status + " from " + this.status + "!");
		}
		this.status = status;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name.isEmpty()) {
			this.name = null;
		} else {
			this.name = name;
		}
	}

	public boolean isSafe() {
		return safe;
	}

	@Override
	public void primeWith(Overseer overseer) { // aka initialize
		this.overseer = overseer;
		Link link = overseer.g.arcMap.get(this);
		dependencies = link.getDependencies();
		outputNodes = link.getOutputNodes();
		if (outputNodes.size() == 1) {
			outputNode = outputNodes.iterator().next();
		}
	}

	protected Set<Node> getDependencies() {
		return dependencies;
	}

	protected Set<Node> getOutputNodes() {
		return outputNodes;
	}

	protected Node getOutputNode() {
		return outputNode;
	}

	@Override
	public void reset() {
		overseer = null;
		status = ArcStatus.IDLE;
		dependencies = null;
		outputNodes = null;
		outputNode = null;
	}

	@Override
	public ReentrantLock getLock() {
		return lock;
	}

	@Override
	public String toString() {
		if (name == null) {
			return super.toString();
		} else {
			return name;
		}
	}
}
