package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.cluster.ClusterManager;
import org.trikkle.cluster.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class MachineMainTest {
	private static final int MANAGER_PORT = 995;

	static Pair<MachineMain, Thread> newMachine(int port) {
		MachineMain machineMain = new MachineMain("0.0.0.0", port);
		Thread thread = new Thread(() -> {
			machineMain.register("localhost", MANAGER_PORT, "password");
			machineMain.startListening();
		});
		thread.start();
		machineMain.listening.acquireUninterruptibly();
		return new Pair<>(machineMain, thread);
	}

	@Test
	void testMain() throws IOException {
		ClusterManager clusterManager = new ClusterManager(MANAGER_PORT, "password");
		new Thread(() -> {
			clusterManager.pollingInterval = 10;
			clusterManager.start();
		}).start();

		try {
			clusterManager.started.acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		List<MachineMain> instances = new ArrayList<>();
		List<Thread> threads = new ArrayList<>();
		for (int i = 19100; i < 19105; i++) {
			var pair = newMachine(i);
			instances.add(pair.key);
			threads.add(pair.value);
		}

		instances.getFirst().serverSocket.close();
		Utils.sleep(60000);
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
