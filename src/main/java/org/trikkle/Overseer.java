package org.trikkle;

import org.trikkle.structs.StrictConcurrentHashMap;
import org.trikkle.structs.StrictHashMap;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class that manages the execution of {@link Graph}s. It is responsible for running the graph and keeping track of
 * the cache which stores all data in key-value pairs. <b>An overseer ends when all of its ending nodes have progress
 * 1.</b> This may mean not all arcs are finished and not all nodes are at progress 1.
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
 * <li>the state of the <b>nodes</b> change (not the arcs) - aka <b>recursive ticktock</b></li>
 * <li>no change was detected but the overseer has not finished - aka <b>iterative ticktock.</b></li>
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
	// number of dependent nodes undone for each datum. Using Map<String,Integer> wouldn't work because of double
	// counting of nodes if more than one link points to them.
	final Map<String, Set<Node>> depNodesOfDatum = new HashMap<>();
	private final Map<String, Object> cache = new StrictConcurrentHashMap<>();
	private final Collection<Link> linkQueue = new ConcurrentLinkedQueue<>();
	private AtomicInteger tick;
	private Queue<Collection<Link>> linkTrace;
	private boolean started = false;
	private boolean unsafeOnRecursive = false;
	private boolean logging = false;
	private Observer observer = null;
	private boolean parallel = true;
	private int parallelThreshold = 2;
	private boolean garbageCollect = true;
	private final Set<String> gcBlacklist = new HashSet<>();
	private Map<String, Object> resultCache;

	/**
	 * Constructs an overseer with the given graph. All {@link Primable}s will be locked and primed
	 * with this overseer.
	 *
	 * @param graph the graph to be executed
	 */
	public Overseer(Graph graph) {
		this.g = graph;
		linkQueue.addAll(g.links);

		for (Link link : g.links) {
			for (Node inputNode : link.getInputNodes()) {
				for (String datumName : inputNode.datumNames) {
					for (Node outputNode : link.getOutputNodes()) {
						// only input datums and not ending datums are keys in this map
						if (depNodesOfDatum.containsKey(datumName)) {
							depNodesOfDatum.get(datumName).add(outputNode);
						} else {
							Set<Node> nodes = ConcurrentHashMap.newKeySet(); // very impt! todo also could be race condition cause
							nodes.add(outputNode);
							depNodesOfDatum.put(datumName, nodes);
						}
					}
				}
			}
		}

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
		tick = new AtomicInteger(0);
		if (logging) {
			linkTrace = new ConcurrentLinkedQueue<>();
		}
		while (!hasEnded()) {
			ticktock(null);
		}
		onEnd();
	}

	void markDone(Node node) {
		if (!garbageCollect) return;

		Set<Link> links = g.outputNodeMap.get(node);
		if (links == null) return;
		for (Link link : links) {
			for (Node inputNode : link.getInputNodes()) {
				for (String datumName : inputNode.datumNames) {
					depNodesOfDatum.get(datumName).remove(node);
					if (depNodesOfDatum.get(datumName).isEmpty() && !gcBlacklist.contains(datumName)) {
						cache.remove(datumName); // garbage collect intermediate values
					}
				}
			}
		}
	}

	private void ticktock(Node caller) {
		if (!started) return; // to prevent adding datums manually from triggering a ticktock
		if (hasEnded()) return;

		// all outputs nodes having progress 1 is equivalent to the arc being done.
		if (caller != null && caller.getProgress() == 1) {
			for (Link link : g.outputNodeMap.get(caller)) {
				// maybe another one of its output nodes got to it first
				if (link.getArc().getStatus() == ArcStatus.FINISHED) continue;
				if (link.getArc().getOutputNodesRemaining() == 0) {
					link.getArc().setStatus(ArcStatus.FINISHED);
				}
			}
		}

		Collection<Link> linksNow = new ArrayList<>(linkQueue.size());
		for (Iterator<Link> iterator = linkQueue.iterator(); iterator.hasNext(); ) {
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

		int t = tick.incrementAndGet();
		if (logging) {
			linkTrace.add(linksNow);
			if (observer != null) observer.accept(caller, t, linksNow);
		} else {
			if (observer != null) observer.accept(caller, 0, linksNow);
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
	 * @return the names of datums that are required to start the graph, belonging to the starting nodes
	 */
	public Set<String> getStartingDatumNames() {
		Set<String> startingDatumNames = new HashSet<>();
		for (Node startingNode : g.startingNodes) {
			startingDatumNames.addAll(startingNode.datumNames);
		}
		return startingDatumNames;
	}

	/**
	 * @return the names of datums that are required to start the graph but which have not been filled
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
		return true;
	}

	private void onEnd() {
		for (Primable primable : g.primables) {
			primable.getLock().unlock();
		}
	}

	/**
	 * Returns the full cache which is a {@link StrictHashMap}. Trying to get a datum that is not in the cache will
	 * throw an exception.
	 *
	 * @return the full cache
	 */
	public Map<String, Object> getCache() { // just give the full cache in case arc needs to iterate through it.
		return cache;
	}

	/**
	 * The result cache contains only the datums of ending nodes. Trying to get a datum that is not in the cache will
	 * return null.
	 *
	 * @return the result cache
	 */
	public Map<String, Object> getResultCache() {
		if (resultCache != null) return resultCache;

		resultCache = new HashMap<>();
		for (Node endingNode : g.endingNodes) {
			for (String datumName : endingNode.datumNames) {
				Object datum = cache.get(datumName);
				resultCache.put(datumName, datum);
			}
		}
		return resultCache;
	}

	/**
	 * Returns the full cache as a copy. Trying to get a datum that is not in the cache will return null.
	 *
	 * @return a copy of the full cache
	 */
	public Map<String, Object> getCacheCopy() {
		return new HashMap<>(cache);
	}

	/**
	 * Returns the datum with the given name from the cache. If the datum is not in the cache, this will return null.
	 *
	 * @param datumName the name of the datum
	 * @return the datum with the given name
	 */
	public Object getDatum(String datumName) {
		return cache.get(datumName);
	}

	/**
	 * Returns the node that contains the given datum. If the datum is not in the graph, this will return null.
	 *
	 * @param datumName the name of the datum
	 * @return the node that contains the given datum
	 */
	public Node getNodeOfDatum(String datumName) {
		return g.nodeOfDatum.get(datumName);
	}

	/**
	 * Returns the current tick number.
	 *
	 * @return the current tick number
	 */
	public int getTick() {
		return tick.get();
	}

	/**
	 * Returns the link trace. If logging is disabled, this will always return null.
	 *
	 * @return the link trace
	 */
	public Queue<Collection<Link>> getLinkTrace() {
		return linkTrace;
	}

	/**
	 * Returns the time taken to run each link in nanoseconds. If the link has not finished running, the burst time
	 * will be -1.
	 *
	 * @param timeUnit the time unit to return the burst times in.
	 * @return the time taken to run each link in nanoseconds
	 * @throws NullPointerException if the time unit is null
	 */
	public Map<Link, Long> getBurstTimes(TimeUnit timeUnit) {
		if (timeUnit == null) {
			throw new NullPointerException("Time unit cannot be null!");
		}

		Map<Link, Long> burstTimes = new HashMap<>();
		for (Link link : g.links) {
			long startTime = link.getArc().getStartTime();
			long endTime = link.getArc().getEndTime();
			if (startTime == -1 || endTime == -1) {
				burstTimes.put(link, -1L);
			} else {
				burstTimes.put(link, timeUnit.convert(endTime - startTime, TimeUnit.NANOSECONDS));
			}
		}
		return burstTimes;
	}

	/**
	 * @return the graph that this overseer is running
	 */
	public Graph getGraph() {
		return g;
	}

	public boolean isParallel() {
		return parallel;
	}

	/**
	 * Default: {@code true}
	 *
	 * @param parallel whether to run runnable links in parallel
	 */
	public void setParallel(boolean parallel) {
		this.parallel = parallel;
	}

	public int getParallelThreshold() {
		return parallelThreshold;
	}

	/**
	 * Default: {@code 2}
	 *
	 * @param parallelThreshold the minimum number of links that must be runnable for the overseer to run them in
	 *                          parallel
	 */
	public void setParallelThreshold(int parallelThreshold) {
		if (parallelThreshold < 2) {
			throw new IllegalArgumentException("Parallel threshold must be at least 1!");
		}
		this.parallelThreshold = parallelThreshold;
	}

	public boolean isLogging() {
		return logging;
	}

	/**
	 * Default: {@code false}
	 *
	 * @param logging whether to log the overseer's progress in ticks and link trace
	 * @see #getTick()
	 * @see #getLinkTrace()
	 */
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

	public boolean isGarbageCollect() {
		return garbageCollect;
	}

	/**
	 * Default: {@code true}
	 *
	 * @param garbageCollect whether to garbage collect intermediate values
	 */
	public void setGarbageCollect(boolean garbageCollect) {
		this.garbageCollect = garbageCollect;
	}

	/**
	 * Get the garbage collection blacklist. A datum in the blacklist will not be garbage collected even if it is no longer needed.
	 *
	 * @return the garbage collection blacklist
	 */
	public Set<String> getGcBlacklist() {
		return gcBlacklist;
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
