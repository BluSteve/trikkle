package org.trikkle;

import org.trikkle.utils.MultiHashMap;
import org.trikkle.utils.MultiMap;

import java.util.*;

public class Graph {
	private final Set<Todo> todos;

	private final Set<Node> nodes = new HashSet<>();
	private final Set<Arc> arcs = new HashSet<>();
	private final Map<Arc, Node> arcToOutputNode = new HashMap<>();

	private final Set<Node> startingNodes;
	private final Set<Node> endingNodes;

	private final Map<Node, Set<Node>> dependenciesOfNode = new HashMap<>();
	private final Map<Node, Todo> todoOfOutputNode = new HashMap<>();
	private Map<Node, Graph> prunedGraphOfNode;

	public Graph(Set<Todo> todos, Set<Node> startingNodes, Set<Node> endingNodes) {
		this.todos = todos;

		for (Todo todo : todos) {
			// Create arcToOutputNode
			Collection<Node> existingOutputNodes = arcToOutputNode.values();
			if (existingOutputNodes.contains(todo.getOutputNode())) {
				throw new IllegalArgumentException("Two Arcs cannot point to the same output Node!");
			}

			if (arcToOutputNode.containsKey(todo.getArc())) {
				throw new IllegalArgumentException("The same Arc cannot be used for two Todos!");
			}
			arcToOutputNode.put(todo.getArc(), todo.getOutputNode());

			nodes.addAll(todo.getDependencies());
			arcs.add(todo.getArc());
			nodes.add(todo.getOutputNode());
			todoOfOutputNode.put(todo.getOutputNode(), todo);
		}

		for (Node node : startingNodes) {
			if (!nodes.contains(node)) {
				throw new IllegalArgumentException("Starting Node not part of Graph!");
			}
		}
		for (Node node : endingNodes) {
			if (!nodes.contains(node)) {
				throw new IllegalArgumentException("Ending Node not part of Graph!");
			}
		}

		this.startingNodes = startingNodes;
		this.endingNodes = endingNodes;

		for (Todo todo : todos) {
			dependenciesOfNode.put(todo.getOutputNode(), todo.getDependencies());
		}
	}

	public static Graph mergeGraphs(List<Graph> graphs, Set<Node> endingNodes) {
		MultiMap<Node, Way> waysToGetNode = new MultiHashMap<>();
		for (Node endingNode : endingNodes) {
			for (int i = 0; i < graphs.size(); i++) {
				Graph graph = graphs.get(i);

				if (graph.nodes.contains(endingNode)) { // if this Graph offers a path to obtain this ending Node
					Way way = new Way(i, graph.dependenciesOfNode.get(endingNode));
					waysToGetNode.putOne(endingNode, way);
				}
			}
		}

		Map<Node, Integer> graphUsedOfNode = new HashMap<>();
		// this graph generates which nodes?
		List<Set<Node>> endingNodesOfGraph = new ArrayList<>(graphs.size());
		for (int i = 0; i < graphs.size(); i++) {
			endingNodesOfGraph.add(new HashSet<>());
		}

		Set<Node> hardDependencies = new HashSet<>();
		for (Map.Entry<Node, Set<Way>> nodeSetEntry : waysToGetNode.entrySet()) {
			Node key = nodeSetEntry.getKey();
			Set<Way> value = nodeSetEntry.getValue();
			if (value.size() == 1) { // if there's only one way to get this Node
				Way way = value.iterator().next();
				hardDependencies.addAll(way.dependencies);
				graphUsedOfNode.put(key, way.graphIndex);
				endingNodesOfGraph.get(way.graphIndex).add(key);
			}
		}

		for (Node endingNode : endingNodes) {
			// if not already resolved through hard dependency
			if (!graphUsedOfNode.containsKey(endingNode)) {
				Set<Way> ways = waysToGetNode.get(endingNode);

				// if subsumed under a hard dependency
				boolean allHard = false;
				for (Way way : ways) {
					if (hardDependencies.containsAll(way.dependencies)) {
						graphUsedOfNode.put(endingNode, way.graphIndex);
						endingNodesOfGraph.get(way.graphIndex).add(endingNode);
						allHard = true;
						break;
					}
				}

				// if not subsumed under a hard dependency
				if (!allHard) {
					int lowestGraphIndex = Integer.MAX_VALUE;
					for (Way way : ways) {
						lowestGraphIndex = Math.min(lowestGraphIndex, way.graphIndex);
					}
					graphUsedOfNode.put(endingNode, lowestGraphIndex);
					endingNodesOfGraph.get(lowestGraphIndex).add(endingNode);
				}
			}
		}


		Set<Todo> finalTodos = new HashSet<>();
		Set<Node> finalStartingNodes = new HashSet<>();
		for (int i = 0; i < graphs.size(); i++) {
			Graph graph = graphs.get(i);

			for (Node node : endingNodesOfGraph.get(i)) {
				Graph prunedGraph = graph.findPrunedGraphFor(node);
				finalTodos.addAll(prunedGraph.todos);
				finalStartingNodes.addAll(prunedGraph.startingNodes);
			}
		}

		// endingNodes is a subset of "finalEndingNodes". This allows the program to finish earlier, computing only
		// necessary nodes.
		return new Graph(finalTodos, finalStartingNodes, endingNodes);
	}

	public static Graph concatGraphs(List<Graph> graphs) {
		Graph aggGraph = graphs.get(0);
		for (int i = 1; i < graphs.size(); i++) {
			Graph graph = graphs.get(i);

			// get intersection between aggGraph's endingNodes and graph's startingNodes
			Set<Node> intersection = new HashSet<>(aggGraph.endingNodes);
			intersection.retainAll(graph.startingNodes);

			Set<Node> startingUnion = new HashSet<>(aggGraph.startingNodes);
			startingUnion.addAll(graph.startingNodes);
			startingUnion.removeAll(intersection);

			Set<Node> endingUnion = new HashSet<>(aggGraph.endingNodes);
			endingUnion.addAll(graph.endingNodes);
			endingUnion.removeAll(intersection);

			// get union of aggGraph and graph's todos
			Set<Todo> todosUnion = new HashSet<>(aggGraph.todos);
			todosUnion.addAll(graph.todos);

			aggGraph = new Graph(todosUnion, startingUnion, endingUnion);
		}

		return aggGraph;
	}

	private Graph findPrunedGraphFor(Node endingNode) {
		// Note: endingNode may not be in endingNodes. It's merely the endingNode of the PRUNED graph.

		if (prunedGraphOfNode == null) {
			prunedGraphOfNode = new HashMap<>();
		}
		else if (prunedGraphOfNode.containsKey(endingNode)) { // cache hit
			return prunedGraphOfNode.get(endingNode);
		}

//		if (!endingNodes.contains(endingNode)) {
//			throw new IllegalArgumentException("That's not an ending Node!");
//		}

		/*
		 find to do which creates this endingNode
		 record this to do
		 get the dependencies of this to do
		 for each dependency find to do which creates it
		*/

		Set<Todo> finalTodos = new HashSet<>();
		Set<Node> finalStartingNodes = new HashSet<>();
		Set<Node> finalEndingNodes = new HashSet<>();

		Stack<Node> nodeStack = new Stack<>();
		nodeStack.push(endingNode);
		while (!nodeStack.empty()) {
			Node node = nodeStack.pop();
			Todo generatingTodo = todoOfOutputNode.get(node);

			if (generatingTodo == null) { // means it's a starting Node
				finalStartingNodes.add(node);
			}
			else {
				finalTodos.add(generatingTodo);
				for (Node dependency : generatingTodo.getDependencies()) {
					nodeStack.push(dependency);
				}
			}
		}

		finalEndingNodes.add(endingNode);

		Graph prunedGraph = new Graph(finalTodos, finalStartingNodes, finalEndingNodes);
		prunedGraphOfNode.put(endingNode, prunedGraph);
		return prunedGraph;
	}

	public Set<Todo> getTodos() {
		return todos;
	}

	public Set<Node> getNodes() {
		return nodes;
	}

	public Map<Arc, Node> getArcToOutputNode() {
		return arcToOutputNode;
	}

	public Set<Arc> getArcs() {
		return arcs;
	}

	public Set<Node> getStartingNodes() {
		return startingNodes;
	}

	public Set<Node> getEndingNodes() {
		return endingNodes;
	}

	private static class Way {
		public final int graphIndex;
		public final Set<Node> dependencies;

		public Way(int graphIndex, Set<Node> dependencies) {
			this.graphIndex = graphIndex;
			this.dependencies = dependencies;
		}
	}
}
