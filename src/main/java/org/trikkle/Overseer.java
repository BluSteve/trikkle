package org.trikkle;

import org.trikkle.structs.StrictConcurrentHashMap;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

public final class Overseer {
	final Graph g;
	private final Map<String, Object> cache = new StrictConcurrentHashMap<>();
	private final Collection<Link> links = new ConcurrentLinkedQueue<>();
	private AtomicInteger tick;
	private Queue<Collection<Link>> linkTrace;
	private boolean started = false;
	private boolean parallel = true;
	private boolean logging = true;
	private int parallelThreshold = 2;

	public Overseer(Graph graph) {
		this(graph, null);
	}

	public Overseer(Graph graph, Map<String, Object> cache) {
		this.g = graph;
		if (cache != null) {
			this.cache.putAll(cache); // doesn't check that the cache has datums that are actually in the graph
		}
		links.addAll(g.links);

		// undoes previous overseer's changes
		// Prime nodes and arcs with this overseer
		for (Primable primable : g.primables) {
			primable.getLock().lock();
		}
		for (Primable primable : g.primables) {
			if (cache == null) primable.reset();
			primable.primeWith(this);
		}
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
		// check that overseer and .start() are called in the same thread
		for (Primable primable : g.primables) {
			if (!primable.getLock().isHeldByCurrentThread()) {
				throw new IllegalStateException(
						"Overseer construction and start() must be called in the same thread!");
			}
		}

		started = true;
		if (logging) {
			tick = new AtomicInteger(0);
			linkTrace = new ConcurrentLinkedQueue<>();
		}
		while (!hasEnded()) {
			ticktock(false);
		}
		onEnd();
	}

	private void ticktock(boolean recursive) {
		if (!started) return; // for adding datums manually
		if (hasEnded()) return;

		Collection<Link> linksNow = new ArrayList<>(links.size());
		for (Iterator<Link> iterator = links.iterator(); iterator.hasNext(); ) {
			Link link = iterator.next();
			if (link.getArc().getStatus() == ArcStatus.FINISHED) {
				iterator.remove();
				continue;
			}
			if (link.runnable()) {
				Arc arc = link.getArc();
				if (recursive && !arc.isSafe()) {
					continue;
				}
				synchronized (arc) { // prevents one arc from being added to two separate linksNow
					if (arc.getStatus() == ArcStatus.IDLE) { // until it finds one that's not finished
						arc.setStatus(ArcStatus.STAND_BY);
						linksNow.add(link);
					}
				}
			}
		}

		if (logging) {
			tick.incrementAndGet();
			linkTrace.add(linksNow);
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

	void unsafeTicktock() {
		ticktock(true);
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
		if (tick.get() != linkTrace.size()) {
			throw new IllegalStateException("Tick and linkTrace are out of sync!");
		}
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

	public Object getDatum(String datumName) {
		return cache.get(datumName);
	}

	public Node getNodeOfDatum(String datumName) {
		return g.nodeOfDatum.get(datumName);
	}

	public int getTick() {
		return tick.get();
	}

	public Queue<Collection<Link>> getLinkTrace() {
		return linkTrace;
	}

	public Graph getGraph() {
		return g;
	}

	public boolean isParallel() {
		return parallel;
	}

	public void setParallel(boolean parallel) {
		this.parallel = parallel;
	}

	public int getParallelThreshold() {
		return parallelThreshold;
	}

	public void setParallelThreshold(int parallelThreshold) {
		if (parallelThreshold < 2) {
			throw new IllegalArgumentException("Parallel threshold must be at least 1!");
		}
		this.parallelThreshold = parallelThreshold;
	}

	public boolean isLogging() {
		return logging;
	}

	public void setLogging(boolean logging) {
		this.logging = logging;
	}
}
