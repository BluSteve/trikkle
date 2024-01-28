package org.trikkle;

import java.util.*;

public class Graph {
	private final Set<Todo> todos;

	private final Set<Node> nodes;
	private final Set<Arc> arcs;
	private final Set<Node> startingNodes;
	private final Set<Node> endingNodes;

	private final Map<Node, Set<Node>> dependenciesOfNode;

	public Graph(Set<Todo> todos, Set<Node> startingNodes, Set<Node> endingNodes) {
		this.todos = todos;

		nodes = new HashSet<>();
		arcs = new HashSet<>();
		for (Todo todo : todos) {
			nodes.addAll(todo.getDependencies());
			arcs.add(todo.getArc());
			nodes.add(todo.getOutputNode());
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

		dependenciesOfNode = new HashMap<>();
		for (Todo todo : todos) {
			dependenciesOfNode.put(todo.getOutputNode(), todo.getDependencies());
		}
	}

	public static Graph mergeGraphs(List<Graph> graphs, Set<Node> startingNodes, Set<Node> endingNodes) {
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

		Set<Node> hardDependencies = new HashSet<>();
		for (Map.Entry<Node, Set<Way>> nodeSetEntry : waysToGetNode.entrySet()) {
			Node key = nodeSetEntry.getKey();
			Set<Way> value = nodeSetEntry.getValue();
			if (value.size() == 1) { // if there's only one way to get this Node
				Way way = value.iterator().next();
				hardDependencies.addAll(way.dependencies);
				graphUsedOfNode.put(key, way.graphIndex);
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
				}
			}
		}


		Set<Graph> finalGraphs = new HashSet<>();
		for (Integer graphIndex : graphUsedOfNode.values()) {
			finalGraphs.add(graphs.get(graphIndex));
		}

		Set<Todo> finalTodos = new HashSet<>();
		Set<Node> finalStartingNodes = new HashSet<>();
		for (Graph graph : finalGraphs) {
			finalTodos.addAll(graph.todos);
			finalStartingNodes.addAll(graph.startingNodes);
		}

		return new Graph(finalTodos, finalStartingNodes, endingNodes);
	}

	public Set<Todo> getTodos() {
		return todos;
	}

	public Set<Node> getNodes() {
		return nodes;
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
