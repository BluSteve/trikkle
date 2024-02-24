package org.trikkle;

import org.trikkle.structs.StrictConcurrentHashMap;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class that manages the execution of {@link Graph}s. It is responsible for running the graph and keeping track of
 * the cache which stores all data in key-value pairs.
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
 * sparsely and economically, only when
 * <ol>
 * <li>the state of the <b>nodes</b> change (not the arcs)</li>
 * <li>no change was detected but the overseer has not finished.</li>
 * </ol>
 * <p>
 * This allows the overseer to tick only when needed and avoids the overhead of polling.
 *
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

	private boolean unsafeOnRecursive = false;
	private boolean logging = true;
	private Observer observer = null;
	private boolean parallel = true;
	private int parallelThreshold = 2;

	/**
	 * Constructs an overseer with the given graph. The initial cache is empty. All {@link Primable}s will be locked and
	 * primed with this overseer.
	 *
	 * @param graph the graph to be executed
	 */
	public Overseer(Graph graph) {
		this(graph, null);
	}

	/**
	 * Constructs an overseer with the given graph and initial cache. All {@link Primable}s will be locked and primed
	 * with this overseer.
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

	/**
	 * Resets the graph (nodes and arcs) to its initial state. This method is useful for running the same graph multiple
	 * times with different input data. The cache is not affected.
	 */
	public void resetGraph() {
		for (Primable primable : g.primables) {
			primable.reset();
			primable.primeWith(this);
		}
	}

	/**
	 * Starts the overseer by running some checks and then ticking until the graph has all ending nodes at progress 1
	 * ({@link Node#getProgress()}) and all arcs are {@link ArcStatus#FINISHED}. Blocks the current thread until the
	 * graph has ended.
	 *
	 * @throws IllegalStateException if the overseer has already started
	 * @throws IllegalStateException if the starting nodes are not fully populated
	 * @throws IllegalStateException if the overseer construction and start() are called in different threads
	 */
	public void start() {
		if (started) {
			throw new IllegalStateException("Overseer started before!");
		}
		// check population of startingNodes
		for (Node startingNode : g.startingNodes) {
			if (!startingNode.isUsable()) {
				throw new IllegalStateException(
						"Starting node " + startingNode + " is not fully populated! All unfilled datums: " +
								getUnfilledStartingDatumNames());
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

	/**
	 * Returns the names of datums that are required to start the graph, belonging to the starting nodes.
	 *
	 * @return the names of datums that are required to start the graph
	 */
	public Set<String> getStartingDatumNames() {
		Set<String> startingDatumNames = new HashSet<>();
		for (Node startingNode : g.startingNodes) {
			startingDatumNames.addAll(startingNode.datumNames);
		}
		return startingDatumNames;
	}

	/**
	 * Returns the names of datums that are required to start the graph, belonging to the starting nodes, but which have
	 * not been filled.
	 *
	 * @return the names of datums that are required to start the graph but have not been filled
	 */
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

	/**
	 * Convenience method for adding a starting datum to the overseer's cache. The datum must belong to a starting node.
	 * Equivalent to calling {@link Node#addDatum(String, Object)} on the starting node directly.
	 *
	 * @param datumName the name of the datum
	 * @param datum     the datum
	 */
	public void addStartingDatum(String datumName, Object datum) {
		Node node = g.nodeOfDatum.get(datumName);
		if (!g.startingNodes.contains(node)) {
			throw new IllegalArgumentException(
					"Datum " + datumName + " does not belong to a starting node!");
		}
		node.addDatum(datumName, datum);
	}

	/**
	 * Fills the remainder of the starting datums with the given cache. If a starting datum has already been filled
	 * manually with {@link #addStartingDatum(String, Object)}, this method will not override it.
	 *
	 * @param cache the cache to fill the starting datums with
	 */
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

	public boolean isUnsafeOnRecursive() {
		return unsafeOnRecursive;
	}

	/**
	 * Default: {@code false}
	 * <p>
	 * WARNING: setting this to false can lead to infinite loops easily. Only set this to false if you are absolutely
	 * sure that your unsafe arcs are well-behaved.
	 * <p>
	 * By default, unsafe arcs can only be activated via an iterative ticktock from the overseer itself, leading to
	 * marginally decreased performance. Recursive ticktocks are completely safe on safe arcs.
	 *
	 * @param unsafeOnRecursive whether to run unsafe arcs on a recursive ticktock call
	 * @see Arc#isSafe()
	 */
	public void setUnsafeOnRecursive(boolean unsafeOnRecursive) { // WARNING this is dangerous
		this.unsafeOnRecursive = unsafeOnRecursive;
	}

	/**
	 * Returns the observer for this overseer.
	 *
	 * @return the observer for this overseer
	 */
	public Observer getObserver() {
		return observer;
	}

	/**
	 * Sets the observer for this overseer. The observer is called every time the overseer ticks. The observer is called
	 * with the following parameters:
	 * <ol>
	 *   <li>the node that called the tick</li>
	 *   <li>the current tick number</li>
	 *   <li>the links that are runnable at the current tick</li>
	 *   </ol>
	 *
	 * @param observer custom observer
	 */
	public void setObserver(Observer observer) {
		this.observer = observer;
	}

	/**
	 * A functional interface for observing the overseer's progress. The observer is called every time the overseer
	 * ticks. If logging ({@link #setLogging(boolean)}) is set to true, the observer is called with the following
	 * parameters:
	 * <ol>
	 * <li>the node that called the tick</li>
	 * <li>the current tick number</li>
	 * <li>the links that are runnable at the current tick</li>
	 * </ol>
	 * Otherwise, it is called with {@code (node, 0, null)}.
	 */
	@FunctionalInterface
	public interface Observer {
		void accept(Node caller, int tick, Collection<Link> links);
	}
}
