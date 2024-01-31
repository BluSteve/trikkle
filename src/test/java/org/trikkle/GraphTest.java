package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.viz.MermaidGraphViz;

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

	@Test
	void softEquals() {
		// generate 4 nodes, with two having the same datumNames
		Node nodeA = new DiscreteNode("A");
		Node nodeC1 = new DiscreteNode("C", "a");
		Node nodeC2 = new DiscreteNode("a", "C");
		Node nodeD = new DiscreteNode("D");

		// generate A to C1 and C2 to D arcs and links
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
		Link link1 = new Link(Set.of(nodeA), arc1, nodeC1);
		Link link2 = new Link(Set.of(nodeC2), arc2, nodeD);


		// generate a graph with links 1 and 2
		Graph graph = new Graph(link1, link2);

		// generate a graph with just link1 and another with just link2
		Graph graph1 = new Graph(link1);
		Graph graph2 = new Graph(link2);

		// make link1a with the same nodes but a new, different arc from link1
		Arc arc1a = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};
		arc1a.name = "arc1a";
		Link link1A = new Link(Set.of(nodeA), arc1a, nodeC1);
		Graph graph1a = new Graph(link1A, link2);

		assertFalse(graph.equals(graph1a));
		assertTrue(graph.congruentTo(graph1a));
	}

	@Test
	void twoNodesSameName() {
		// make a new node with the same name as paramNode
		Node paramNode2 = new DiscreteNode("param", "smth else");
		// add two different nodes but with the same datum name into a graph. should throw an error
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			new Graph(Set.of(
					new Link(Set.of(paramNode), arcs.get(0), magicNode),
					new Link(Set.of(paramNode2), arcs.get(1), matrixNode)
			));
		});

		assertTrue(exception.getMessage().contains("Two Nodes cannot have the same datum name!"));
	}

	@Test
	void visualize() {
		assertEquals(MermaidGraphViz.defaultVisualize(graph1), graph1.toString());
		assertEquals(MermaidGraphViz.defaultVisualize(graph1, graph2),
				new MermaidGraphViz().visualize(graph1, graph2));
		String s = new MermaidGraphViz("ns").visualize(graph1, graph2);
		System.out.println(s);
		assertTrue(s.contains("ns_graph1_node1"));
	}

	@Test
	void bigTest() {
		Graph graph = GraphGenerator.generateGraph(100, 1);
	}

	@Test
	void prunedGraphs() {
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			graph1.findPrunedGraphFor(Set.of(paramNode));
		});
		assertTrue(e.getMessage().contains("Graph must have at least one Link!"));

		Exception e1 = assertThrows(IllegalArgumentException.class, () -> {
			graph1.findPrunedGraphFor(Set.of(paramNode, matrixNode));
		});
		assertTrue(e1.getMessage().contains("targetNodes must be a subset of the Graph's nodes!"));

		Graph graph = graph1.findPrunedGraphFor(Set.of(hfNode));
		Graph graphcached = graph1.findPrunedGraphFor(Set.of(hfNode));
		assertSame(graph, graphcached);
	}
}
