package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.cluster.ClusterManager;
import org.trikkle.cluster.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

class MachineMainTest {
	private static final int MANAGER_PORT = 995;

	static Pair<MachineMain, Thread> newMachine(int port) {
		MachineMain machineMain = new MachineMain("0.0.0.0", port);
		Thread thread = new Thread(() -> {
			machineMain.register("localhost", MANAGER_PORT, "password");
			machineMain.startListening();
		});
		thread.start();
		return new Pair<>(machineMain, thread);
	}

	@Test
	void testMain() throws IOException {
		Semaphore semaphore = new Semaphore(0);

		new Thread(() -> {
			ClusterManager clusterManager = new ClusterManager(MANAGER_PORT, "password");
			clusterManager.pollingInterval = 10;
			clusterManager.start(semaphore);
		}).start();

		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		List<MachineMain> instances = new ArrayList<>();
		List<Thread> threads = new ArrayList<>();
		for (int i = 9000; i < 9005; i++) {
			var pair = newMachine(i);
			instances.add(pair.key);
			threads.add(pair.value);
		}

		Utils.sleep(500);
		instances.getFirst().serverSocket.close();
		Utils.sleep(10000);
	}

	@Test
	void secondMachine() {
		MachineMain machineMain2 = new MachineMain("0.0.0.0", 992);
		machineMain2.register("localhost", MANAGER_PORT, "password");
		machineMain2.startListening();
	}

	public record Pair<K, V>(K key, V value) {
	}
}
