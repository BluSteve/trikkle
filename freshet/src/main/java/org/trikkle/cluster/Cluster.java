package org.trikkle.cluster;

import java.util.List;

// a cluster contains a list of machines. it allows adding new machines and remove existing ones.
public class Cluster {
	public List<Machine> machines;

	public void addMachine(Machine machine) {
		machines.add(machine);
	}

	public void removeMachine(Machine machine) {
		machines.remove(machine);
	}
}
