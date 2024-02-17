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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MachineMain {
	public int ownPort;
	public KeyPair keyPair;
	public Cipher encryptCipher, decryptCipher;
	public List<MachineInfo> machines = new ArrayList<>();
	public Map<MachineInfo, Cipher> machineCiphers = new HashMap<>();
	public MachineInfo myself;

	public MachineMain(int ownPort) {
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
		initialData.ip = "";
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
		try (ServerSocket serverSocket = new ServerSocket(ownPort)) {
			while (true) {
				Socket socket = serverSocket.accept();
				// new machine connected
				TlvMessage message = TlvMessage.readFrom(socket.getInputStream()).decrypted(decryptCipher);

				System.out.println("message = " + message);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
