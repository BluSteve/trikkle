package org.trikkle;

import org.trikkle.structs.IBitmask;
import org.trikkle.structs.MultiHashMap;
import org.trikkle.structs.MultiMap;
import org.trikkle.structs.StrictConcurrentHashMap;

import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

public final class Overseer {
	private static final int PARALLEL_THRESHOLD = 2;
	private final Graph g;
	private final MultiMap<IBitmask, Link> linkMap = new MultiHashMap<>();
	private final Map<String, Object> cache = new StrictConcurrentHashMap<>();
	private final Map<Node, Integer> indexOfNode = new HashMap<>();
	private final AtomicInteger tick = new AtomicInteger(0);
	private final Stack<RecursiveAction> tasks = new Stack<>();
	private boolean started = false;

	public Overseer(Graph graph) {
		this.g = graph;

		// undoes previous overseer's changes
		// Prime nodes and arcs with this overseer
		for (Primable primable : g.primables) {
			primable.getLock().lock();
			primable.reset();
			primable.primeWith(this);
		}

		// Generate helper indices
		int i = 0;
		for (Node node : g.nodes) {
			indexOfNode.put(node, i);
			i++;
		}

		// Generate bitmasks for each link
		for (Link link : g.links) {
			IBitmask bitmask = IBitmask.getBitmask(g.nodes.size());
			for (Node dependency : link.getDependencies()) {
				bitmask.set(indexOfNode.get(dependency));
			}

			linkMap.putOne(bitmask, link);
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
		Node node = g.nodeOfDatum.get(datumName);
		if (!g.startingNodes.contains(node)) {
			throw new IllegalArgumentException(
					"Datum " + datumName + " does not belong to a starting node!");
		}
		node.addDatum(datumName, datum);
	}

	public void start() {
		if (started) {
			throw new IllegalStateException("Overseer started before!");
		}

		// check population of startingNodes
		for (Node startingNode : g.startingNodes) {
			if (!startingNode.isUsable()) {
				throw new IllegalStateException("Starting nodes not fully populated; unable to start!");
			}
		}

		started = true;
		while (!hasEnded()) {
			ticktock();
		}
		while (!tasks.isEmpty()) {
			tasks.pop().join();
		}
		System.out.println("Overseer ended in " + tick.get() + " ticks.");
		onEnd();
	}

	private void ticktock() {
		if (!started) return; // for adding datums manually
		if (hasEnded()) return;
		tick.incrementAndGet();

		IBitmask state = getCurrentState();
		Collection<Arc> arcsNow = new ArrayList<>(g.arcs.size());
		for (Map.Entry<IBitmask, Set<Link>> linkEntry : linkMap.entrySet()) {
			if (state.supersetOf(linkEntry.getKey())) { // all where requirements are satisfied
				for (Link link : linkEntry.getValue()) {
					Arc arc = link.getArc();
					synchronized (arc) { // prevents one arc from being added to two separate arcsNow
						if (arc.status == ArcStatus.IDLE) { // until it finds one that's not finished
							arc.status = ArcStatus.STAND_BY;
							arcsNow.add(arc);
						}
					}
				}
			}
		}

		if (arcsNow.size() < PARALLEL_THRESHOLD) {
			for (Arc arc : arcsNow) {
				arc.runWrapper();
			}
		} else {
			// Run all links that can be done now (aka arcsNow) in parallel.
			for (Arc arc : arcsNow) {
				RecursiveAction task = new RecursiveAction() {
					@Override
					protected void compute() {
						arc.runWrapper();
					}
				};
				tasks.push(task);
				task.fork();
			}
		}

		pruneLinks();
	}

	private void pruneLinks() {
		for (Set<Link> value : linkMap.values()) {
			value.removeIf(link -> link.getArc().getStatus() == ArcStatus.FINISHED);
		}
	}

	Map<String, Object> getCache() { // just give the full cache in case arc needs to iterate through it.
		return cache;
	}

	public Object getDatum(String datumName) {
		return cache.get(datumName);
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

	public Node getNodeOfDatum(String datumName) {
		return g.nodeOfDatum.get(datumName);
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
		for (Arc arc : g.arcs) {
			if (arc.getStatus() != ArcStatus.FINISHED) {
				return false;
			}
		}
		return true;
	}

	private void onEnd() {
		for (Primable primable : g.primables) {
			primable.getLock().unlock();
		}
	}

	public Graph getGraph() {
		return g;
	}
}
