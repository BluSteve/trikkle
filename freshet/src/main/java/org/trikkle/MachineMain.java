package org.trikkle;

import org.trikkle.cluster.MachineInfo;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MachineMain {
	public String ownIp;
	public int ownPort;
	public KeyPair keyPair;
	public Cipher encryptCipher, decryptCipher;
	public List<MachineInfo> machines = new ArrayList<>();
	public Map<MachineInfo, Cipher> machineCiphers = new HashMap<>();
	public MachineInfo myself;
	public BlockingQueue<Boolean> listening = new ArrayBlockingQueue<>(1);

	public MachineMain(String ownIp, int ownPort) {
		this.ownIp = ownIp; // this is the ip of the machine to be used for communication with other machines
		this.ownPort = ownPort;

		KeyPairGenerator generator;
		try {
			generator = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		generator.initialize(2048);

		keyPair = generator.generateKeyPair();

		// encrypt and decrypt ciphers
		try {
			encryptCipher = Cipher.getInstance("RSA");
			encryptCipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
			decryptCipher = Cipher.getInstance("RSA");
			decryptCipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}

	public Cipher getCipher(MachineInfo machine) {
		if (machineCiphers.containsKey(machine)) {
			return machineCiphers.get(machine);
		} else {
			try {
				Cipher cipher = Cipher.getInstance("RSA");
				cipher.init(Cipher.ENCRYPT_MODE, machine.publicKey);
				machineCiphers.put(machine, cipher);
				return cipher;
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void sendToMachine(MachineInfo machine, TlvMessage message) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(machine.ip, machine.port));

			message.encrypted(getCipher(machine)).writeTo(socket.getOutputStream());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void register(String managerIp, int managerPort, String password) {
		InitialData initialData = new InitialData();
		initialData.password = password;
		initialData.publicKey = keyPair.getPublic();
		initialData.ip = ownIp;
		initialData.port = ownPort;

		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(managerIp, managerPort));
			char type = 'a';
			byte[] data = Serializer.serialize(initialData);
			TlvMessage message = new TlvMessage(type, data);
			message.writeTo(socket.getOutputStream());

			// add other machines to machines list
			TlvMessage response = TlvMessage.readFrom(socket.getInputStream());
			MachineInfo[] machineArray = (MachineInfo[]) Serializer.deserialize(response.data);
			for (MachineInfo machineInfo : machineArray) {
				if (machineInfo.publicKey.equals(keyPair.getPublic())) {
					myself = machineInfo;
				} else {
					machines.add(machineInfo);
				}
			}

			System.out.println("machines = " + machines);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void startListening() {
		new Thread(() -> {
			try (ServerSocket serverSocket = new ServerSocket(ownPort)) {
				listening.add(true);

				// check for acknowledgement from all machines
				Set<MachineInfo> ackSet = new HashSet<>();
				while (ackSet.size() < machines.size()) {
					Socket socket = serverSocket.accept();
					TlvMessage message = TlvMessage.readFrom(socket.getInputStream()).decrypted(decryptCipher);
					if (message.dataType != 'N') {
						throw new RuntimeException("Expected 'N' but got " + message.dataType);
					}
					MachineInfo machine = (MachineInfo) Serializer.deserialize(message.data);
					// if machine is not in machines then something is very wrong.
					ackSet.add(machine);
				}

				System.out.println(ownPort + " All machines acknowledged. Starting to listen for messages.");

				while (!Thread.currentThread().isInterrupted()) {
					Socket socket = serverSocket.accept();
					TlvMessage message = TlvMessage.readFrom(socket.getInputStream()).decrypted(decryptCipher);

					switch (message.dataType) {
						case 'n': // new machine added
							MachineInfo machine = (MachineInfo) Serializer.deserialize(message.data);
							machines.add(machine);
							System.out.println(ownPort + " updated machines = " + machines);
							sendToMachine(machine, new TlvMessage('N', Serializer.serialize(myself)));
							break;
						default:
							throw new RuntimeException("Unknown message type: " + message.dataType);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).start();

		try {
			listening.take();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		for (MachineInfo machine : machines) {
			sendToMachine(machine, new TlvMessage('n', Serializer.serialize(myself)));
		}
	}
}
