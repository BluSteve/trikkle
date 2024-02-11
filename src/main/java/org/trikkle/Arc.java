package org.trikkle;

import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Arc implements Primable {
	private final Lock lock = new ReentrantLock();
	protected Overseer overseer;
	protected ArcStatus status = ArcStatus.IDLE;
	protected Set<Node> dependencies, outputNodes;
	private Node outputNode;
	private String name;

	public abstract void run(); // lambda won't work because it won't allow for multiple parameter inputs

	void runWrapper() {
		run();
	}

	protected Object getDatum(String datumName) {
		return overseer.getCache().get(datumName);
	}

	protected void returnDatum(String datumName, Object datum) {
		Node node = overseer.getNodeOfDatum(datumName);
		if (outputNodes.contains(node)) {
			node.addDatum(datumName, datum);
		} else {
			throw new IllegalArgumentException(
					"Arc " + name + " cannot return datum " + datumName + " because it is not an output of this arc!");
		}
	}

	public ArcStatus getStatus() {
		return status;
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

	protected Node getOutputNode() {
		return outputNode;
	}

	@Override
	public void primeWith(Overseer overseer) { // aka initialize
		this.overseer = overseer;
		Link link = overseer.getGraph().arcMap.get(this);
		dependencies = link.getDependencies();
		outputNodes = link.getOutputNodes();
		if (outputNodes.size() == 1) {
			outputNode = outputNodes.iterator().next();
		}
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
	public Lock getLock() {
		return lock;
	}
}
