package org.trikkle.cluster;

import org.trikkle.InitialData;
import org.trikkle.Serializer;
import org.trikkle.TlvMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
// web frontend with a manually opened port that potential new machines can connect to.
// kind of like an admissions officer or bouncer
// requires a password to enter
// updates all machines in the cluster when a new machine joins. gives the new machine this data (machine list) too.

// maybe ssh handshake at some point in the future, for now just assume that if a machine can give you their public key,
// it has the private key too.
// machine is identified by its public key

// after first connection (which is transient), the manager communicates with the machine through its designated port
public class ClusterManager {
	public int port;
	public String password;
	public Queue<MachineInfo> machines = new ConcurrentLinkedQueue<>();

	public ClusterManager(int port, String password) {
		this.port = port;
		this.password = password;
	}

	public static void main(String[] args) {
		ClusterManager clusterManager = new ClusterManager(995, "password");
		clusterManager.start();
	}

	public void start() {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			while (!Thread.currentThread().isInterrupted()) {
				Socket socket = serverSocket.accept();
				// new machine connected
				TlvMessage message = TlvMessage.readFrom(socket.getInputStream());

				if (message.dataType == 'a') {
					InitialData initialData = (InitialData) Serializer.deserialize(message.data);

					if (initialData.password.equals(password)) {
						String ip = initialData.ip.isEmpty() ? socket.getInetAddress().getHostAddress() : initialData.ip;
						MachineInfo machine = new MachineInfo(initialData.publicKey, ip, initialData.port);
						System.out.println("machine = " + machine);
						machines.add(machine);

						MachineInfo[] machinesArray = machines.toArray(new MachineInfo[0]);
						TlvMessage response = new TlvMessage('u', Serializer.serialize(machinesArray));
						response.writeTo(socket.getOutputStream());
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void updateCluster() {
	}
}
