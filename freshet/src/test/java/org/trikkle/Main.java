package org.trikkle;

import org.trikkle.cluster.ClusterManager;
import org.trikkle.serial.JarInfo;
import org.trikkle.serial.MachineInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
	public static void main(String[] args) throws IOException, InterruptedException {
		// join a cluster and then upload a jar to all the machines in the cluster
		ClusterManager clusterManager = new ClusterManager(995, "password");
		new Thread(clusterManager::start).start();

		clusterManager.started.acquire();

		Scanner scanner = new Scanner(System.in);
		while (true) {
			String line = scanner.nextLine();
			if (line.equals("exit")) {
				System.exit(0);
			}
			if (line.equals("t")) {
				System.out.println("sending test message");
				for (MachineInfo machine : clusterManager.machines) {
					TlvMessage tlvMessage = new TlvMessage('t', "hello world".getBytes());
					machine.sendMessage(tlvMessage);
				}
			} else if (line.equals("s")) {
				System.out.println("starting overseer");
//				org.trikkle.Handlers.handleStart(null, null);
				for (MachineInfo machine : clusterManager.machines) {
					TlvMessage tlvMessage = new TlvMessage('s', new byte[0]);
					machine.sendMessage(tlvMessage);
				}
			} else if (line.equals("j")) {
				JarInfo jarInfo =
						new JarInfo("org.trikkle.Handlers", Files.readAllBytes(Paths.get("client/build/libs/client.jar")));
				for (MachineInfo machine : clusterManager.machines) {
					TlvMessage tlvMessage = new TlvMessage('j', Serializer.serialize(jarInfo));
					machine.sendMessage(tlvMessage);
				}
			}
		}
	}
}
