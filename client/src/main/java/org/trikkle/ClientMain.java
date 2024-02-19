package org.trikkle;

import org.trikkle.cluster.ClusterManager;
import org.trikkle.serial.JarInfo;
import org.trikkle.serial.MachineInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class ClientMain {
	public static void main(String[] args) throws IOException, InterruptedException {
		// join a cluster and then upload a jar to all the machines in the cluster
		ClusterManager clusterManager = new ClusterManager(995, "password");
		new Thread(clusterManager::start).start();

		clusterManager.started.acquire();

		Scanner scanner = new Scanner(System.in);
		while (true) {
			String line = scanner.nextLine();
			if (line.equals("j")) {
				System.out.println("sending jar");
				JarInfo jarInfo = new JarInfo("org.trikkle.Handlers",
						Files.readAllBytes(Paths.get("client/build/libs/client.jar")));
				clusterManager.uploadJar(jarInfo);
			} else if (line.equals("t")) {
				System.out.println("sending test message");
				for (MachineInfo machine : clusterManager.machines) {
					TlvMessage tlvMessage = new TlvMessage('t', "hello world".getBytes());
					machine.sendMessage(tlvMessage);
				}
			}
		}
	}
}
