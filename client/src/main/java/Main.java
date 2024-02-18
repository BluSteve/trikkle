import org.trikkle.MachineMain;
import org.trikkle.Serializer;
import org.trikkle.TlvMessage;
import org.trikkle.serial.JarInfo;
import org.trikkle.serial.MachineInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
	public static void main(String[] args) throws IOException {
		// join a cluster and then upload a jar to all the machines in the cluster
		MachineMain machineMain = new MachineMain("localhost", 9999);
		machineMain.register("localhost", 995, "password");
		machineMain.startListening();

		JarInfo jarInfo = new JarInfo("Handlers", Files.readAllBytes(Paths.get("client/build/libs/client.jar")));
		for (MachineInfo machine : machineMain.machines) {
			TlvMessage tlvMessage = new TlvMessage('j', Serializer.serialize(jarInfo));
			machineMain.sendToMachine(machine, tlvMessage);
		}

		Scanner scanner = new Scanner(System.in);
		while (true) {
			String line = scanner.nextLine();
			if (line.equals("exit")) {
				System.exit(0);
			}
			if (line.equals("t")) {
				System.out.println("sending test message");
				for (MachineInfo machine : machineMain.machines) {
					TlvMessage tlvMessage = new TlvMessage('t', "hello world".getBytes());
					machineMain.sendToMachine(machine, tlvMessage);
				}
			}
		}
	}
}
