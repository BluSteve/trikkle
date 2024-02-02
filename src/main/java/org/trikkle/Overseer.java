package org.trikkle;

import org.trikkle.structs.IBitmask;
import org.trikkle.structs.MultiHashMap;
import org.trikkle.structs.MultiMap;
import org.trikkle.structs.StrictConcurrentHashMap;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public final class Overseer {
	private final Graph g;
	private final MultiMap<IBitmask, Link> links = new MultiHashMap<>();
	private final Map<String, Object> cache = new StrictConcurrentHashMap<>();
	private final List<Node> nodeOfIndex = new ArrayList<>();
	private final Map<Node, Integer> indexOfNode = new HashMap<>();
	private final Map<String, Node> nodeOfDatumName = new HashMap<>();
	private int tick = 0;
	private boolean started = false;

	public Overseer(Graph graph) {
		this.g = graph;

		// undoes previous Overseer's changes
		for (Node node : g.nodes) {
			node.reset();
		}

		// Prime Nodes and Arcs with this Overseer
		for (Primable primable : g.nodes) {
			primable.primeWith(this);
		}
		for (Primable primable : g.arcs) {
			primable.primeWith(this);
		}


		// Generate helper indices
		int i = 0;
		for (Node node : g.nodes) {
			nodeOfIndex.add(node);
			indexOfNode.put(node, i);
			for (String datumName : node.datumNames) {
				nodeOfDatumName.put(datumName, node);
			}
			i++;
		}


		// Generate bitmasks for each Link
		for (Link link : g.links) {
			IBitmask bitmask = IBitmask.getBitmask(g.nodes.size());
			for (Node dependency : link.getDependencies()) {
				bitmask.set(indexOfNode.get(dependency));
			}

			links.putOne(bitmask, link);
		}
	}

	public Set<String> getStartingDatumNames() {
		Set<String> startingDatumNames = new HashSet<>();
		for (Node startingNode : g.startingNodes) {
			startingDatumNames.addAll(startingNode.datumNames);
		}
		return startingDatumNames;
	}

	public void addStartingDatum(String datumName, Object datum) {
		Node node = nodeOfDatumName.get(datumName);
		if (!g.startingNodes.contains(node)) {
			throw new IllegalArgumentException(
					"Node of datumName \"" + datumName + "\" does not belong to a starting node!");
		}
		node.addDatum(datumName, datum);
	}

	public void start() {
		// check population of startingNodes
		for (Node startingNode : g.startingNodes) {
			if (!startingNode.isUsable()) {
				throw new IllegalStateException("Starting nodes not fully populated; unable to start!");
			}
		}

		started = true;
		ticktock(null);
	}

	void ticktock(Node outputNode) { // passing outputNode is only for debugging purposes
		if (!started) return; // for adding datums manually
		tick++;
		System.out.println("\ntick = " + tick);
		if (hasEnded()) {
			onEnd();
			return;
		}

		if (outputNode != null) {
			System.out.printf("tick = %d, just filled %s%n", tick, outputNode.datumNames);
		} else {
			System.out.printf("tick = %d, outputNode not passed%n", tick);
		}


		// Get all Links with idle Arcs that the current state allows to be executed
		IBitmask state = getCurrentState();
		Set<Link> linksNow = new HashSet<>();
		for (Map.Entry<IBitmask, Set<Link>> linkEntry : links.entrySet()) {
			if (state.compareTo(linkEntry.getKey()) >= 0) { // all where requirements are satisfied
				for (Link link : linkEntry.getValue()) {
					if (link.getArc().status == ArcStatus.IDLE) { // until it finds one that's not finished
						link.getArc().status = ArcStatus.STAND_BY;
						linksNow.add(link);
					}
				}
			}
		}

		System.out.printf("tick = %d, linksNow.size() = %d%n", tick, linksNow.size());


		// Run all links that can be done now (aka linksNow) in parallel.
		Link[] linkArray = linksNow.toArray(new Link[0]);
		List<RecursiveAction> tasks = new ArrayList<>(); // parallel stream doesn't work fsr
		for (int i = 0; i < linkArray.length; i++) {
			int finalI = i;
			tasks.add(new RecursiveAction() {
				@Override
				protected void compute() {
					Link link = linkArray[finalI];
					System.out.printf("Started: tick = %d, link = %s%n", tick, link);
					link.getArc().runWrapper();
				}
			});
		}
		ForkJoinTask.invokeAll(tasks);
	}

	Map<String, Object> getCache() { // just give the full cache in case arc needs to iterate through it.
		return cache;
	}

	public Map<String, Object> getResultCache() {
		Map<String, Object> resultCache = new HashMap<>();
		for (Node endingNode : g.endingNodes) {
			for (String datumName : endingNode.datumNames) {
				Object datum = cache.get(datumName);
				resultCache.put(datumName, datum);
			}
		}
		return resultCache;
	}

	public Map<String, Object> getCacheCopy() {
		return new HashMap<>(cache);
	}

	public Node getOutputNodeOfDatum(String datumName) {
		return nodeOfDatumName.get(datumName);
	}

	Node getOutputNodeOfArc(Arc arc) {
		return g.arcMap.get(arc).getOutputNode();
	}

	private IBitmask getCurrentState() {
		IBitmask state = IBitmask.getBitmask(g.nodes.size());
		for (Node node : g.nodes) {
			if (node.isUsable()) {
				state.set(indexOfNode.get(node));
			}
		}
		return state;
	}

	private boolean hasEnded() {
		for (Node endingNode : g.endingNodes) {
			if (endingNode.getProgress() != 1) {
				return false;
			}
		}
		return true;
	}

	private void onEnd() {
		System.out.println("Overseer finished!");
	}

	public Graph getGraph() {
		return g;
	}
}
