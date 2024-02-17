package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.cluster.ClusterManager;

import java.util.concurrent.TimeUnit;

class MachineMainTest {
	@Test
	void testMain() {
		Thread thread = new Thread(() -> {
			ClusterManager clusterManager = new ClusterManager(995, "password");
			clusterManager.start();
		});
		thread.start();
		System.out.println("here");
		MachineMain machineMain = new MachineMain(999);
		machineMain.register("localhost", 995, "password");
		MachineMain machineMain2 = new MachineMain(992);
		machineMain2.register("localhost", 995, "password");

		machineMain.startListening();
		machineMain2.startListening();

		extracted();
		machineMain2.sendToMachine(machineMain2.machines.getFirst(), new TlvMessage('t', "test".getBytes()));
		extracted();
	}

	private void extracted() {
		try {
			TimeUnit.MILLISECONDS.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
