package org.trikkle;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ALL")
class GraphTest {
	static Node paramNode = new DiscreteNode(Set.of("param"));
	static Node magicNode = new DiscreteNode(Set.of("magic"));
	static Node hfNode = new DiscreteNode(Set.of("hf"));
	static Node matrixNode = new DiscreteNode(Set.of("matrix"));
	static Node dipoleNode = new DiscreteNode(Set.of("dipole"));
	static List<Arc> arcs = GraphGenerator.generateArcs(4);
	static Todo todo1 = new Todo(Set.of(magicNode, paramNode), arcs.get(0), hfNode);
	static Graph graph1 = new Graph(Set.of(todo1));
	static Todo todo2 = new Todo(Set.of(matrixNode), arcs.get(1), hfNode);
	static Todo todo3 = new Todo(Set.of(matrixNode), arcs.get(2), dipoleNode);
	static Graph graph2 = new Graph(Set.of(todo2, todo3));
	static Todo todo4 = new Todo(Set.of(paramNode), arcs.get(3), dipoleNode);

	// learn from ExampleFunctions.java

	@Test
	void twoArcsSameNode() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			new Graph(Set.of(
					new Todo(Set.of(paramNode), arcs.get(0), magicNode),
					new Todo(Set.of(paramNode), arcs.get(1), magicNode)
			));
		});
		assertTrue(exception.getMessage().contains("Two Arcs cannot point to the same output Node!"));
	}

	@Test
	void twoTodosSameArc() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			new Graph(Set.of(
					new Todo(Set.of(paramNode), arcs.get(0), magicNode),
					new Todo(Set.of(magicNode), arcs.get(0), hfNode)
			));
		});
		assertTrue(exception.getMessage().contains("The same Arc cannot be used for two Todos!"));
	}

	@Test
	void hasCycle() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			new Graph(Set.of(
					new Todo(Set.of(paramNode), arcs.get(0), magicNode),
					new Todo(Set.of(magicNode), arcs.get(1), hfNode),
					new Todo(Set.of(hfNode), arcs.get(2), paramNode)
			));
		});
		assertTrue(exception.getMessage().contains("Graph has a cycle!"));
	}

	@Test
	void concatGraphs() {
		// make nodes with datumNames A to F
		Node nodeA = new DiscreteNode(Set.of("A"));
		Node nodeB = new DiscreteNode(Set.of("B"));
		Node nodeC = new DiscreteNode(Set.of("C"));
		Node nodeD = new DiscreteNode(Set.of("D"));
		Node nodeE = new DiscreteNode(Set.of("E"));
		Node nodeF = new DiscreteNode(Set.of("F"));

		// make three empty arcs and set arc.name to their variable names
		Arc arc1 = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};
		arc1.name = "arc1";
		Arc arc2 = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};
		arc2.name = "arc2";
		Arc arc3 = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};
		arc3.name = "arc3";


		// make a to do connecting nodeB to nodeF with arc1
		Todo todo1 = new Todo(Set.of(nodeB), arc1, nodeF);

		// make a to do connecting node A and B to node C with arc2
		Todo todo2 = new Todo(Set.of(nodeA, nodeB), arc2, nodeC);

		// make a to do connecting node C and E to node D with arc3
		Todo todo3 = new Todo(Set.of(nodeC, nodeE), arc3, nodeD);

		// make a graph with todos 1 and 2
		Graph graph1 = new Graph(Set.of(todo1, todo2));
		// make a graph with todo3
		Graph graph2 = new Graph(Set.of(todo3));

		Node nodeG = new DiscreteNode(Set.of("G"));
		Node nodeH = new DiscreteNode(Set.of("H"));
		Arc arc4 = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};
		arc4.name = "arc4";
		Arc arc5 = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};
		arc5.name = "arc5";
		Todo todo4 = new Todo(Set.of(nodeD), arc4, nodeG);
		Node nodeI = new DiscreteNode(Set.of("I"));
		Todo todo5 = new Todo(Set.of(nodeI), arc5, nodeH);

		Graph graph4 = new Graph(Set.of(todo4, todo5));

		Graph graph3 = Graph.concatGraphs(List.of(graph1, graph2, graph4));

		Graph manualGraph = new Graph(Set.of(
				new Todo(Set.of(nodeB), arc1, nodeF),
				new Todo(Set.of(nodeA, nodeB), arc2, nodeC),
				new Todo(Set.of(nodeC, nodeE), arc3, nodeD),
				new Todo(Set.of(nodeD), arc4, nodeG),
				new Todo(Set.of(nodeI), arc5, nodeH)
		));

		// visualize graph3 and manualgraph
//		MermaidGraphViz mermaidGraphViz = new MermaidGraphViz();
//		System.out.println(mermaidGraphViz.visualize(graph3));
//		System.out.println(mermaidGraphViz.visualize(manualGraph));

		assertEquals(manualGraph, graph3);
	}

	@Test
	void congruentTo() {
		Graph graph = new Graph(Set.of(
				new Todo(Set.of(matrixNode), arcs.get(2), hfNode),
				new Todo(Set.of(matrixNode), arcs.get(3), dipoleNode)
		));
		Graph graph3 = new Graph(Set.of(todo4));
		assertFalse(graph3.congruentTo(graph1));
		assertFalse(graph.congruentTo(graph1));
		assertTrue(graph.congruentTo(graph2));
	}

	@Test
	void mergeGraphs1() {
		Graph mergedGraph = Graph.mergeGraphs(List.of(graph1, graph2), Set.of(hfNode));
		assertEquals(graph1, mergedGraph);
	}

	@Test
	void mergeGraphs2() {
		Graph mergedGraph = Graph.mergeGraphs(List.of(graph1, graph2), Set.of(dipoleNode));

		Graph manualGraph = new Graph(Set.of(todo3));
		assertEquals(manualGraph, mergedGraph);
	}

	@Test
	void mergeGraphs3() {
		Graph mergedGraph = Graph.mergeGraphs(List.of(graph1, graph2), Set.of(hfNode, dipoleNode));
		assertEquals(graph2, mergedGraph);
	}

	@Test
	void mergeGraphs4() {
		Graph e1 = new Graph(Set.of(todo1, todo4));
		Graph mergedGraph = Graph.mergeGraphs(List.of(e1, graph2), Set.of(hfNode, dipoleNode));
		assertEquals(e1, mergedGraph);
	}
}
