package org.trikkle.cluster;

import org.trikkle.Serializer;
import org.trikkle.TlvMessage;
import org.trikkle.serial.InitialData;
import org.trikkle.serial.MachineInfo;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
	public Collection<MachineInfo> machines = new ConcurrentLinkedQueue<>();
	public Map<MachineInfo, Cipher> machineCiphers = new HashMap<>();

	public ClusterManager(int port, String password) {
		this.port = port;
		this.password = password;
	}

	public static void main(String[] args) {
		ClusterManager clusterManager = new ClusterManager(995, "password");
		clusterManager.start();
	}

	private static Cipher getEncryptCipher(PublicKey publicKey) {
		Cipher encryptCipher;
		try {
			encryptCipher = Cipher.getInstance("RSA");
			encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
			throw new RuntimeException(e);
		}
		return encryptCipher;
	}

	public void start() {
		new Thread(() -> {
			// poll machines
			while (!Thread.currentThread().isInterrupted()) {
				for (MachineInfo machine : machines) {
					// if machine is not reachable, remove it from the list
					try (Socket socket = new Socket(machine.ip, machine.port)) {
						TlvMessage message = new TlvMessage('p', new byte[0]).encrypted(machineCiphers.get(machine));
						message.writeTo(socket.getOutputStream());
					} catch (IOException e) {
						System.out.println("Machine " + machine + " is not reachable. Removing from list.");
						machines.remove(machine);
						machineCiphers.remove(machine);
						System.out.println("machines = " + machines);
						try {
							announceRemove(machine);
						} catch (IOException ioException) {
							ioException.printStackTrace();
						}
					}
				}
				Utils.sleep(10000); // poll every ten seconds
			}
		}).start();

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("ClusterManager started on port " + port + " with password \"" + password + "\"");
			while (!Thread.currentThread().isInterrupted()) {
				Socket socket = serverSocket.accept();
				// new machine connected
				TlvMessage message = TlvMessage.readFrom(socket.getInputStream());

				if (message.dataType == 'a') {
					InitialData initialData = (InitialData) Serializer.deserialize(message.data);

					if (initialData.password.equals(password)) {
						String ip = initialData.ip.isEmpty() ? socket.getInetAddress().getHostAddress() : initialData.ip;
						MachineInfo machine = new MachineInfo(initialData.publicKey, ip, initialData.port);
						System.out.println("New machine " + machine + " connected!");
						machines.add(machine);
						machineCiphers.put(machine, getEncryptCipher(initialData.publicKey));

						// replies with current list of machines
						MachineInfo[] machinesArray = machines.toArray(new MachineInfo[0]);
						TlvMessage response =
								new TlvMessage('u', Serializer.serialize(machinesArray)).encrypted(machineCiphers.get(machine));
						response.writeTo(socket.getOutputStream());

						// tells other machines about the new machine
						announceNew(machine);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void announceNew(MachineInfo machine) throws IOException {
		for (MachineInfo otherMachine : machines) {
			if (!otherMachine.equals(machine)) {
				try (Socket otherSocket = new Socket(otherMachine.ip, otherMachine.port)) {
					TlvMessage updateMessage =
							new TlvMessage('n', Serializer.serialize(machine)).encrypted(machineCiphers.get(otherMachine));
					updateMessage.writeTo(otherSocket.getOutputStream());
				}
			}
		}
	}

	private void announceRemove(MachineInfo machine) throws IOException {
		for (MachineInfo otherMachine : machines) {
			if (!otherMachine.equals(machine)) {
				try (Socket otherSocket = new Socket(otherMachine.ip, otherMachine.port)) {
					TlvMessage updateMessage =
							new TlvMessage('r', Serializer.serialize(machine)).encrypted(machineCiphers.get(otherMachine));
					updateMessage.writeTo(otherSocket.getOutputStream());
				}
			}
		}
	}
}
