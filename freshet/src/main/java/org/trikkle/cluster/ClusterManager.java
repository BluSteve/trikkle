package org.trikkle.cluster;

import org.trikkle.Machine;
import org.trikkle.Serializer;
import org.trikkle.TlvMessage;
import org.trikkle.serial.InitialData;
import org.trikkle.serial.JarInfo;
import org.trikkle.serial.MachineInfo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
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
	public Collection<Machine> machines = new ConcurrentLinkedQueue<>();
	public long pollingInterval = 10000;
	public Semaphore started = new Semaphore(0);
	public JarInfo currentJar;

	public ClusterManager(int port, String password) {
		this.port = port;
		this.password = password;
	}

	public static void main(String[] args) {
		ClusterManager clusterManager = new ClusterManager(995, "password");
		clusterManager.start();
	}

	public void start() {
		new Thread(() -> {
			// poll machines
			while (!Thread.currentThread().isInterrupted()) {
				for (Machine machine : machines) {
					// if machine is not reachable, remove it from the list
					try {
						TlvMessage message = new TlvMessage('p', new byte[0]);
						machine.sendPlainMessage(message);
					} catch (IOException e) {
						System.out.println("Machine " + machine + " is not reachable. Removing from list.");
						machines.remove(machine);
						System.out.println("machines = " + machines);
						try {
							announceRemove(machine);
						} catch (IOException ioException) {
							ioException.printStackTrace();
						}
					}
				}
				Utils.sleep(pollingInterval); // poll every x milliseconds
			}
		}).start();

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("ClusterManager started on port " + port + " with password \"" + password + "\"");
			started.release();

			while (!Thread.currentThread().isInterrupted()) {
				Socket socket = serverSocket.accept();
				// new machine connected
				TlvMessage message = TlvMessage.readFrom(socket.getInputStream());

				if (message.dataType == 'a') {
					InitialData initialData = (InitialData) Serializer.deserialize(message.data);

					if (initialData.password.equals(password)) {
						String ip = initialData.ip.isEmpty() ? socket.getInetAddress().getHostAddress() : initialData.ip;
						Machine machine = new Machine(new MachineInfo(initialData.publicKey, ip, initialData.port), socket);
						machines.add(machine);
						System.out.println("New machine " + machine + " connected!");

						// replies with current list of machines
						Machine[] machinesArray = machines.toArray(new Machine[0]);
						TlvMessage response = new TlvMessage('u', Serializer.serialize(machinesArray));
						machine.sendMessage(response);

						// give the machine the latest jar with a new socket, can put elsewhere
						if (currentJar != null) {
							TlvMessage jarMessage = new TlvMessage('j', Serializer.serialize(currentJar));
							machine.sendMessage(jarMessage);
						}

						// tells other machines about the new machine
						announceNew(machine);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void broadcastJar(JarInfo jarInfo) throws IOException {
		currentJar = jarInfo;
		for (Machine machine : machines) {
			TlvMessage message = new TlvMessage('j', Serializer.serialize(jarInfo));
			machine.sendMessage(message);
		}
	}

	private void announceNew(Machine machine) throws IOException {
		for (Machine otherMachine : machines) {
			if (!otherMachine.equals(machine)) {
				TlvMessage updateMessage = new TlvMessage('n', Serializer.serialize(machine.info));
				otherMachine.sendMessage(updateMessage);
			}
		}
	}

	private void announceRemove(Machine machine) throws IOException {
		for (Machine otherMachine : machines) {
			if (!otherMachine.equals(machine)) {
				TlvMessage updateMessage = new TlvMessage('r', Serializer.serialize(machine.info));
				otherMachine.sendMessage(updateMessage);
			}
		}
	}
}
