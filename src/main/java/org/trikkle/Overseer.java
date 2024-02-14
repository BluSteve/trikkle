package org.trikkle;

import org.trikkle.structs.StrictConcurrentHashMap;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class that manages the execution of {@link Graph}s. It is responsible for running the graph and keeping track of
 * the cache.
 * <p>
 * In Trikkle's architecture, a {@link Node} is only used to represent a dependency relationship. The actual
 * data associated with the node is stored in the overseer's cache. This is to allow for the same graph to be run
 * multiple times with different input data.
 * <p>
 * An overseer object can only be started and therefore run once. If you need to run the same graph again, you must
 * create a new overseer. An overseer may "carry on" the work of a previous overseer by using the cache of the
 * previous overseer as its initial cache, with its graph being an extension of the previous graph.
 * <p>
 * Concurrent execution of overlapping graphs leads to unexpected behavior and is prevented by the overseer. All locks
 * on {@link Primable}s are acquired by the overseer during construction and only released when the overseer ends.
 * <p>
 * A "tick" passes every time the overseer checks for runnable links and runs them. The frequency and timing of going
 * to the next tick, or "ticktocking" was a subject of much deliberation. The current implementation is to ticktock
 * sparsely and economically, only when the state of the <b>nodes</b> change (not the arcs). This allows the overseer
 * to tick only when needed and avoids the overhead of polling. More information on this can be found in the
 * documentation of the {@link #ticktock(Node)} method.
 *
 * @author Steve Cao
 * @see Graph
 * @see Node
 * @see Arc
 * @see Link
 * @see Primable
 * @since 0.1.0
 */
public final class Overseer {
	final Graph g;
	private final Map<String, Object> cache = new StrictConcurrentHashMap<>();
	private final Collection<Link> links = new ConcurrentLinkedQueue<>();
	private AtomicInteger tick;
	private Queue<Collection<Link>> linkTrace;
	private boolean started = false;

	private boolean checkRecursion = true;
	private boolean logging = true;
	private Observer observer = null;
	private boolean parallel = true;
	private int parallelThreshold = 2;

	/**
	 * Constructs an overseer with the given graph. The initial cache is empty.
	 *
	 * @param graph the graph to be executed
	 */
	public Overseer(Graph graph) {
		this(graph, null);
	}

	/**
	 * Constructs an overseer with the given graph and initial cache.
	 *
	 * @param graph        the graph to be executed
	 * @param initialCache the initial cache, possibly from another overseer
	 */
	public Overseer(Graph graph, Map<String, Object> initialCache) {
		this.g = graph;
		if (initialCache != null) {
			this.cache.putAll(initialCache); // doesn't check that the initialCache has datums that are actually in the graph
		}
		links.addAll(g.links);

		// undoes previous overseer's changes
		// Prime nodes and arcs with this overseer
		for (Primable primable : g.primables) {
			primable.getLock().lock();
		}
		for (Primable primable : g.primables) {
			primable.primeWith(this);
		}
	}

	public void resetGraph() {
		for (Primable primable : g.primables) {
			primable.reset();
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
			ticktock(null);
		}
		onEnd();
	}

	private void ticktock(Node caller) {
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
				if (checkRecursion && caller != null && !arc.isSafe()) {
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

	void unsafeTicktock(Node caller) {
		if (caller == null) {
			throw new NullPointerException("Caller cannot be null!");
		}
		ticktock(caller);
	}

	public Set<String> getStartingDatumNames() {
		Set<String> startingDatumNames = new HashSet<>();
		for (Node startingNode : g.startingNodes) {
			startingDatumNames.addAll(startingNode.datumNames);
		}
		return startingDatumNames;
	}

	public Set<String> getUnfilledStartingDatumNames() {
		Set<String> startingDatumNames = new HashSet<>();
		for (Node startingNode : g.startingNodes) {
			for (String datumName : startingNode.datumNames) {
				if (!cache.containsKey(datumName)) {
					startingDatumNames.add(datumName);
				}
			}
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

	public void fillStartingDatums(Map<String, Object> cache) {
		Set<String> unfilled = getUnfilledStartingDatumNames();
		for (Map.Entry<String, Object> entry : cache.entrySet()) {
			if (!unfilled.contains(entry.getKey())) continue;
			addStartingDatum(entry.getKey(), entry.getValue());
		}
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

	public boolean isCheckRecursion() {
		return checkRecursion;
	}

	public void setCheckRecursion(boolean checkRecursion) { // WARNING this is dangerous
		this.checkRecursion = checkRecursion;
	}

	public Observer getObserver() {
		return observer;
	}

	public void setObserver(Observer observer) {
		this.observer = observer;
	}

	@FunctionalInterface
	public interface Observer {
		void accept(Node caller, int tick, Collection<Link> links);
	}
}
