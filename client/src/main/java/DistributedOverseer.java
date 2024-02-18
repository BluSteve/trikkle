import org.trikkle.*;
import org.trikkle.serial.MachineInfo;
import serial.ArcRequest;
import serial.DatumRequest;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

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
	Map<Integer, ArcRequest> pendingRequests = new ConcurrentHashMap<>();
	Set<Integer> arcsRan = ConcurrentHashMap.newKeySet();

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
				tlvMessage = new TlvMessage('1', Serializer.serialize((Serializable) cache.get(datumName)))
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
			// todo arguments in data field to decide which machine gets to run it
			// right now just do it based on who asked first
			// this is only if this machine and the other machine request simultaneously
			// if i didn't even ask to run this arc, then just auto accept

			ArcRequest theirRequest = (ArcRequest) Serializer.deserialize(data);
			TlvMessage tlvMessage;

			if (arcsRan.contains(theirRequest.index)) {
				// i already ran this arc
				tlvMessage = new TlvMessage('0', new byte[0]);
			} else if (!pendingRequests.containsKey(theirRequest.index)) {
				// i'm not invested in running this arc
				tlvMessage = new TlvMessage('1', new byte[0]);
			} else {
				// i'm invested in running this arc
				ArcRequest myRequest = pendingRequests.get(theirRequest.index);
				if (myRequest.argument - theirRequest.argument > 0) {
					// i'm better
					tlvMessage = new TlvMessage('0', new byte[0]);
				} else if (myRequest.argument - theirRequest.argument < 0) {
					// i'm worse
					tlvMessage = new TlvMessage('1', new byte[0]);
					pendingRequests.remove(theirRequest.index);
				} else {
					// we're equal and this will lead to a deadlock
					throw new IllegalStateException("equal arguments");
				}
			}

			try {
				tlvMessage.writeTo(socket.getOutputStream());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	protected void ticktock(Node caller) {
		if (!started) return; // for adding datums manually
		if (hasEnded()) return;
		// this is when the local graph state changes.
		// it needs to inform others of the change before doing/ deciding whether to do arcs
		// afterwards it must request for permission to do an arc
		// so this whole method has to be overriden
		broadcastLocalGraph();

		Collection<Link> linksNow = new ArrayList<>(links.size());
		for (Iterator<Link> iterator = links.iterator(); iterator.hasNext(); ) {
			Link link = iterator.next();
			if (link.getArc().getStatus() == ArcStatus.FINISHED) { // lazily remove finished links
				iterator.remove();
				continue;
			}
			if (link.runnable()) {
				Arc arc = link.getArc();
				if (!unsafeOnRecursive && caller != null && !arc.isSafe()) {
					continue;
				}
				synchronized (arc) { // prevents one arc from being added to two separate linksNow
					if (arc.getStatus() == ArcStatus.IDLE) {
						boolean can = requestToRunArc(arc);
						if (can) {
							arc.setStatus(ArcStatus.STAND_BY);
							linksNow.add(link);
							arcsRan.add(g.arcIndex.get(arc));
							pendingRequests.remove(g.arcIndex.get(arc)); // important that this comes after adding to arcsRan
						}
					}
				}
			}
		}

		if (logging) {
			int t = tick.incrementAndGet();
			linkTrace.add(linksNow);
			if (observer != null) observer.accept(caller, t, linksNow);
		} else {
			if (observer != null) observer.accept(caller, 0, null);
		}

		if (linksNow.isEmpty()) return;
		if (!parallel || linksNow.size() < parallelThreshold) {
			for (Link link : linksNow) {
				link.getArc().runWrapper();
			}
		} else {
			// Run all links that can be done now (aka linksNow) in parallel.
			RecursiveAction[] tasks = new RecursiveAction[linksNow.size()];
			int i = 0;
			for (Link link : linksNow) {
				tasks[i] = new RecursiveAction() {
					@Override
					protected void compute() {
						link.getArc().runWrapper();
					}
				};
				i++;
			}
			ForkJoinTask.invokeAll(tasks);
		}
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

	public boolean requestToRunArc(Arc arc) {
		int index = g.arcIndex.get(arc);
		ArcRequest arcRequest = new ArcRequest(index, new Date().getTime());
		pendingRequests.put(index, arcRequest); // todo figure out when to remove this

		TlvMessage tlvMessage = new TlvMessage('c', Serializer.serialize(arcRequest));

		for (MachineInfo machineInfo : machine.machines) {
			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress(machineInfo.ip, machineInfo.port));
				tlvMessage.encrypted(machine.getCipher(machineInfo)).writeTo(socket.getOutputStream());

				// get response
				TlvMessage response = TlvMessage.readFrom(socket.getInputStream());
				if (response.dataType == '1') {
					System.out.println("arc request accepted by " + machineInfo);
				} else if (response.dataType == '0') {
					System.out.println("arc request denied by " + machineInfo);
					return false;
				}
			} catch (IOException e) {
				System.err.println("Could not send message to " + machine);
			}
		}
		return true;
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
					if (response.dataType == '1') {
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
