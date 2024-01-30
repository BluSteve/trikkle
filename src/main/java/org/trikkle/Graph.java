package org.trikkle;

import org.trikkle.utils.MultiHashMap;
import org.trikkle.utils.MultiMap;

import java.util.*;

public class Graph {
	public static boolean ALLOW_CYCLES = false;
	public final Set<Link> links;
	public final Set<Arc> arcs;
	public final Set<Node> nodes = new HashSet<>();
	public final Set<Node> startingNodes = new HashSet<>();
	public final Set<Node> endingNodes = new HashSet<>();
	public final Map<Arc, Link> arcMap = new HashMap<>();
	public final Map<Node, Link> outputNodeMap = new HashMap<>();
	public final Map<Node, Set<Node>> dependenciesOfNode = new HashMap<>();
	private Map<Node, Graph> prunedGraphOfNode;

	public Graph(Set<Link> links) {
		this.links = links;

		Set<Node> dependencies = new HashSet<>();
		for (Link link : links) {
			Set<Node> existingOutputNodes = outputNodeMap.keySet();
			if (existingOutputNodes.contains(link.getOutputNode())) {
				throw new IllegalArgumentException("Two Arcs cannot point to the same output Node!");
			}
			if (arcMap.containsKey(link.getArc())) {
				throw new IllegalArgumentException("The same Arc cannot be used for two Links!");
			}

			nodes.addAll(link.getDependencies());
			nodes.add(link.getOutputNode());
			arcMap.put(link.getArc(), link);
			outputNodeMap.put(link.getOutputNode(), link);
			dependenciesOfNode.put(link.getOutputNode(), link.getDependencies());

			dependencies.addAll(link.getDependencies());
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

		if (hasCycle() && !ALLOW_CYCLES) {
			throw new IllegalArgumentException("Graph has a cycle!");
		}
	}

	public static Graph mergeGraphs(List<Graph> graphs, Set<Node> endingNodes) {
		MultiMap<Node, Way> waysToGetNode = new MultiHashMap<>();
		for (Node endingNode : endingNodes) {
			for (int i = 0; i < graphs.size(); i++) {
				Graph graph = graphs.get(i);

				if (graph.nodes.contains(endingNode)) { // if this Graph offers a path to obtain this ending Node
					Way way = new Way(i, graph.outputNodeMap.get(endingNode).getDependencies());
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

		Set<Link> finalLinks = new HashSet<>();
		for (int i : graphUsedOfNode.values()) {
			Graph graph = graphs.get(i);
			finalLinks.addAll(graph.links);
		}

		return new Graph(finalLinks).findPrunedGraphFor(endingNodes);
	}

	public static Graph concatGraphs(List<Graph> graphs) {
		Set<Link> finalLinks = new HashSet<>();
		for (Graph graph : graphs) {
			finalLinks.addAll(graph.links);
		}

		return new Graph(finalLinks);
	}

	public static boolean hasCycle(Map<Node, Set<Node>> dependenciesOfNode) {
		Set<Node> nodes = dependenciesOfNode.keySet();
		Set<Node> checked = new HashSet<>();
		for (Node node : nodes) {
			Stack<Node> nodeStack = new Stack<>();
			nodeStack.push(node);

			while (!nodeStack.empty()) {
				Node popped = nodeStack.pop();
				if (checked.contains(popped)) continue;

				Set<Node> dependencies = dependenciesOfNode.get(popped);
				if (dependencies == null) continue;
				// without the below line it'll loop forever when a node has itself as a dependency
				if (dependencies.contains(node) || dependencies.contains(popped)) return true;

				nodeStack.addAll(dependencies);
			}

			checked.add(node);
		}

		return false;
	}

	public Graph findPrunedGraphFor(Set<Node> targetEndingNodes) {
		// Note: targetEndingNodes may not be in endingNodes. It's merely the targetEndingNodes of the PRUNED graph.
		if (!nodes.containsAll(targetEndingNodes)) {
			throw new IllegalArgumentException("targetEndingNodes must be a subset of nodes!");
		}

		if (targetEndingNodes.size() == 1) {
			if (prunedGraphOfNode == null) {
				prunedGraphOfNode = new HashMap<>();
			} else if (prunedGraphOfNode.containsKey(targetEndingNodes.iterator().next())) { // cache hit
				return prunedGraphOfNode.get(targetEndingNodes.iterator().next());
			}
		}

		/*
		 find link which creates this targetEndingNodes
		 record this link
		 get the dependencies of this link
		 for each dependency find link which creates it
		*/

		Set<Link> finalLinks = new HashSet<>();

		Stack<Node> nodeStack = new Stack<>();
		for (Node targetEndingNode : targetEndingNodes) {
			nodeStack.push(targetEndingNode);
		}
		while (!nodeStack.empty()) {
			Node node = nodeStack.pop();
			Link generatingLink = outputNodeMap.get(node) == null ? null : outputNodeMap.get(node);

			if (generatingLink != null) {
				finalLinks.add(generatingLink);
				for (Node dependency : generatingLink.getDependencies()) {
					nodeStack.push(dependency);
				}
			}
		}

		Graph prunedGraph = new Graph(finalLinks);
		if (targetEndingNodes.size() == 1) {
			prunedGraphOfNode.put(targetEndingNodes.iterator().next(), prunedGraph);
		}
		return prunedGraph;
	}

	public boolean hasCycle() {
		return hasCycle(dependenciesOfNode);
	}

	public boolean congruentTo(Graph graph) {
		// check that links are the same size and that all links are congruent to some link in graph.links
		if (links.size() != graph.links.size()) {
			return false;
		}
		for (Link link : links) {
			boolean found = false;
			for (Link graphLink : graph.links) {
				if (link.congruentTo(graphLink)) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Graph graph = (Graph) o;
		return Objects.equals(links, graph.links);
	}

	@Override
	public int hashCode() {
		return Objects.hash(links);
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
