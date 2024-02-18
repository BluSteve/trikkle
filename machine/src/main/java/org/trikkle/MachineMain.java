package org.trikkle;

import org.trikkle.serial.InitialData;
import org.trikkle.serial.JarInfo;
import org.trikkle.serial.MachineInfo;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

public class MachineMain {
	public String ownIp;
	public int ownPort;
	public KeyPair keyPair;
	public Cipher encryptCipher, decryptCipher;
	public Set<MachineInfo> machines = ConcurrentHashMap.newKeySet();
	public Map<MachineInfo, Cipher> machineCiphers = new ConcurrentHashMap<>();
	public MachineInfo myself;
	public Map<Character, Consumer<byte[]>> handlers = new HashMap<>();
	public ServerSocket serverSocket;
	public Semaphore listening = new Semaphore(0);

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

	public static void main(String[] args) {
		MachineMain machineMain = new MachineMain("localhost", 999);
		machineMain.register("localhost", 995, "password");
		machineMain.startListening();
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
			System.err.println("Could not send message to " + machine);
		}
	}

	public void broadcast(TlvMessage message) {
		// todo maybe copy machines to avoid concurrent modification exception
		for (MachineInfo machine : machines) {
			sendToMachine(machine, message);
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
			TlvMessage response = TlvMessage.readFrom(socket.getInputStream()).decrypted(decryptCipher);
			MachineInfo[] machineArray = (MachineInfo[]) Serializer.deserialize(response.data);
			for (MachineInfo machineInfo : machineArray) {
				if (machineInfo.publicKey.equals(keyPair.getPublic())) {
					myself = machineInfo;
				} else {
					machines.add(machineInfo);
				}
			}

			System.out.println(ownPort + " initial machines = " + machines);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void startListening() {
		try {
			serverSocket = new ServerSocket(ownPort);
			listening.release();

			while (!Thread.currentThread().isInterrupted()) {
				Socket socket = serverSocket.accept();
				TlvMessage message = TlvMessage.readFrom(socket.getInputStream()).decrypted(decryptCipher);

				switch (message.dataType) {
					// sent by manager
					case 'n': // new machine added
						MachineInfo machine = (MachineInfo) Serializer.deserialize(message.data);
						machines.add(machine);
						System.out.println(ownPort + " updated machines = " + machines);
						break;
					case 'r': // machine removed
						MachineInfo removedMachine = (MachineInfo) Serializer.deserialize(message.data);
						machines.remove(removedMachine);
						System.out.println(ownPort + " updated machines = " + machines);
						break;
					case 'p': // alive check
						break;

					// sent by other machines
					case 'j': // received jar
						System.out.println("Received jar.");
						JarInfo jarInfo = (JarInfo) Serializer.deserialize(message.data);

						String jarName = ".cache.jar";
						FileOutputStream fos = new FileOutputStream(jarName);
						fos.write(jarInfo.jar);
						fos.close();

						URL url = Paths.get(jarName).toUri().toURL();
						URLClassLoader child = new URLClassLoader(new URL[]{url}, MachineMain.class.getClassLoader());
						Class<?> classToLoad;
						try {
							classToLoad = Class.forName(jarInfo.className, false, child);
						} catch (ClassNotFoundException e) {
							throw new RuntimeException(e);
						}

						Method[] methods = classToLoad.getMethods();
						for (Method method : methods) {
							if (!method.isAnnotationPresent(Handler.class)) continue;

							Handler handler = method.getAnnotation(Handler.class);

							// adapt method to consumer
							Consumer<byte[]> consumer = (byte[] data) -> {
								try {
									method.invoke(null, (Object) data);
								} catch (IllegalAccessException | InvocationTargetException e) {
									throw new RuntimeException(e);
								}
							};
							handlers.put(handler.dataType(), consumer);
						}
						break;

					default:
						if (handlers.containsKey(message.dataType)) {
							handlers.get(message.dataType).accept(message.data);
						} else {
							System.err.println("Unknown message type: " + message.dataType);
						}
				}
			}
		} catch (BindException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			System.err.println(ownPort + " stopped listening");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
