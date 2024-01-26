package org.trikkle;

import java.util.*;

public class Overseer {
	private final Map<IBitmask, Set<Todo>> todos = new HashMap<>();
	private final Map<String, Object> cache = new HashMap<>();
	private Set<Node> nodes = new HashSet<>(), startingNodes = new HashSet<>(), endingNodes = new HashSet<>();
	private List<Node> nodeOfIndex = new ArrayList<>();
	private Map<Node, Integer> indexOfNode = new HashMap<>();

	private Map<Arc, Node> arcToOutputNode = new HashMap<>();
	private int tick = 0;
	private boolean started = false;

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

		IBitmask sitrep = new ArrayBitmask(nodes.size());
		for (Node node : nodes) {
			if (node.getProgress() == 1) {
				sitrep.set(indexOfNode.get(node));
			}
		}

		Set<Todo> todoNow = new HashSet<>();
		for (Map.Entry<IBitmask, Set<Todo>> todoEntry : todos.entrySet()) {
			if (sitrep.compareTo(todoEntry.getKey()) >= 0) { // all where requirements are satisfied
				for (Todo todo : todoEntry.getValue()) {
					todoNow.add(todo);
				}
			}
		}
		/* This may not be necessary as only one dependency state is changed per tick.
		 * A direct comparison may therefore suffice and can be evaluated in O(1).
		 * I'll keep this here for now.
		 *
		 * The only reason why this is here is because you can change multiple node's progress
		 * during initialization which violates the one tick - at most one extra node done principle*/

		System.out.println("tick = " + tick);
		tick++;

		if (todoNow.isEmpty()) {
			System.out.println("Warning: no more Todos left.");
		}
		for (Todo todo : todoNow) {
			if (todo.getArc().status != ArcStatus.FINISHED) {
				System.out.println("todo = " + todo);
				todo.getArc().runWrapper();
			}
		}
	}

	public void addTodos(Set<Todo> todoSet) {
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
