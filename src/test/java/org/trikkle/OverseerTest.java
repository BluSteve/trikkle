package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.viz.IGraphViz;
import org.trikkle.viz.MermaidGraphViz;

import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class OverseerTest {
	@Test
	void nodeTest() {
		Node node = DiscreteNode.of("toSquare");
		node.setProgress(1);

		Exception e = assertThrows(IllegalArgumentException.class, () -> node.addDatum("toSquare2", 3.0));
		assertTrue(e.getMessage().contains("Datum toSquare2 was not declared by this node!"));

		Exception e1 = assertThrows(IllegalArgumentException.class, () -> node.setProgress(2));
		assertTrue(e1.getMessage().contains("Progress 2.0 not between 0 and 1!"));

		assertFalse(node.setProgress(0.5));
	}

	@Test
	void simpleTest() {
		Nodespace ns = new Nodespace();
		Node inputNode = ns.discreteOf("toSquare");
		assertNotSame(inputNode, DiscreteNode.of("toSquare"));
		assertNotEquals(inputNode, DiscreteNode.of("toSquare"));
		assertTrue(inputNode.congruentTo(DiscreteNode.of("toSquare")));

		Arc arc = new AutoArc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = toSquare * toSquare;
				returnDatum("squared", squared);
			}
		};
		Node outputNode = ns.discreteOf("squared");
		Link link = new Link(Set.of(inputNode), arc, outputNode);

		Graph graph = new Graph(link);
		System.out.println(new MermaidGraphViz().visualize(graph));
		Overseer overseer = new Overseer(graph);
		assertSame(graph, overseer.getGraph());

		inputNode.addDatum("toSquare", 2.0);
		overseer.start();

		assertEquals(overseer.getCacheCopy(), overseer.getCache());
		assertNotSame(overseer.getCacheCopy(), overseer.getCache());
		Map<String, Object> results = overseer.getResultCache();
		assertEquals(4.0, results.get("squared"));
	}

	@Test
	void complexTest() {
		Node inputNode2 = DiscreteNode.of("finalMultiplier", "finalExponent");
		Node inputNode = DiscreteNode.of("toSquare");
		Arc arc = new AutoArc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = toSquare * toSquare;
				returnDatum("squared", squared);
			}
		};
		arc.setName("squarer");
		Node node2 = DiscreteNode.of("squared");
		Link link = new Link(Set.of(inputNode), arc, node2);

		Arc arc2 = new AutoArc() {
			@Override
			public void run() {
				double squared = (double) getDatum("squared");
				double toSquare = (double) getDatum("toSquare");
				double result = squared + 1.5 * toSquare;

				returnDatum("result1", result);
			}
		};
		arc2.setName("process 1");
		Node node3 = DiscreteNode.of("result1");
		Link link2 = new Link(Set.of(inputNode, node2), arc2, node3);

		Arc arc3 = new AutoArc() {
			@Override
			public void run() {
				double result1 = (double) getDatum("result1");
				double finalMultiplier = (double) getDatum("finalMultiplier");
				double finalExponent = (double) getDatum("finalExponent");
				returnDatum("result2", Math.pow(result1 * finalMultiplier, finalExponent));
			}
		};
		arc3.setName("aggregator");
		Node node4 = DiscreteNode.of("result2");
		Link link3 = new Link(Set.of(node3, inputNode2), arc3, node4);

		Graph graph = new Graph(link, link2, link3);
		IGraphViz visualizer = new MermaidGraphViz();
		System.out.println(visualizer.visualize(graph));
		Overseer overseer = new Overseer(graph);

		assertEquals(overseer.getStartingDatumNames(), Set.of("toSquare", "finalMultiplier", "finalExponent"));

		overseer.addStartingDatum("toSquare", 2.0);
		overseer.addStartingDatum("finalMultiplier", 3.0);

		Exception e1 = assertThrows(IllegalArgumentException.class, () -> overseer.addStartingDatum("result2", 30));
		assertTrue(e1.getMessage().contains("Datum result2 does not belong to a starting node!"));

		Exception e = assertThrows(IllegalStateException.class, overseer::start);
		assertTrue(e.getMessage().contains("Starting nodes not fully populated; unable to start!"));

		overseer.addStartingDatum("finalExponent", 1.2);

		overseer.start();

		// check that all arcs in the graph are finished
		for (Link link1 : graph.links) {
			assertEquals(ArcStatus.FINISHED, link1.getArc().getStatus());
		}

		Map<String, Object> results = overseer.getResultCache();
		assertEquals(38.60674203230342, results.get("result2"));
	}

	@Test
	void streamTest() {
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
		Node streamNode = StreamNode.of("stream1");
		Link link = new Link(Set.of(), inputArc, streamNode);

		Arc consumerArc = new Arc() {
			double total = 0; // is this a pure function? it is if you reset()

			@Override
			public void run() {
				Queue<Double> queue = (Queue<Double>) getDatum("stream1");
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
					returnDatum("result1", total); // this must be the last line as it's a recursive call
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
		Node outputNode = DiscreteNode.of("result1");
		Link link2 = new Link(Set.of(streamNode), consumerArc, outputNode);

		Graph graph = new Graph(link, link2);
		Overseer overseer = new Overseer(graph);
		overseer.start();

		Map<String, Object> results = overseer.getResultCache();
		assertEquals(45.0, results.get("result1"));

		overseer = new Overseer(graph);
		overseer.start();

		assertEquals(45.0, overseer.getDatum("result1"));
	}

	@Test
	void multipleArcs() {
		StreamNode streamNode = StreamNode.of("numberStream");
		streamNode.setLimit(18);
		Arc inputArc = new AutoArc() {
			@Override
			public void run() {
				for (int i = 1; i < 10; i++) {
					returnDatum("numberStream", (double) i);
				}
			}
		};
		Arc inputArc2 = new AutoArc() {
			@Override
			public void run() {
				for (int i = 101; i < 110; i++) {
					returnDatum("numberStream", (double) i);
				}
			}
		};
		Link link = new Link(Set.of(), inputArc, streamNode);
		Link link2 = new Link(Set.of(), inputArc2, streamNode);
		Graph graph = new Graph(link, link2);
		System.out.println(graph);

		Overseer overseer = new Overseer(graph);
		overseer.start();

		System.out.println(overseer.getDatum("numberStream"));
		assertEquals(18, ((Queue<Double>) overseer.getDatum("numberStream")).size());
		assertEquals(streamNode.getProgress(), 1);
	}

	@Test
	void stressTest() {
		StreamNode streamNode = StreamNode.of("numberStream");
		streamNode.setLimit(1000);
		Set<Link> links = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			Arc inputArc = new AutoArc() {
				@Override
				public void run() {
					for (int i = 1; i <= 10; i++) {
						returnDatum("numberStream", (double) i);
					}
				}
			};
			Link link = new Link(Set.of(), inputArc, streamNode);
			links.add(link);
		}
		Graph graph = new Graph(links);
		System.out.println(graph);

		Set<RecursiveAction> actions = new HashSet<>();
		for (int i = 0; i < 2; i++) {
			actions.add(new RecursiveAction() {
				@Override
				protected void compute() {
					Overseer overseer = new Overseer(graph);
					overseer.start();
					assertEquals(1000, ((Queue<Double>) overseer.getDatum("numberStream")).size());
				}
			});
		}
		ForkJoinTask.invokeAll(actions);

		assertEquals(streamNode.getProgress(), 1);
	}

	@Test
	void manyNodes() {
		Set<Node> dependencies = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			dependencies.add(new Nodespace().discreteOf());
		}
		Arc inputArc = new AutoArc() {
			@Override
			public void run() {
				getOutputNode().setProgress(1);
			}
		};
		Link link = new Link(dependencies, inputArc, DiscreteNode.of());

		Graph graph = new Graph(link);
		System.out.println(graph);

		Overseer overseer = new Overseer(graph);
		for (Node dependency : dependencies) {
			dependency.setUsable();
		}
		overseer.start();
	}

	@Test
	void multipleArcsDiscrete() {
		Node discreteNode = DiscreteNode.of("res1", "res2");
		Arc inputArc = new AutoArc() {
			@Override
			public void run() {
				returnDatum("res1", 1.0);
			}
		};
		Arc inputArc2 = new AutoArc() {
			@Override
			public void run() {
				returnDatum("res2", 2.0);
			}
		};
		Link link = new Link(Set.of(), inputArc, discreteNode);
		Link link2 = new Link(Set.of(), inputArc2, discreteNode);
		Graph graph = new Graph(link, link2);
		System.out.println(graph);

		Overseer overseer = new Overseer(graph);
		overseer.start();

		assertEquals(1.0, overseer.getDatum("res1"));
		assertEquals(2.0, overseer.getDatum("res2"));

		assertEquals(graph, graph.findPrunedGraphFor(discreteNode));
	}

	@Test
	void multipleOutputTest() {
		Arc inputArc = new AutoArc() {
			@Override
			public void run() {
				returnDatum("res1", 1.0);
				returnDatum("res2", 2.0);
				returnDatum("res2a", 3.0);
			}
		};
		Node discreteNode = DiscreteNode.of("res1");
		Node discreteNode2 = DiscreteNode.of("res2", "res2a");
		Link link = new Link(Set.of(), inputArc, Set.of(discreteNode, discreteNode2));
		Graph graph = new Graph(link);
		System.out.println(graph);

		Overseer overseer = new Overseer(graph);
		overseer.start();

		assertEquals(1.0, overseer.getDatum("res1"));
		assertEquals(2.0, overseer.getDatum("res2"));
		assertEquals(3.0, overseer.getDatum("res2a"));
	}

	@Test
	void noOutputTest() {
		AtomicBoolean ab = new AtomicBoolean(false);
		Arc inputArc = new AutoArc() {
			@Override
			public void run() {
				System.out.println("hello there! i'm done!");
				ab.set(true);
			}
		};
		Node discreteNode = DiscreteNode.of();
		Link link = new Link(Set.of(discreteNode), inputArc, Set.of());
		Graph graph = new Graph(link);
		System.out.println(graph);

		Overseer overseer = new Overseer(graph);
		discreteNode.setUsable();
		overseer.start();

		assertTrue(ab.get());
		assertTrue(overseer.getResultCache().isEmpty());
	}

	@Test
	void sameThread() {
		Arc foo = new AutoArc() {
			@Override
			public void run() {
				System.out.println("hello");
			}
		};

		Overseer overseer = new Overseer(new Graph(new Link(Set.of(), foo, Set.of())));
		try {
			new Thread(overseer::start).start();
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains("Overseer construction and start() must be called in the same " +
					"thread!"));
		}
	}
}
