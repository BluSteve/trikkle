package org.trikkle;

import org.trikkle.structs.MultiHashMap;
import org.trikkle.structs.MultiMap;
import org.trikkle.viz.MermaidGraphViz;

import java.util.*;

public final class Graph implements Congruent<Graph> {
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
		if (links == null || links.isEmpty()) {
			throw new IllegalArgumentException("Graph must have at least one Link!");
		}
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

		// throw an error if two nodes have any datum names in common
		Set<String> datumNames = new HashSet<>();
		for (Node node : nodes) {
			for (String datumName : node.datumNames) {
				if (datumNames.contains(datumName)) {
					throw new IllegalArgumentException("Two Nodes cannot have the same datum name!");
				}
			}
			datumNames.addAll(node.datumNames);
		}

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

	public Graph(Link... links) {
		this(new HashSet<>(Arrays.asList(links)));
	}

	/**
	 * Merges the Graphs into one optimized Graph, using the endingNodes as the ending Nodes of the merged Graph.
	 *
	 * @param graphs      Graphs to merge in descending order of priority
	 * @param endingNodes the ending Nodes of the merged Graph
	 * @return the merged Graph
	 */
	public static Graph mergeGraphs(List<Graph> graphs, Set<Node> endingNodes) {
		MultiMap<Node, Way> waysToGetNode = new MultiHashMap<>();
		for (Node endingNode : endingNodes) {
			for (int i = 0; i < graphs.size(); i++) {
				Graph graph = graphs.get(i);

				if (graph.nodes.contains(endingNode)) { // if this Graph offers a path to obtain this ending Node
					Way way = new Way(i, graph.getAllDependenciesOfNode(endingNode));
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
				if (ways == null) {
					throw new IllegalArgumentException("No way to get to ending Node " + endingNode + "!");
				}

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
					// todo this could be faster with an ordered list
					int lowestGraphIndex = Integer.MAX_VALUE;
					for (Way way : ways) {
						lowestGraphIndex = Math.min(lowestGraphIndex, way.graphIndex);
					}
					graphUsedOfNode.put(endingNode, lowestGraphIndex);
				}
			}
		}

		Set<Link> finalLinks = new HashSet<>();
		for (Map.Entry<Node, Integer> nodeIntegerEntry : graphUsedOfNode.entrySet()) {
			Node node = nodeIntegerEntry.getKey();
			int i = nodeIntegerEntry.getValue();
			Graph graph = graphs.get(i).findPrunedGraphFor(Collections.singleton(node));
			finalLinks.addAll(graph.links);
		}

		return new Graph(finalLinks);
	}

	/**
	 * Combines all links from multiple Graphs into one Graph.
	 *
	 * @param graphs the Graphs to concatenate
	 * @return the combined Graph
	 */
	public static Graph concatGraphs(Set<Graph> graphs) {
		Set<Link> finalLinks = new HashSet<>();
		for (Graph graph : graphs) {
			finalLinks.addAll(graph.links);
		}

		return new Graph(finalLinks);
	}

	public static Graph concatGraphs(Graph... graphs) {
		return concatGraphs(new HashSet<>(Arrays.asList(graphs)));
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

	public Set<Node> getAllDependenciesOfNode(Node node) {
		if (!nodes.contains(node)) {
			throw new IllegalArgumentException("Node must be in the Graph!");
		}

		Set<Node> dependencies = new HashSet<>();
		Stack<Node> nodeStack = new Stack<>();
		nodeStack.push(node);
		while (!nodeStack.empty()) {
			Node popped = nodeStack.pop();
			if (dependencies.contains(popped)) continue;
			dependencies.add(popped);
			Set<Node> nodeDependencies = dependenciesOfNode.get(popped);
			if (nodeDependencies != null) {
				nodeStack.addAll(nodeDependencies);
			}
		}
		dependencies.remove(node);
		return dependencies;
	}

	public Graph findPrunedGraphFor(Set<Node> targetEndingNodes) {
		// Note: targetEndingNodes may not be in endingNodes. It's merely the targetEndingNodes of the PRUNED graph.
		if (!nodes.containsAll(targetEndingNodes)) {
			throw new IllegalArgumentException("targetNodes must be a subset of the Graph's nodes!");
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

	public Graph findPrunedGraphFor(Node... targetEndingNodes) {
		return findPrunedGraphFor(new HashSet<>(Arrays.asList(targetEndingNodes)));
	}

	public boolean hasCycle() {
		return hasCycle(dependenciesOfNode);
	}

	@Override
	public boolean congruentTo(Graph graph) {
		return Congruent.setsCongruent(links, graph.links);
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

	@Override
	public String toString() {
		return MermaidGraphViz.defaultVisualize(this);
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
