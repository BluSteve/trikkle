package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.annotations.Input;
import org.trikkle.annotations.Output;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EasyArcTest {
	@Test
	void testEasyArc() {
		Arc arc = new AutoArc() {
			@Input
			double input1, input1a;
			@Input
			String input2;
			@Output
			int output;

			@Override
			public void run() {
				output = (int) (input1 + input1a) + input2.length();
				System.out.println("output = " + output);
			}
		};
		Link link = new Link(arc);

		Nodespace nodespace = new Nodespace();
		nodespace.addAll(link.getDependencies());
		nodespace.addAll(link.getOutputNodes());
		System.out.println(nodespace.nodeStore);

		Graph graph = new Graph(link);
		Overseer overseer = new Overseer(graph);
		overseer.addStartingDatum("input1", 2.5);
		overseer.addStartingDatum("input2", "hello");
		overseer.addStartingDatum("input1a", 3.6);
		overseer.start();

		assertEquals(11, overseer.getResultCache().get("output"));

		Overseer overseer2 = new Overseer(graph);
		overseer2.resetGraph();
		overseer2.addStartingDatum("input1", 2.5);
		overseer2.addStartingDatum("input2", "helloo");
		overseer2.addStartingDatum("input1a", 0);
		overseer2.start();

		assertEquals(8, overseer2.getResultCache().get("output"));
	}

	@Test
	void testEasyArc2() {
		Arc arc = new AutoArc("sumdiff") {
			@Input
			double a, b;
			@Output
			double sum, diff;

			@Override
			public void run() {
				sum = a + b;
				diff = a - b;
			}
		};
		Node a = new DiscreteNode("a");
		Node b = new DiscreteNode("b");
		Node sum = new DiscreteNode("sum");
		Node diff = new DiscreteNode("diff");
		Link link = new Link(Set.of(a, b), arc, Set.of(sum, diff));

		Graph graph = new Graph(link);
		System.out.println(graph);
		Overseer overseer = new Overseer(graph);
		overseer.addStartingDatum("a", 2.5);
		overseer.addStartingDatum("b", 3.6);
		overseer.start();

		assertEquals(6.1, overseer.getResultCache().get("sum"));
		assertEquals(-1.1, overseer.getResultCache().get("diff"));
	}

	@Test
	void testEmptyEasyArc() {
		AtomicBoolean ran = new AtomicBoolean(false);
		Arc arc = new AutoArc() {
			@Override
			public void run() {
				getOutputNode().setUsable();
				ran.set(true);
			}
		};

		Link link = new Link(arc);
		Graph graph = new Graph(link);

		Overseer overseer = new Overseer(graph);
		link.getDependencies().iterator().next().setUsable();
		overseer.start();

		assertTrue(ran.get());
	}

	@Test
	void halfLinkTest() {
		Arc arc = new AutoArc("getyz") {
			@Input
			double x;
			@Output
			double y, z;

			@Override
			public void run() {
				y = x * 2;
				z = x * 3;
			}
		};

		Node y = new DiscreteNode("y");
		Node z = new DiscreteNode("z");
		HalfLink halfLink = new HalfLink(arc, Set.of(y, z));

		Arc arc2 = new AutoArc("printy") {
			@Input
			double y;

			@Override
			public void run() {
				System.out.println("y = " + y);
			}
		};

		HalfLink halfLink2 = new HalfLink(arc2, Set.of());

		List<Link> links = HalfLink.toFullLinks(List.of(halfLink, halfLink2));

		System.out.println(links);

		Graph graph = new Graph(links);
		Overseer overseer = new Overseer(graph);
		overseer.addStartingDatum("x", 2.5);
		overseer.start();

		assertEquals(5.0, overseer.getDatum("y"));
		assertEquals(7.5, overseer.getDatum("z"));
	}

	@Test
	void testStream() {
		Arc inputArc = new AutoArc() {
			@Override
			public void run() {
				for (int i = 1; i < 10; i++) {
					returnDatum("stream1", (double) i);
					getOutputNode().setProgress(i / 10.0);
				}

				getOutputNode().setProgress(1);
			}
		};
		Node streamNode = new StreamNode("stream1");
		Link link = new Link(Set.of(), inputArc, streamNode);

		Arc consumerArc = new Arc(false) {
			double total = 0; // is this a pure function? it is if you reset()
			@Input(name = "stream1")
			Queue<Double> queue;
			@Output
			double result1;

			@Override
			public void run() {
				Node stream1Node = overseer.getNodeOfDatum("stream1");

				double sum = 0;
				synchronized (queue) {
					if (queue.size() >= 3) {
						while (!queue.isEmpty()) {
							sum += queue.poll();
						}

						total += sum;
						// prevents stream1Node from calling this arc again when another node ticktock().
						stream1Node.setUsable();
					}
				}

				System.out.println("total = " + total);

				if (stream1Node.getProgress() == 1) {
					stream1Node.setUsable();
					this.setStatus(ArcStatus.FINISHED);
					result1 = total;
				} else {
					this.setStatus(ArcStatus.IDLE);
				}
			}

			@Override
			public void reset() {
				super.reset();
				total = 0;
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
