package org.trikkle;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.*;

public class MachineMain {
	public String managerIp;
	public int managerPort;

	public String password;
	public String ownIp;
	public int ownPort;

	public KeyPair keyPair;

	public MachineMain(String managerIp, int managerPort, String password, String ownIp, int ownPort) {
		this.managerIp = managerIp;
		this.managerPort = managerPort;
		this.password = password;
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

	public static void main(String[] args) {
		MachineMain machineMain = new MachineMain("localhost", 995, "password", "localhost", 999);
		machineMain.start();
	}

	public void start() {
		InitialData initialData = new InitialData();
		initialData.password = password;
		initialData.publicKey = keyPair.getPublic();
		initialData.ip = ownIp;
		initialData.port = ownPort;

		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(managerIp, managerPort));
			char type = 'a';
			byte[] data = Serializer.serialize(initialData);
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeChar(type);
			out.writeInt(data.length);
			out.write(data);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
