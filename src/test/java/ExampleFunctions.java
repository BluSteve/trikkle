import org.trikkle.*;
import org.trikkle.viz.IGraphViz;
import org.trikkle.viz.MermaidGraphViz;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ExampleFunctions {
	static double simpleFn(double x) {
		return x * x;
	}

	static void simpleTest() {
		Node inputNode = new DiscreteNode("toSquare");
		Arc arc = new Arc.AutoArc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = simpleFn(toSquare);
				returnDatum("squared", squared);
			}
		};
		Node outputNode = new DiscreteNode("squared");
		Link link = new Link(Set.of(inputNode), arc, outputNode);


		Graph graph = new Graph(link);
		System.out.println(new MermaidGraphViz().visualize(graph));
		Overseer overseer = new Overseer(graph);

		inputNode.addDatum("toSquare", 2.0);
		overseer.start();


		Map<String, Object> results = overseer.getResultCache();
		System.out.println(results);
	}

	static void complexTest() {
		Node inputNode2 = new DiscreteNode("finalMultiplier", "finalExponent");
		Node inputNode = new DiscreteNode("toSquare");
		Arc arc = new Arc.AutoArc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = simpleFn(toSquare);
				returnDatum("squared", squared);
			}
		};
		arc.name = "squarer";
		Node node2 = new DiscreteNode("squared");
		Link link = new Link(Set.of(inputNode), arc, node2);


		Arc arc2 = new Arc.AutoArc() {
			@Override
			public void run() {
				double squared = (double) getDatum("squared");
				double toSquare = (double) getDatum("toSquare");
				double result = squared + 1.5 * toSquare;

				returnDatum("result1", result);
			}
		};
		arc2.name = "process 1";
		Node node3 = new DiscreteNode("result1");
		Link link2 = new Link(Set.of(inputNode, node2), arc2, node3);

		Arc arc3 = new Arc.AutoArc() {
			@Override
			public void run() {
				double result1 = (double) getDatum("result1");
				double finalMultiplier = (double) getDatum("finalMultiplier");
				double finalExponent = (double) getDatum("finalExponent");
				returnDatum("result2", Math.pow(result1 * finalMultiplier, finalExponent));
			}
		};
		arc3.name = "aggregator";
		Node node4 = new DiscreteNode("result2");
		Link link3 = new Link(Set.of(node3, inputNode2), arc3, node4);


		Arc phantomArc1 = new Arc.AutoArc() {
			@Override
			public void run() {
				returnDatum("toSquare", 2.0);
			}
		};
		phantomArc1.name = "phantomArc1";
		Link phantomLink1 = new Link(Set.of(), phantomArc1, inputNode);

		Arc phantomArc2 = new Arc.AutoArc() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				returnDatum("finalMultiplier", 3.0);
				returnDatum("finalExponent", 1.2);
			}
		};
		phantomArc2.name = "phantomArc2";
		Link phantomLink2 = new Link(Set.of(), phantomArc2, inputNode2);

//		inputNode.setUsable(true);
//		inputNode2.setUsable(true);
		Graph graph = new Graph(link, link2, link3);
		IGraphViz visualizer = new MermaidGraphViz();
		System.out.println(visualizer.visualize(graph));
		Overseer overseer = new Overseer(graph);

		inputNode.addDatum("toSquare", 2.0);
		inputNode2.addDatum("finalMultiplier", 3.0);
		inputNode2.addDatum("finalExponent", 1.2);

		overseer.start();


		Map<String, Object> results = overseer.getResultCache();
		System.out.println(results);
	}

	static void streamTest() {
		Arc inputArc = new Arc.AutoArc() {
			@Override
			public void run() {
				for (int i = 1; i < 10; i++) {
					returnDatum("stream1", (double) i);
					outputNode.setProgress(i / 10.0);
				}

				outputNode.setProgress(1);
			}
		};
		Node streamNode = new StreamNode(Set.of("stream1"));
		Link link = new Link(Set.of(), inputArc, streamNode);

		Arc consumerArc = new Arc() {
			double total = 0; // is this a pure function?

			@Override
			public void run() {
				Queue<Double> queue = (Queue<Double>) getDatum("stream1");
				Node stream1Node = overseer.getOutputNodeOfDatum("stream1");

				double sum = 0;
				synchronized (queue) {
					if (queue.size() >= 3) {
						this.status = ArcStatus.IN_PROGRESS;
						while (!queue.isEmpty()) {
							sum += queue.poll();
						}

						total += sum;
						// prevents stream1Node from calling this arc again when another node ticktock().
						stream1Node.setUsable(false);
					}
				}

				System.out.println("total = " + total);

				if (stream1Node.getProgress() == 1) {
					stream1Node.setUsable(false);
					this.status = ArcStatus.FINISHED;
					returnDatum("result1", total); // this must be the last line as it's a recursive call
				}
				else this.status = ArcStatus.IDLE;
			}
		};
		Node outputNode = new DiscreteNode("result1");
		Link link2 = new Link(Set.of(streamNode), consumerArc, outputNode);

		Graph graph = new Graph(link, link2);
		Overseer overseer = new Overseer(graph);
		overseer.start();

		Map<String, Object> results = overseer.getResultCache();
		System.out.println(results);
	}

	static void mergeTest() {
		Node paramNode = new DiscreteNode("param");
		Node magicNode = new DiscreteNode("magic");
		Node hfNode = new DiscreteNode("hf");
		Node matrixNode = new DiscreteNode("matrix");
		Node dipoleNode = new DiscreteNode("dipole");

		Arc arc1 = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};
		Arc arc2 = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};
		Arc arc3 = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};
		Arc arc4 = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};

		arc1.name = "arc1";
		arc2.name = "arc2";
		arc3.name = "arc3";
		arc4.name = "arc4";

		Link link1 = new Link(Set.of(magicNode, paramNode), arc1, hfNode);
		Link link4 = new Link(Set.of(paramNode), arc4, dipoleNode);
		Link link2 = new Link(Set.of(matrixNode), arc2, hfNode);
		Link link3 = new Link(Set.of(matrixNode), arc3, dipoleNode);

		Graph graph1 = new Graph(link1);
		Graph graph2 = new Graph(link2, link3);
		Graph graph3 = Graph.mergeGraphs(List.of(graph1, graph2), Set.of(hfNode, dipoleNode));

		// visualize all three graphs with the graph variable name printed before the graph
		System.out.println(new MermaidGraphViz().visualize(graph1, graph2));
		System.out.println(new MermaidGraphViz().visualize(graph3));
	}

	static void concatTest() {
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

		// display graph 1, 2, 4 with labels printed for each and namespace set to the labels
		System.out.println(new MermaidGraphViz().visualize(graph1, graph2, graph4));

		Graph graph3 = Graph.concatGraphs(graph1, graph2, graph4).findPrunedGraphFor(Set.of(nodeG));
		System.out.println("graph3");
		System.out.println(new MermaidGraphViz("graph3").visualize(graph3));
	}

	static void softEqualsTest() {
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

		// visualize
		System.out.println(new MermaidGraphViz().visualize(graph));

		// generate a graph with just link1 and another with just link2
		Graph graph1 = new Graph(link1);
		Graph graph2 = new Graph(link2);
		// concat
		Graph graph3 = Graph.concatGraphs(graph1, graph2);
		// visualize

		System.out.println(new MermaidGraphViz().visualize(graph3));
		System.out.println("graph3.hasCycle() = " + graph3.hasCycle());

		// make link1a with the same nodes but a new, different arc from link1
		Arc arc1a = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};
		arc1a.name = "arc1a";
		Link link1A = new Link(Set.of(nodeA), arc1a, nodeC1);
		Graph graph1a = new Graph(link1A, link2);
		System.out.println(graph.equals(graph1a));
		System.out.println(graph.congruentTo(graph1a));
	}

	public static void main(String[] args) {
		// run all test
		complexTest();
	}
}
