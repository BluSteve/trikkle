package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.viz.MermaidGraphViz;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ALL")
class GraphTest {
	static Node paramNode = DiscreteNode.of("param");
	static Node magicNode = DiscreteNode.of("magic");
	static Node hfNode = DiscreteNode.of("hf");
	static Node matrixNode = DiscreteNode.of("matrix");
	static Node dipoleNode = DiscreteNode.of("dipole");
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
		Node nodeA = DiscreteNode.of("A");
		Node nodeB = DiscreteNode.of("B");
		Node nodeC = DiscreteNode.of("C");
		Node nodeD = DiscreteNode.of("D");
		Node nodeE = DiscreteNode.of("E");
		Node nodeF = DiscreteNode.of("F");

		// make three empty arcs and set arc.name to their variable names
		Arc arc1 = new AutoArc() {
			@Override
			public void run() {

			}
		};
		arc1.setName("arc1");
		Arc arc2 = new AutoArc() {
			@Override
			public void run() {

			}
		};
		arc2.setName("arc2");
		Arc arc3 = new AutoArc() {
			@Override
			public void run() {

			}
		};
		arc3.setName("arc3");


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

		Node nodeG = DiscreteNode.of("G");
		Node nodeH = DiscreteNode.of("H");
		Arc arc4 = new AutoArc() {
			@Override
			public void run() {

			}
		};
		arc4.setName("arc4");
		Arc arc5 = new AutoArc() {
			@Override
			public void run() {

			}
		};
		arc5.setName("arc5");
		Link link4 = new Link(Set.of(nodeD), arc4, nodeG);
		Node nodeI = DiscreteNode.of("I");
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
	void mergeGraphs5() {
		Node ieNode = DiscreteNode.of("ie");
		Arc arc = GraphGenerator.generateArcs(1).get(0);
		Link link = new Link(Set.of(dipoleNode), arc, ieNode);
		Graph e1 = new Graph(link2, link3, link);
		Graph mergedGraph = Graph.mergeGraphs(List.of(graph1, e1), Set.of(hfNode, ieNode));
		assertEquals(mergedGraph, e1);
	}

	@Test
	void softEquals() {
		// generate 4 nodes, with two having the same datumNames
		Node nodeA = DiscreteNode.of("A");
		Node nodeC1 = DiscreteNode.of("C", "a");
		Node nodeC2 = DiscreteNode.of("a", "C");
		Node nodeD = DiscreteNode.of("D");

		// generate A to C1 and C2 to D arcs and links
		Arc arc1 = new AutoArc() {
			@Override
			public void run() {

			}
		};
		arc1.setName("arc1");
		Arc arc2 = new AutoArc() {
			@Override
			public void run() {

			}
		};
		arc2.setName("arc2");
		Link link1 = new Link(Set.of(nodeA), arc1, nodeC1);
		Link link2 = new Link(Set.of(nodeC2), arc2, nodeD);


		// generate a graph with links 1 and 2
		Graph graph = new Graph(link1, link2);

		// generate a graph with just link1 and another with just link2
		Graph graph1 = new Graph(link1);
		Graph graph2 = new Graph(link2);

		// make link1a with the same nodes but a new, different arc from link1
		Arc arc1a = new AutoArc() {
			@Override
			public void run() {

			}
		};
		arc1a.setName("arc1a");
		Link link1A = new Link(Set.of(nodeA), arc1a, nodeC1);
		Graph graph1a = new Graph(link1A, link2);

		assertFalse(graph.equals(graph1a));
		assertTrue(graph.congruentTo(graph1a));
	}

	@Test
	void twoNodesSameName() {
		// make a new node with the same name as paramNode
		Node paramNode2 = DiscreteNode.of("param", "smth else");
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
