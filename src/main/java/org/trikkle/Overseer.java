package org.trikkle;

import java.util.*;
import java.util.concurrent.*;

public class Overseer {
	private final Map<IBitmask, Set<Todo>> todos = new HashMap<>();
	private final Map<String, Object> cache = new ConcurrentHashMap<>();
	private final Set<Node> nodes = new HashSet<>();
	private final Set<Node> startingNodes = new HashSet<>();
	private final Set<Node> endingNodes = new HashSet<>();
	private final List<Node> nodeOfIndex = new ArrayList<>();
	private final Map<Node, Integer> indexOfNode = new HashMap<>();
	private final Map<Arc, Node> arcToOutputNode = new HashMap<>();
	private int tick = 0;
	private boolean started = false;

	public Overseer(Set<Todo> todoSet) {
		for (Todo todo : todoSet) {
			nodes.addAll(todo.getDependencies());
			arcToOutputNode.put(todo.getArc(), todo.getOutputNode());
			nodes.add(todo.getOutputNode());
		}

		for (Primable primable : nodes) {
			primable.primeWith(this);
		}
		for (Primable primable : arcToOutputNode.keySet()) {
			primable.primeWith(this);
		}

		int i = 0;
		for (Node node : nodes) {
			nodeOfIndex.add(node);
			indexOfNode.put(node, i);
			i++;
		}

		for (Todo todo : todoSet) {
			IBitmask bitmask = new ArrayBitmask(nodes.size()); // hardcode ArrayBitmask for now.
			for (Node dependency : todo.getDependencies()) {
				bitmask.set(indexOfNode.get(dependency));
			}

			if (todos.containsKey(bitmask)) {
				todos.get(bitmask).add(todo);
			}
			else {
				Set<Todo> set = new HashSet<>();
				set.add(todo);
				todos.put(bitmask, set);
			}
		}
	}

	public void start() {
		// check population of startingNodes
		for (Node startingNode : startingNodes) {
			if (startingNode.getProgress() != 1) {
				throw new IllegalStateException("Starting nodes not fully populated; unable to start!");
			}
		}

		started = true;
		ticktock();
	}

	private void end() {
		System.out.println("Overseer finished!");
	}

	public Map<String, Object> getResultCache() {
		Map<String, Object> resultCache = new HashMap<>();
		for (Node endingNode : endingNodes) {
			for (String datumName : endingNode.datumNames) {
				resultCache.put(datumName, cache.get(datumName));
			}
		}

		return resultCache;
	}

	private boolean hasEnded() {
		for (Node endingNode : endingNodes) {
			if (endingNode.getProgress() != 1) {
				return false;
			}
		}

		return true;
	}

	private boolean hasStarted() {
		return started;
	}

	public void ticktock() {
		if (!hasStarted()) { // todo i think this could be gotten rid of since ticktock is in arc now
			return;
		}
		if (hasEnded()) {
			end();
			return;
		}

		tick++;

		IBitmask sitrep = new ArrayBitmask(nodes.size());
		for (Node node : nodes) {
			if (node.getProgress() == 1) {
				sitrep.set(indexOfNode.get(node));
			}
		}


		Set<Todo> todosNow = new HashSet<>();
		for (Map.Entry<IBitmask, Set<Todo>> todoEntry : todos.entrySet()) {
			if (sitrep.compareTo(todoEntry.getKey()) >= 0) { // all where requirements are satisfied
				for (Todo todo : todoEntry.getValue()) {
					if (todo.getArc().status == ArcStatus.IDLE) { // until it finds one that's not finished
						todo.getArc().status = ArcStatus.STAND_BY;
						todosNow.add(todo);
					}
				}
			}
		}
		/* This may not be necessary as only one dependency state is changed per tick.
		 * A direct comparison may therefore suffice and can be evaluated in O(1).
		 * I'll keep this here for now.
		 *
		 * The only reason why this is here is because you can change multiple node's progress
		 * during initialization which violates the one tick - at most one extra node done principle*/


		System.out.println("todosNow.size() = " + todosNow.size());
		Todo[] todoArray = todosNow.toArray(new Todo[0]);
		List<RecursiveAction> tasks = new ArrayList<>(); // parallel stream doesn't work fsr

		for (int i = 0; i < todoArray.length; i++) {
			int finalI = i;
			tasks.add(new RecursiveAction() {
				@Override
				protected void compute() {
					Todo todo = todoArray[finalI];
					todo.getArc().runWrapper();
					System.out.println("tick = " + tick + ", todo = " + todo);
				}
			});
		}

		ForkJoinTask.invokeAll(tasks);
	}

	public void setAsStarting(Set<Node> nodeSet) {
		for (Node node : nodeSet) {
			if (!nodes.contains(node)) {
				throw new IllegalArgumentException("Node not part of graph!");
			}
			else {
				startingNodes.add(node);
			}
		}
	}

	public void setAsEnding(Set<Node> nodeSet) {
		for (Node node : nodeSet) {
			if (!nodes.contains(node)) {
				throw new IllegalArgumentException("Node not part of graph!");
			}
			else {
				endingNodes.add(node);
			}
		}
	}

	public Node getOutputNode(Arc arc) {
		return arcToOutputNode.get(arc);
	}

	public Map<String, Object> getCache() { // just give the full cache in case arc needs to iterate through it.
		return cache;
	}
}
