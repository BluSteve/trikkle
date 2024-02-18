package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.cluster.ClusterManager;

class MachineMainTest {
	@Test
	void testMain() {
		Thread thread = new Thread(() -> {
			ClusterManager clusterManager = new ClusterManager(995, "password");
			clusterManager.start();
		});
		thread.start();
		System.out.println("here");

		MachineMain machineMain = new MachineMain("0.0.0.0", 999);
		machineMain.register("localhost", 995, "password");
		machineMain.startListening();

		MachineMain machineMain2 = new MachineMain("0.0.0.0", 992);
		machineMain2.register("localhost", 995, "password");
		machineMain2.startListening();

		System.out.println("machineMain = " + machineMain.machines);
		System.out.println("machineMain2 = " + machineMain2.machines);
	}

	@Test
	void secondMachine() {
		MachineMain machineMain2 = new MachineMain("0.0.0.0", 992);
		machineMain2.register("localhost", 995, "password");
		machineMain2.startListening();
	}
}
