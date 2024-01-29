package org.trikkle;

import org.trikkle.utils.MultiHashMap;
import org.trikkle.utils.MultiMap;
import org.trikkle.utils.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class Graph {
	public final Set<Todo> todos;
	public final Set<Arc> arcs;
	public final Set<Node> nodes = new HashSet<>();
	public final Set<Node> startingNodes = new HashSet<>();
	public final Set<Node> endingNodes = new HashSet<>();
	public final Map<Arc, Pair<Todo, Node>> arcMap = new HashMap<>();
	public final Map<Node, Pair<Todo, Arc>> outputNodeMap = new HashMap<>();
	private Map<Node, Graph> prunedGraphOfNode;

	public Graph(Set<Todo> todos) {
		this.todos = todos;

		Set<Node> dependencies = new HashSet<>();
		for (Todo todo : todos) {
			Collection<Node> existingOutputNodes =
					arcMap.values().stream().map(Pair::getSe).collect(Collectors.toList());
			if (existingOutputNodes.contains(todo.getOutputNode())) {
				throw new IllegalArgumentException("Two Arcs cannot point to the same output Node!");
			}
			if (arcMap.containsKey(todo.getArc())) {
				throw new IllegalArgumentException("The same Arc cannot be used for two Todos!");
			}

			nodes.addAll(todo.getDependencies());
			nodes.add(todo.getOutputNode());
			arcMap.put(todo.getArc(), new Pair<>(todo, todo.getOutputNode()));
			outputNodeMap.put(todo.getOutputNode(), new Pair<>(todo, todo.getArc()));

			dependencies.addAll(todo.getDependencies());
		}
		arcs = arcMap.keySet();

		// find ending nodes
		for (Node node : nodes) {
			if (!dependencies.contains(node)) {
				endingNodes.add(node);
			}
		}

		// find starting nodes
		for (Node node : nodes) {
			if (!outputNodeMap.containsKey(node)) {
				startingNodes.add(node);
			}
		}
	}

	public static Graph mergeGraphs(List<Graph> graphs, Set<Node> endingNodes) {
		MultiMap<Node, Way> waysToGetNode = new MultiHashMap<>();
		for (Node endingNode : endingNodes) {
			for (int i = 0; i < graphs.size(); i++) {
				Graph graph = graphs.get(i);

				if (graph.nodes.contains(endingNode)) { // if this Graph offers a path to obtain this ending Node
					Way way = new Way(i, graph.outputNodeMap.get(endingNode).fi.getDependencies());
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
		for (int i = 0; i < graphs.size(); i++) {
			Graph graph = graphs.get(i);

			for (Node node : endingNodesOfGraph.get(i)) {
				Graph prunedGraph = graph.findPrunedGraphFor(node);
				finalTodos.addAll(prunedGraph.todos);
			}
		}

		// endingNodes is a subset of "finalEndingNodes". This allows the program to finish earlier, computing only
		// necessary nodes.
		return new Graph(finalTodos);
	}

	public static Graph concatGraphs(List<Graph> graphs) { // todo optimize
		Graph aggGraph = graphs.get(0);
		for (int i = 1; i < graphs.size(); i++) {
			Graph graph = graphs.get(i);

			// get union of aggGraph and graph's todos
			Set<Todo> todosUnion = new HashSet<>(aggGraph.todos);
			todosUnion.addAll(graph.todos);

			aggGraph = new Graph(todosUnion);
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

		Stack<Node> nodeStack = new Stack<>();
		nodeStack.push(endingNode);
		while (!nodeStack.empty()) {
			Node node = nodeStack.pop();
			Todo generatingTodo = outputNodeMap.get(node) == null ? null : outputNodeMap.get(node).fi;

			if (generatingTodo != null) {
				finalTodos.add(generatingTodo);
				for (Node dependency : generatingTodo.getDependencies()) {
					nodeStack.push(dependency);
				}
			}
		}

		Graph prunedGraph = new Graph(finalTodos);
		prunedGraphOfNode.put(endingNode, prunedGraph);
		return prunedGraph;
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
