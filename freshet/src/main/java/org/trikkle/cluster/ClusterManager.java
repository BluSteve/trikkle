package org.trikkle.cluster;

import org.trikkle.InitialData;
import org.trikkle.Serializer;

import java.io.*;
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
public class ClusterManager {
	public int port;
	public String password;
	public Queue<Machine> machines = new ConcurrentLinkedQueue<>();

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
			while (true) {
				Socket socket = serverSocket.accept();
				// new machine connected
				DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

				char dataType = in.readChar();
				int length = in.readInt();
				byte[] data = new byte[length];
				in.readFully(data);

				if (dataType == 'a') {
					InitialData initialData = (InitialData) Serializer.deserialize(data);

					if (initialData.password.equals(password)) {
						Machine machine = new Machine(initialData.publicKey, initialData.ip, initialData.port);
						System.out.println("machine = " + machine);
						machines.add(machine);
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
