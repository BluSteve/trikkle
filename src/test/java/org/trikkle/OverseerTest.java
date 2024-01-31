package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.viz.IGraphViz;
import org.trikkle.viz.MermaidGraphViz;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OverseerTest {
	@Test
	void simpleTest() {
		Node inputNode = new DiscreteNode("toSquare");
		Arc arc = new Arc.AutoArc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = toSquare * toSquare;
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
		assertEquals(4.0, results.get("squared"));
	}

	@Test
	void complexTest() {
		Node inputNode2 = new DiscreteNode("finalMultiplier", "finalExponent");
		Node inputNode = new DiscreteNode("toSquare");
		Arc arc = new Arc.AutoArc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = toSquare * toSquare;
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

		Graph graph = new Graph(link, link2, link3);
		IGraphViz visualizer = new MermaidGraphViz();
		System.out.println(visualizer.visualize(graph));
		Overseer overseer = new Overseer(graph);

		overseer.addStartingDatum("toSquare", 2.0);
		overseer.addStartingDatum("finalMultiplier", 3.0);

		Exception e1 = assertThrows(IllegalArgumentException.class, () -> overseer.addStartingDatum("result2", 30));
		assertTrue(e1.getMessage().contains("Node of datumName \"result2\" does not belong to a starting node!"));

		Exception e = assertThrows(IllegalStateException.class, overseer::start);
		assertTrue(e.getMessage().contains("Starting nodes not fully populated; unable to start!"));

		overseer.addStartingDatum("finalExponent", 1.2);

		overseer.start();

		Map<String, Object> results = overseer.getResultCache();
		assertEquals(38.60674203230342, results.get("result2"));
	}

	@Test
	void streamTest() {
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
		assertEquals(45.0, results.get("result1"));
	}
}
