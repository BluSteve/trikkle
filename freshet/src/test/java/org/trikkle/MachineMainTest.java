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
		MachineMain machineMain = new MachineMain("localhost", 999);
		machineMain.register("localhost", 995, "password");
		MachineMain machineMain2 = new MachineMain("localhost", 992);
		machineMain2.register("localhost", 995, "password");

		new Thread(machineMain::startListening).start();

		MachineMain.sendToMachine(
	}
}
