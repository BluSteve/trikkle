package org.trikkle;

import org.trikkle.structs.MultiHashMap;
import org.trikkle.structs.MultiMap;
import org.trikkle.viz.MermaidGraphViz;

import java.util.*;

/**
 * An execution graph. Graph never modifies the links, nodes, or arcs that are passed to it. It only uses them to
 * create data structures.
 *
 * @since 0.1.0
 */
public final class Graph implements Congruent<Graph> {
	/**
	 * If true, the graph will allow cycles. If false, the graph will throw an exception if it has a cycle.
	 */
	@SuppressWarnings("CanBeFinal")
	public static boolean ALLOW_CYCLES = false;
	public final Set<Link> links;
	public final Set<Primable> primables;
	public final Set<Arc> arcs;
	public final Set<Node> nodes;
	public final Map<Arc, Integer> arcIndex = new HashMap<>();
	public final Map<Node, Integer> nodeIndex = new HashMap<>();
	public final Arc[] arcArray;
	public final Node[] nodeArray;

	public final Set<Node> startingNodes = new HashSet<>();
	public final Set<Node> endingNodes = new HashSet<>();
	public final Map<Arc, Link> arcMap = new HashMap<>();
	public final MultiMap<Node, Link> outputNodeMap = new MultiHashMap<>();
	public final MultiMap<Node, Node> dependenciesOfNode = new MultiHashMap<>();
	public final Map<String, Node> nodeOfPointer = new HashMap<>();

	/**
	 * Create a graph with the given links. If the type of the given links is not a set, it will be converted into a
	 * set, otherwise the set object will be used without copying.
	 *
	 * @param links the links of the graph
	 */
	public Graph(List<Link> links) {
		if (links == null || links.isEmpty()) {
			throw new IllegalArgumentException("Graph must have at least one link!");
		}
		this.links = new HashSet<>(links);

		arcs = arcIndex.keySet();
		nodes = nodeIndex.keySet();

		int nodeI = 0, arcI = 0;
		Set<Node> dependencyNodes = new HashSet<>(); // all nodes that have been dependencies
		for (Link link : links) {
			if (arcMap.containsKey(link.getArc())) {
				throw new IllegalArgumentException("The same arc cannot be used for two links!");
			}

			// indexing data structures. the order of traversal is constant
			for (Node dependency : link.getDependencies()) {
				if (!nodeIndex.containsKey(dependency)) {
					nodeIndex.put(dependency, nodeI++);
				}
			}
			for (Node outputNode : link.getOutputNodes()) {
				if (!nodeIndex.containsKey(outputNode)) {
					nodeIndex.put(outputNode, nodeI++);
				}
			}
			arcIndex.put(link.getArc(), arcI++);

			// assistant data structures
			arcMap.put(link.getArc(), link);
			for (Node outputNode : link.getOutputNodes()) {
				outputNodeMap.putOne(outputNode, link);
				for (Node dependency : link.getDependencies()) {
					dependenciesOfNode.putOne(outputNode, dependency);
				}
			}

			dependencyNodes.addAll(link.getDependencies());
		}
		primables = new HashSet<>(nodes);
		primables.addAll(arcs);

		// create arrays
		arcArray = new Arc[arcs.size()];
		nodeArray = new Node[nodes.size()];
		for (Map.Entry<Arc, Integer> entry : arcIndex.entrySet()) {
			arcArray[entry.getValue()] = entry.getKey();
		}
		for (Map.Entry<Node, Integer> entry : nodeIndex.entrySet()) {
			nodeArray[entry.getValue()] = entry.getKey();
		}

		// check for no duplicate pointers
		for (Node node : nodes) {
			for (String pointer : node.pointers) {
				Node rnode = nodeOfPointer.put(pointer, node);
				if (rnode != null) {
					throw new IllegalArgumentException("Two nodes cannot have the same pointer " + pointer + "!");
				}
			}
		}

		// find ending nodes
		for (Node node : nodes) {
			if (!dependencyNodes.contains(node)) {
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
		this(Arrays.asList(links));
	}

	/**
	 * Merges the graphs into one optimized graph, using the {@code endingNodes} as the ending nodes of the merged
	 * graph.
	 *
	 * @param graphs      Graphs to merge in descending order of priority
	 * @param endingNodes the ending Nodes of the merged Graph
	 * @return the merged Graph
	 */
	public static Graph mergeGraphs(List<Graph> graphs, Collection<Node> endingNodes) {
		Map<Node, Graph> graphUsedOfNode = new HashMap<>();
		for (Node endingNode : endingNodes) {
			for (Graph graph : graphs) {
				if (graph.nodes.contains(endingNode)) { // if this Graph offers a path to obtain this ending Node
					graphUsedOfNode.put(endingNode, graph);
					break;
				}
			}
		}

		if (graphUsedOfNode.size() != endingNodes.size()) {
			throw new IllegalArgumentException("Not all ending nodes are reachable by the given graphs!");
		}

		List<Link> finalLinks = new ArrayList<>();
		for (Map.Entry<Node, Graph> nodeGraphEntry : graphUsedOfNode.entrySet()) {
			Node node = nodeGraphEntry.getKey();
			Graph graph = nodeGraphEntry.getValue().findPrunedGraphFor(Collections.singleton(node));
			finalLinks.addAll(graph.links);
		}

		return new Graph(finalLinks);
	}

	/**
	 * Combines all links from multiple graphs into one graph.
	 *
	 * @param graphs the Graphs to concatenate
	 * @return the combined Graph
	 */
	public static Graph concatGraphs(Collection<Graph> graphs) {
		Set<Link> finalLinks = new HashSet<>();
		for (Graph graph : graphs) {
			finalLinks.addAll(graph.links);
		}

		return new Graph(new ArrayList<>(finalLinks));
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

		/*
		 find link which creates this targetEndingNodes
		 record this link
		 get the dependencies of this link
		 for each dependency find link which creates it
		*/

		List<Link> finalLinks = new ArrayList<>();

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

		return new Graph(finalLinks);
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
}
