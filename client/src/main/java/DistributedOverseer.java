import org.trikkle.*;
import org.trikkle.serial.MachineInfo;
import serial.DatumRequest;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

/*
distributed methods:
0. send graph to all machines cluster (doesn't have to be inside distributedoverseer)

1. get datum
2. update graph with local changes
3. request consensus to execute arc

datatypes needed:
g: broadcast graph state
d: request for a datum
D: reply with datum
c: request permission to run arc
C: reply. content contains yes or no. another possibility is no response. all must respond with yes to run arc.

replies are done in the same socket so there's no need to write a handler. a handler is only for cold requests.
 */
public class DistributedOverseer extends Overseer {
	MachineMain machine;

	public DistributedOverseer(MachineMain machine, Graph graph) {
		this(machine, graph, null);
	}

	public DistributedOverseer(MachineMain machine, Graph graph, Map<String, Object> initialCache) {
		super(graph, initialCache);
		this.machine = machine;

		// add handlers for the datatypes
		machine.handlers.put('g', (byte[] data, Socket socket) -> {
			SerializableGraph sGraph = (SerializableGraph) Serializer.deserialize(data);
			updateLocalGraph(sGraph);
		});
		machine.handlers.put('d', (byte[] data, Socket socket) -> {
			DatumRequest datumRequest = (DatumRequest) Serializer.deserialize(data);
			String datumName = datumRequest.datumName;

			TlvMessage tlvMessage;
			if (cache.containsKey(datumName)) {
				// todo this is not necessarily serializable
				tlvMessage = new TlvMessage('D', Serializer.serialize((Serializable) cache.get(datumName)))
						.encrypted(TlvMessage.getEncryptCipher(datumRequest.publicKey));
			} else {
				tlvMessage = new TlvMessage('0', new byte[0]); // null response
			}

			try {
				tlvMessage.writeTo(socket.getOutputStream());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		machine.handlers.put('c', (byte[] data, Socket socket) -> {
			// todo
		});
	}

	@Override
	protected void ticktock(Node caller) {
		// this is when the local graph state changes.
		// it needs to inform others of the change before doing/ deciding whether to do arcs
		broadcastLocalGraph();

		// afterwards it must request for permission to do an arc
		// so this whole method has to be overriden
	}

	// this is when some other DO sends a graph update. probably need to ticktock yourself
	public void updateLocalGraph(SerializableGraph sGraph) { // todo figure out unsafe arcs later. maybe just run locally
		for (int i = 0; i < sGraph.arcsStatus.length; i++) {
			try {
				g.arcArray[i].setStatus(sGraph.arcsStatus[i]);
			} catch (IllegalArgumentException ignored) {
			}
		}

		for (int i = 0; i < sGraph.nodesProgress.length; i++) {
			g.nodeArray[i].setProgress(sGraph.nodesProgress[i]);
			if (sGraph.nodesUsable[i]) g.nodeArray[i].setUsable();
		}
	}

	public void broadcastLocalGraph() {
		TlvMessage tlvMessage = new TlvMessage('g', Serializer.serialize(new SerializableGraph(g)));
		machine.broadcast(tlvMessage);
	}

	// return datum doesn't need to be overriden
	// get datum needs to be overriden definitely. all requests to get datum should go through that method
	protected Object getDatumProtected(String datumName) {
		// if datum is not in cache, request from others
		if (cache.containsKey(datumName)) {
			return cache.get(datumName);
		} else {
			TlvMessage message =
					new TlvMessage('d', Serializer.serialize(new DatumRequest(datumName, machine.keyPair.getPublic())));

			// try all machines until one responds
			for (MachineInfo machineInfo : machine.machines) {
				try (Socket socket = new Socket()) {
					socket.connect(new InetSocketAddress(machineInfo.ip, machineInfo.port));
					message.encrypted(machine.getCipher(machineInfo)).writeTo(socket.getOutputStream());

					// get response
					TlvMessage response = TlvMessage.readFrom(socket.getInputStream());
					if (response.dataType == 'D') {
						response = response.decrypted(machine.decryptCipher);
						Object datum = Serializer.deserialize(response.data);
						cache.put(datumName, datum);
						return datum;
					} else if (response.dataType == '0') {
						System.out.println("datum not found from " + machineInfo);
					}
				} catch (IOException e) {
					System.err.println("Could not send message to " + machine);
				}
			}
		}

		throw new IllegalStateException("datum not found");
	}
}
