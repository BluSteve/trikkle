package org.trikkle;

import java.util.*;

public class Graph {
	private final Set<Todo> todos;

	private final Set<Node> nodes;
	private final Set<Arc> arcs;
	private final Set<Node> startingNodes;
	private final Set<Node> endingNodes;

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
	}

	public static Graph mergeGraphs(List<Graph> graphs, Set<Node> startingNodes, Set<Node> endingNodes) {
		MultiMap<Node, Way> waysToGetNode = new MultiHashMap<>();
		for (Node endingNode : endingNodes) {
			for (int i = 0; i < graphs.size(); i++) {
				Graph graph = graphs.get(i);
				Map<Node, Set<Node>> nodeGraph = graph.getNodeGraph();
				Way way = new Way(i,nodeGraph.get(endingNode));
				waysToGetNode.putOne(endingNode, way);
			}
		}

		Map<Node, Integer> graphUsedOfNode = new HashMap<>();
		Set<Node> hardDependencies = new HashSet<>();
		for (Map.Entry<Node, Set<Way>> nodeSetEntry : waysToGetNode.entrySet()) {
			Node key = nodeSetEntry.getKey();
			Set<Way> value = nodeSetEntry.getValue();
			if (value.size() == 1) {
				Way way = value.iterator().next();
				hardDependencies.addAll(way.dependencies);
				graphUsedOfNode.put(key, way.graphIndex);
			}
		}

		for (Node endingNode : endingNodes) {
			// if not already resolved through hard dependency
			if (!graphUsedOfNode.containsKey(endingNode)) {
				Set<Way> ways = waysToGetNode.get(endingNode);
				boolean allHard = false;
				for (Way way : ways) {
					if (hardDependencies.containsAll(way.dependencies)) {
						graphUsedOfNode.put(endingNode, way.graphIndex);
						allHard = true;
						break;
					}
				}

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

		Set<Todo> todos = new HashSet<>();
		for (Graph graph : finalGraphs) {
			todos.addAll(graph.todos);
		}

		Graph mergedGraph = new Graph(todos, startingNodes, endingNodes);
		return mergedGraph;
	}

	private Map<Node, Set<Node>> getNodeGraph() {
		Map<Node, Set<Node>> nodeGraph = new HashMap<>();
		for (Todo todo : todos) {
			nodeGraph.put(todo.getOutputNode(), todo.getDependencies());
		}

		return nodeGraph;
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
