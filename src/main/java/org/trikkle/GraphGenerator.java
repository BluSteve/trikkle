package org.trikkle;

import java.util.*;

public class GraphGenerator {
	private final static long SEED = 504957110;

	public static Graph generateGraph(int numNodes, int numArcs) {
		if (numArcs > numNodes) {
			throw new IllegalArgumentException("numArcs must be less than or equal to numNodes");
		}

		// generate nodes
		List<Node> nodes = new ArrayList<>();
		for (int i = 0; i < numNodes; i++) {
			nodes.add(new DiscreteNode(Collections.singleton(intToExcelColumn(i + 1))));
		}
		// generate arcs
		List<Arc> arcs = new ArrayList<>();
		for (int i = 0; i < numArcs; i++) {
			Arc arc = new Arc.AutoArc() {
				@Override
				public void run() {
				}
			};
			arc.name = String.valueOf(i + 1);
			arcs.add(arc);
		}

		// randomly generate todos
		return getGraph(nodes, arcs, new Random(SEED));
	}

	private static Graph getGraph(List<Node> nodes, List<Arc> arcs, Random random) {
		int numNodes = nodes.size();
		int numArcs = arcs.size();

		List<Todo> todos = new ArrayList<>();
		Map<Node, Set<Node>> dependenciesOfNode = new HashMap<>();
		Set<Node> unusedNodes = new HashSet<>(nodes);
		List<Node> unusedOutputNodes = new ArrayList<>(nodes);
		for (int i = 0; i < numArcs; i++) {
			int numDependencies = random.nextInt(numNodes + 1);
			Set<Node> dependencies = new HashSet<>();
			for (int j = 0; j < numDependencies; j++) {
				dependencies.add(nodes.get(random.nextInt(numNodes)));
			}

			Node outputNode = null;
			Collections.shuffle(unusedOutputNodes, random);
			for (Node unusedOutputNode : unusedOutputNodes) {
				HashMap<Node, Set<Node>> copy = new HashMap<>(dependenciesOfNode);
				copy.put(unusedOutputNode, dependencies);
				if (!Graph.hasCycle(copy)) {
					outputNode = unusedOutputNode;
					break;
				}
			}
			if (outputNode == null) {
				return getGraph(nodes, arcs, new Random(random.nextLong()));
			}

			todos.add(new Todo(dependencies, arcs.get(i), outputNode));
			dependenciesOfNode.put(outputNode, dependencies);

			unusedNodes.remove(outputNode);
			unusedNodes.removeAll(dependencies);
			unusedOutputNodes.remove(outputNode);
		}

		// add unused nodes to the dependencies of a random to do in to dos
		for (Node node : unusedNodes) {
			todos.get(random.nextInt(numArcs)).getDependencies().add(node);
		}

		return new Graph(new HashSet<>(todos));
	}

	private static String intToExcelColumn(int n) {
		if (n <= 0) {
			throw new IllegalArgumentException("n must be positive");
		}
		StringBuilder sb = new StringBuilder();
		while (n > 0) {
			int rem = n % 26;
			if (rem == 0) {
				sb.append('Z');
				n = (n / 26) - 1;
			}
			else {
				sb.append((char) ((rem - 1) + 'A'));
				n = n / 26;
			}
		}
		return sb.reverse().toString();
	}
}
