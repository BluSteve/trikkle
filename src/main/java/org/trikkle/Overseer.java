package org.trikkle;

import org.trikkle.utils.IBitmask;
import org.trikkle.utils.MultiHashMap;
import org.trikkle.utils.MultiMap;
import org.trikkle.utils.StrictConcurrentHashMap;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class Overseer {
	private final Graph g;
	private final MultiMap<IBitmask, Todo> todos = new MultiHashMap<>();
	private final Map<String, Object> cache = new StrictConcurrentHashMap<>();
	private final List<Node> nodeOfIndex = new ArrayList<>();
	private final Map<Node, Integer> indexOfNode = new HashMap<>();
	private final Map<String, Node> nodeOfDatumName = new HashMap<>();
	private int tick = 0;

	public Overseer(Graph graph) {
		this.g = graph;

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


		// Generate bitmasks for each To do
		for (Todo todo : g.todos) {
			IBitmask bitmask = IBitmask.getBitmask(g.nodes.size());
			for (Node dependency : todo.getDependencies()) {
				bitmask.set(indexOfNode.get(dependency));
			}

			todos.putOne(bitmask, todo);
		}
	}

	public void start() {
		// check population of startingNodes
		for (Node startingNode : g.startingNodes) {
			if (!startingNode.isUsable()) {
				throw new IllegalStateException("Starting nodes not fully populated; unable to start!");
			}
		}

		ticktock(null);
	}

	void ticktock(Node outputNode) { // passing outputNode is only for debugging purposes
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

	private void end() {
		System.out.println("Overseer finished!");
	}

	private boolean hasEnded() {
		for (Node endingNode : g.endingNodes) {
			if (endingNode.getProgress() != 1) {
				return false;
			}
		}

		return true;
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

	public Node getOutputNodeOfArc(Arc arc) {
		return g.arcMap.get(arc).se;
	}

	public Node getOutputNodeOfDatum(String datumName) {
		return nodeOfDatumName.get(datumName);
	}

	public Map<String, Object> getCache() { // just give the full cache in case arc needs to iterate through it.
		return cache;
	}
}
