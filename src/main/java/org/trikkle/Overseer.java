package org.trikkle;

import java.util.*;
import java.util.concurrent.*;

public class Overseer {
	private final MultiMap<IBitmask, Todo> todos = new MultiHashMap<>();
	private final Map<String, Object> cache = new ConcurrentHashMap<>();
	private final Map<Arc, Node> arcToOutputNode;
	private final Set<Node> nodes;
	private final Set<Node> startingNodes;
	private final Set<Node> endingNodes;
	private final List<Node> nodeOfIndex = new ArrayList<>();
	private final Map<Node, Integer> indexOfNode = new HashMap<>();
	private final Map<String, Node> nodeOfDatumName = new HashMap<>();
	private int tick = 0;

	public Overseer(Graph graph) {
		this.nodes = graph.getNodes();
		this.startingNodes = graph.getStartingNodes();
		this.endingNodes = graph.getEndingNodes();
		this.arcToOutputNode = graph.getArcToOutputNode();

		// Prime Nodes and Arcs with this Overseer
		for (Primable primable : nodes) {
			primable.primeWith(this);
		}
		for (Primable primable : arcToOutputNode.keySet()) {
			primable.primeWith(this);
		}


		// Generate helper indices
		int i = 0;
		for (Node node : nodes) {
			nodeOfIndex.add(node);
			indexOfNode.put(node, i);
			for (String datumName : node.datumNames) {
				nodeOfDatumName.put(datumName, node);
			}
			i++;
		}


		// Generate bitmasks for each To do
		for (Todo todo : graph.getTodos()) {
			IBitmask bitmask = new ArrayBitmask(nodes.size()); // hardcode ArrayBitmask for now.
			for (Node dependency : todo.getDependencies()) {
				bitmask.set(indexOfNode.get(dependency));
			}

			todos.putOne(bitmask, todo);
		}
	}

	public void start() {
		// check population of startingNodes
		for (Node startingNode : startingNodes) {
			if (startingNode.getProgress() != 1) {
				throw new IllegalStateException("Starting nodes not fully populated; unable to start!");
			}
		}

		ticktock(null);
	}

	private void end() {
		System.out.println("Overseer finished!");
	}

	public Map<String, Object> getResultCache() {
		Map<String, Object> resultCache = new HashMap<>();
		for (Node endingNode : endingNodes) {
			for (String datumName : endingNode.datumNames) {
				Object datum = cache.get(datumName);
				// todo decide whether to throw an exception here.
//				if (datum == null) {
//					throw new NullPointerException("Result datum \""+ datumName + "\" is null!");
//				}
				resultCache.put(datumName, datum);
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

	public void ticktock(Node outputNode) { // passing callingArc is only for debugging purposes
		tick++;
		System.out.println("\ntick = " + tick);
		if (hasEnded()) {
			end();
			return;
		}

		if (outputNode != null) {
			System.out.printf("tick = %d, just filled %s%n", tick, outputNode.datumNames);
		}
		else {
			System.out.printf("tick = %d, callingArc not passed%n", tick);
		}


		// Get all Todos with idle Arcs that the current state allows to be executed
		IBitmask state = getCurrentState();
		Set<Todo> todosNow = new HashSet<>();
		for (Map.Entry<IBitmask, Set<Todo>> todoEntry : todos.entrySet()) {
			if (state.compareTo(todoEntry.getKey()) >= 0) { // all where requirements are satisfied
				for (Todo todo : todoEntry.getValue()) {
					if (todo.getArc().status == ArcStatus.IDLE) { // until it finds one that's not finished
						todo.getArc().status = ArcStatus.STAND_BY;
						todosNow.add(todo);
					}
				}
			}
		}

		System.out.printf("tick = %d, todosNow.size() = %d%n", tick, todosNow.size());


		// Run all todos that can be done now (aka todosNow) in parallel.
		Todo[] todoArray = todosNow.toArray(new Todo[0]);
		List<RecursiveAction> tasks = new ArrayList<>(); // parallel stream doesn't work fsr
		for (int i = 0; i < todoArray.length; i++) {
			int finalI = i;
			tasks.add(new RecursiveAction() {
				@Override
				protected void compute() {
					Todo todo = todoArray[finalI];
					System.out.printf("Started: tick = %d, todo = %s%n", tick, todo);
					todo.getArc().runWrapper();
				}
			});
		}
		ForkJoinTask.invokeAll(tasks);
	}

	private IBitmask getCurrentState() {
		IBitmask state = new ArrayBitmask(nodes.size());
		for (Node node : nodes) {
			if (node.isUsable()) {
				state.set(indexOfNode.get(node));
			}
		}
		return state;
	}

	public Node getOutputNodeOfArc(Arc arc) {
		return arcToOutputNode.get(arc);
	}

	public Node getOutputNodeOfDatum(String datumName) {
		return nodeOfDatumName.get(datumName);
	}

	public Map<String, Object> getCache() { // just give the full cache in case arc needs to iterate through it.
		return cache;
	}
}
