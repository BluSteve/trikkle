package org.trikkle;

import java.util.Set;

public abstract class Node implements Primable { // Generics are too restricting for this class
	public Set<String> datumNames; // unique identifies a node
	protected Overseer overseer;
	protected boolean usable; // can be true then false
	private double progress; // monotonically increasing

	public Node(Set<String> datumNames) {
		this.datumNames = datumNames;
	}

	// todo no two nodes can exist with the same datum names!! then remove the congruentTo method and put back the
	//  overrided equals and hashcode methods
	public void addDatum(String datumName, Object datum) {
		if (!datumNames.contains(datumName)) {
			throw new IllegalArgumentException("Datum \"" + datumName + "\" was not declared by this Node!");
		}
		uncheckedAddDatum(datumName, datum);
	}

	protected abstract void uncheckedAddDatum(String datumName, Object datum);

	protected void onDone() {
	}

	public boolean isUsable() {
		return usable;
	}

	public void setUsable(boolean usable) {
		this.usable = usable;
	}

	public double getProgress() {
		return progress;
	}

	public void setProgress(double progress) {
		if (progress < 0 || progress > 1) {
			throw new IllegalArgumentException("Progress not between 0 and 1!");
		}
		if (progress < this.progress) {
			throw new IllegalArgumentException("Progress cannot decrease!");
		}

		this.progress = progress;
		if (progress == 1) {
			usable = true; // usable declares that this node is ready to be used
			onDone();
		}
	}

	public boolean congruentTo(Node node) {
		return datumNames.equals(node.datumNames);
	}

	@Override
	public void primeWith(Overseer overseer) {
		this.overseer = overseer;
	}

	@Override
	public Overseer getOverseer() {
		return overseer;
	}

	@Override
	public String toString() {
		return "Node" + datumNames.toString();
	}
}
