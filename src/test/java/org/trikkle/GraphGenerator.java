package org.trikkle;

import org.trikkle.viz.MermaidGraphViz;

import java.util.*;

public class GraphGenerator {
	public static Graph generateGraph(int numNodes, int numArcs) {
		if (numArcs > numNodes) {
			throw new IllegalArgumentException("numArcs must be less than or equal to numNodes");
		}


		// generate nodes
		List<Node> nodes = new ArrayList<>();
		for (int i = 0; i < numNodes; i++) {
			nodes.add(new DiscreteNode(Set.of(intToExcelColumn(i + 1))));
		}
		// generate arcs
		List<Arc> arcs = new ArrayList<>();
		for (int i = 0; i < numArcs; i++) {
			Arc arc = new Arc.AutoArc() {
				@Override
				public void run() {
				}
			};
			arc.name = String.valueOf(i);
			arcs.add(arc);
		}

		// randomly generate todos
		Random random = new Random(1000);
		List<Todo> todos = new ArrayList<>();
		Set<Node> notUsedNodes = new HashSet<>(nodes);
		List<Node> notUsedOutputNodes = new ArrayList<>(nodes);
		for (int i = 0; i < numArcs; i++) {
			int numDependencies = random.nextInt(numNodes) + 1;
			Set<Node> dependencies = new HashSet<>();
			for (int j = 0; j < numDependencies; j++) {
				dependencies.add(nodes.get(random.nextInt(numNodes)));
			}

//				if (dependencies.containsAll(notUsedOutputNodes)) continue;
			Node outputNode = notUsedOutputNodes.get(random.nextInt(notUsedOutputNodes.size()));

			Todo todo = new Todo(dependencies, arcs.get(i), outputNode);
			todos.add(todo);
			notUsedNodes.remove(outputNode);
			notUsedNodes.removeAll(dependencies);
			notUsedOutputNodes.remove(outputNode);
		}

		// add unused nodes to the dependencies of a random to do in to dos
		for (Node node : notUsedNodes) {
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

	public static void main(String[] args) {
		Graph graph = generateGraph(2, 1);
		System.out.println(new MermaidGraphViz().visualize(graph));
		System.out.println(graph.hasCycle());
	}
}
