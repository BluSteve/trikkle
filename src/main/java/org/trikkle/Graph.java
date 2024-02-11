package org.trikkle;

import org.trikkle.structs.MultiHashMap;
import org.trikkle.structs.MultiMap;
import org.trikkle.viz.MermaidGraphViz;

import java.util.*;

public final class Graph implements Congruent<Graph> {
	public static boolean ALLOW_CYCLES = false;
	public final Set<Link> links;
	public final Set<Primable> primables;
	public final Set<Arc> arcs;
	public final Set<Node> nodes = new HashSet<>();
	public final Set<Node> startingNodes = new HashSet<>();
	public final Set<Node> endingNodes = new HashSet<>();
	public final Map<Arc, Link> arcMap = new HashMap<>();
	public final MultiMap<Node, Link> outputNodeMap = new MultiHashMap<>();
	public final MultiMap<Node, Node> dependenciesOfNode = new MultiHashMap<>();
	private Map<Node, Graph> prunedGraphOfNode;

	public Graph(Set<Link> links) {
		if (links == null || links.isEmpty()) {
			throw new IllegalArgumentException("Graph must have at least one link!");
		}
		this.links = links;

		Set<Node> dependencies = new HashSet<>();
		for (Link link : links) {
			if (arcMap.containsKey(link.getArc())) {
				throw new IllegalArgumentException("The same arc cannot be used for two links!");
			}

			nodes.addAll(link.getDependencies());
			nodes.addAll(link.getOutputNodes());
			arcMap.put(link.getArc(), link);
			for (Node outputNode : link.getOutputNodes()) {
				outputNodeMap.putOne(outputNode, link);
				for (Node dependency : link.getDependencies()) {
					dependenciesOfNode.putOne(outputNode, dependency);
				}
			}

			dependencies.addAll(link.getDependencies());
		}
		arcs = arcMap.keySet();
		primables = new HashSet<>(nodes);
		primables.addAll(arcs);

		// todo remove this
		Set<String> datumNames = new HashSet<>();
		for (Node node : nodes) {
			for (String datumName : node.datumNames) {
				if (datumNames.contains(datumName)) {
					throw new IllegalArgumentException("Two nodes cannot have the same datum name!");
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
		Map<Node, Graph> graphUsedOfNode = new HashMap<>();
		for (Node endingNode : endingNodes) {
			for (Graph graph : graphs) {
				if (graph.nodes.contains(endingNode)) { // if this Graph offers a path to obtain this ending Node
					graphUsedOfNode.put(endingNode, graph);
					break;
				}
			}
		}

		Set<Link> finalLinks = new HashSet<>();
		for (Map.Entry<Node, Graph> nodeGraphEntry : graphUsedOfNode.entrySet()) {
			Node node = nodeGraphEntry.getKey();
			Graph graph = nodeGraphEntry.getValue().findPrunedGraphFor(Collections.singleton(node));
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

	public Set<Node> findAllDependencies(Node node) {
		if (!nodes.contains(node)) {
			throw new IllegalArgumentException("Node must be in the graph!");
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
			throw new IllegalArgumentException("targetNodes must be a subset of the graph's nodes!");
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
			Set<Link> generatingLinks = outputNodeMap.get(node) == null ? null : outputNodeMap.get(node);

			if (generatingLinks != null) {
				finalLinks.addAll(generatingLinks);
				for (Link generatingLink : generatingLinks) {
					for (Node dependency : generatingLink.getDependencies()) {
						nodeStack.push(dependency);
					}
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
