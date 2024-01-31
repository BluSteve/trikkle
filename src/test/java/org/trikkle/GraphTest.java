package org.trikkle;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ALL")
class GraphTest {
	static Node paramNode = new DiscreteNode("param");
	static Node magicNode = new DiscreteNode("magic");
	static Node hfNode = new DiscreteNode("hf");
	static Node matrixNode = new DiscreteNode("matrix");
	static Node dipoleNode = new DiscreteNode("dipole");
	static List<Arc> arcs = GraphGenerator.generateArcs(4);
	static Link link1 = new Link(Set.of(magicNode, paramNode), arcs.get(0), hfNode);
	static Graph graph1 = new Graph(link1);
	static Link link2 = new Link(Set.of(matrixNode), arcs.get(1), hfNode);
	static Link link3 = new Link(Set.of(matrixNode), arcs.get(2), dipoleNode);
	static Graph graph2 = new Graph(link2, link3);
	static Link link4 = new Link(Set.of(paramNode), arcs.get(3), dipoleNode);

	// learn from ExampleFunctions.java

	@Test
	void twoArcsSameNode() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			new Graph(Set.of(
					new Link(Set.of(paramNode), arcs.get(0), magicNode),
					new Link(Set.of(paramNode), arcs.get(1), magicNode)
			));
		});
		assertTrue(exception.getMessage().contains("Two Arcs cannot point to the same output Node!"));
	}

	@Test
	void twoLinksSameArc() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			new Graph(Set.of(
					new Link(Set.of(paramNode), arcs.get(0), magicNode),
					new Link(Set.of(magicNode), arcs.get(0), hfNode)
			));
		});
		assertTrue(exception.getMessage().contains("The same Arc cannot be used for two Links!"));
	}

	@Test
	void hasCycle() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			new Graph(Set.of(
					new Link(Set.of(paramNode), arcs.get(0), magicNode),
					new Link(Set.of(magicNode), arcs.get(1), hfNode),
					new Link(Set.of(hfNode), arcs.get(2), paramNode)
			));
		});
		assertTrue(exception.getMessage().contains("Graph has a cycle!"));
	}

	@Test
	void concatGraphs() {
		// make nodes with datumNames A to F
		Node nodeA = new DiscreteNode("A");
		Node nodeB = new DiscreteNode("B");
		Node nodeC = new DiscreteNode("C");
		Node nodeD = new DiscreteNode("D");
		Node nodeE = new DiscreteNode("E");
		Node nodeF = new DiscreteNode("F");

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


		// make a link connecting nodeB to nodeF with arc1
		Link link1 = new Link(Set.of(nodeB), arc1, nodeF);

		// make a link connecting node A and B to node C with arc2
		Link link2 = new Link(Set.of(nodeA, nodeB), arc2, nodeC);

		// make a link connecting node C and E to node D with arc3
		Link link3 = new Link(Set.of(nodeC, nodeE), arc3, nodeD);

		// make a graph with links 1 and 2
		Graph graph1 = new Graph(link1, link2);
		// make a graph with link3
		Graph graph2 = new Graph(link3);

		Node nodeG = new DiscreteNode("G");
		Node nodeH = new DiscreteNode("H");
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
		Link link4 = new Link(Set.of(nodeD), arc4, nodeG);
		Node nodeI = new DiscreteNode("I");
		Link link5 = new Link(Set.of(nodeI), arc5, nodeH);

		Graph graph4 = new Graph(link4, link5);

		Graph graph3 = Graph.concatGraphs(graph1, graph2, graph4);

		Graph manualGraph = new Graph(Set.of(
				new Link(Set.of(nodeB), arc1, nodeF),
				new Link(Set.of(nodeA, nodeB), arc2, nodeC),
				new Link(Set.of(nodeC, nodeE), arc3, nodeD),
				new Link(Set.of(nodeD), arc4, nodeG),
				new Link(Set.of(nodeI), arc5, nodeH)
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
				new Link(Set.of(matrixNode), arcs.get(2), hfNode),
				new Link(Set.of(matrixNode), arcs.get(3), dipoleNode)
		));
		Graph graph3 = new Graph(link4);
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

		Graph manualGraph = new Graph(link3);
		assertEquals(manualGraph, mergedGraph);
	}

	@Test
	void mergeGraphs3() {
		Graph mergedGraph = Graph.mergeGraphs(List.of(graph1, graph2), Set.of(hfNode, dipoleNode));
		assertEquals(graph2, mergedGraph);
	}

	@Test
	void mergeGraphs4() {
		Graph e1 = new Graph(link1, link4);
		Graph mergedGraph = Graph.mergeGraphs(List.of(e1, graph2), Set.of(hfNode, dipoleNode));
		assertEquals(e1, mergedGraph);
	}
}
