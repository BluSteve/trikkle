package org.trikkle;

import org.trikkle.cluster.MachineInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class MachineMain {
	public String ownIp;
	public int ownPort;
	public KeyPair keyPair;
	public List<MachineInfo> machines;
	public MachineInfo myself;

	public MachineMain(String ownIp, int ownPort) {
		this.ownIp = ownIp;
		this.ownPort = ownPort;

		KeyPairGenerator generator;
		try {
			generator = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		generator.initialize(2048);

		keyPair = generator.generateKeyPair();
	}

	public void sendToMachine(MachineInfo machine, TlvMessage message) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(machine.ip, machine.port));
			message.writeTo(socket.getOutputStream());
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
		try (ServerSocket serverSocket = new ServerSocket(ownPort)) {
			while (true) {
				Socket socket = serverSocket.accept();
				// new machine connected
				TlvMessage message = TlvMessage.readFrom(socket.getInputStream());
				System.out.println("message = " + message);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
