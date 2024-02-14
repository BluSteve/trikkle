package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.viz.IGraphViz;
import org.trikkle.viz.LogUtils;
import org.trikkle.viz.MermaidGraphViz;

import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.trikkle.viz.LogUtils.animate;
import static org.trikkle.viz.LogUtils.toMarkdown;

class OverseerTest {
	public static void sleep(int milliseconds) {
		try {
			TimeUnit.MILLISECONDS.sleep(milliseconds);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void nodeTest() {
		Node node = new DiscreteNode("toSquare");
		Arc arc = new AutoArc() {
			@Override
			public void run() {
			}
		};
		Link link = new Link(Set.of(node), arc, new EmptyNode());
		Graph graph = new Graph(link);
		Overseer overseer = new Overseer(graph);
		node.addDatum("toSquare", 2.0);
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
		assertNotSame(inputNode, new DiscreteNode("toSquare"));
		assertNotEquals(inputNode, new DiscreteNode("toSquare"));
		assertTrue(inputNode.congruentTo(new DiscreteNode("toSquare")));

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
		Node inputNode2 = new DiscreteNode("finalMultiplier", "finalExponent");
		Node inputNode = new DiscreteNode("toSquare");
		Arc arc = new AutoArc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = toSquare * toSquare;
				returnDatum("squared", squared);
			}
		};
		arc.setName("squarer");
		Node node2 = new DiscreteNode("squared");
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
		Node node3 = new DiscreteNode("result1");
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
		Node node4 = new DiscreteNode("result2");
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
		Node streamNode = Nodespace.DEFAULT.streamOf("stream1");
		Link link = new Link(Set.of(), inputArc, streamNode);

		Arc consumerArc = new Arc(false) {
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
		Node outputNode = new DiscreteNode("result1");
		Link link2 = new Link(Set.of(streamNode), consumerArc, outputNode);

		Graph graph = new Graph(link, link2);
		Overseer overseer = new Overseer(graph);
		overseer.start();

		Map<String, Object> results = overseer.getResultCache();
		assertEquals(45.0, results.get("result1"));

		overseer = new Overseer(graph);
		overseer.resetGraph();
		overseer.start();

		assertEquals(45.0, overseer.getDatum("result1"));
	}

	@Test
	void multipleArcs() {
		StreamNode streamNode = Nodespace.DEFAULT.streamOf("numberStream");
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
		overseer.resetGraph();
		overseer.start();

		System.out.println(overseer.getDatum("numberStream"));
		assertEquals(18, ((Queue<Double>) overseer.getDatum("numberStream")).size());
		assertEquals(streamNode.getProgress(), 1);
	}

	@Test
	void stressTest() {
		StreamNode streamNode = Nodespace.DEFAULT.streamOf("numberStream");
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
					overseer.resetGraph();
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
			dependencies.add(new Nodespace().emptyOf());
		}
		Arc inputArc = new AutoArc() {
			@Override
			public void run() {
				getOutputNode().setProgress(1);
			}
		};
		Link link = new Link(dependencies, inputArc, Nodespace.DEFAULT.emptyOf());

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
		Node discreteNode = new DiscreteNode("res1", "res2");
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
		Node discreteNode = new DiscreteNode("res1");
		Node discreteNode2 = new DiscreteNode("res2", "res2a");
		Link link = new Link(Set.of(), inputArc, Set.of(discreteNode, discreteNode2));
		System.out.println(link);
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
		Node discreteNode = Nodespace.DEFAULT.emptyOf();
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

	@Test
	void IRcomparison() {
		Arc long1 = new AutoArc() {
			@Override
			public void run() {
				sleep(200);
			}
		};
		long1.setName("long1");

		Arc med1 = new AutoArc() {
			@Override
			public void run() {
				sleep(100);
				getOutputNode().setUsable();
			}
		};
		med1.setName("med1");

		Arc med2 = new AutoArc() {
			@Override
			public void run() {
				sleep(100);
			}
		};
		med2.setName("med2");

		Node medNode = Nodespace.DEFAULT.emptyOf();

		Link link = new Link(Set.of(), long1, Set.of());
		Link link2 = new Link(Set.of(), med1, medNode);
		Link link3 = new Link(Set.of(medNode), med2, Set.of());

		Graph graph = new Graph(link, link2, link3);
		Overseer overseer = new Overseer(graph);
		overseer.setParallelThreshold(2);
		long start = System.currentTimeMillis();
		overseer.start();
		long end = System.currentTimeMillis();
		System.out.println("parallel: " + (end - start));
		assertTrue(end - start < 300);

		Overseer overseer2 = new Overseer(graph);
		overseer2.resetGraph();
		overseer2.setParallel(false);
		long start2 = System.currentTimeMillis();
		overseer2.start();
		long end2 = System.currentTimeMillis();
		System.out.println("sequential: " + (end2 - start2));
		assertTrue(end2 - start2 > 300);

		System.out.println(LogUtils.linkTraceToString(overseer2.getLinkTrace()));
	}

	@Test
	void unsafeTest() {
		Arc unsafeArc = new Arc(false) {
			@Override
			public void run() {
				setStatus(ArcStatus.FINISHED); // set to idle and make sure there's no stackoverflow
			}
		};

		Graph graph = new Graph(new Link(Set.of(), unsafeArc, Set.of()));
		Overseer overseer = new Overseer(graph);
		overseer.start();
	}

	@Test
	void economyTest() {
		Set<Link> manyLinks = new HashSet<>();
		for (int i = 0; i < 10000; i++) {
			int finalI = i;
			Arc arc = new AutoArc() {
				@Override
				public void run() {
					getOutputNode().setUsable();
				}
			};
			manyLinks.add(new Link(Set.of(new Nodespace().emptyOf()), arc, new Nodespace().emptyOf()));
		}
		Overseer overseer = new Overseer(new Graph(manyLinks));
		for (Node startingNode : overseer.getGraph().startingNodes) {
			startingNode.setUsable();
		}
		overseer.setParallel(false);
		overseer.start();
		assertEquals(1, overseer.getTick());
	}

	@Test
	void animationTest() {
		// make three arcs that output to one node each with different datum names
		Arc arc1 = new AutoArc() {
			@Override
			public void run() {
				returnDatum("node1", 1.0);
			}
		};
		Arc arc2 = new AutoArc() {
			@Override
			public void run() {
				returnDatum("node2", 2.0);
			}
		};
		Arc arc3 = new AutoArc() {
			@Override
			public void run() {
				returnDatum("node3", 3.0);
			}
		};

		// make three nodes
		Node node1 = new DiscreteNode("node1");
		Node node2 = new DiscreteNode("node2");
		Node node3 = new DiscreteNode("node3");
		Node initNode = Nodespace.DEFAULT.emptyOf();

		// make three links
		Link link1 = new Link(Set.of(initNode), arc1, node1);
		Link link2 = new Link(Set.of(node1), arc2, node2);
		Link link3 = new Link(Set.of(node2), arc3, node3);

		// make a graph with the three links
		Graph graph = new Graph(link1, link2, link3);
		Overseer overseer = new Overseer(graph);
		initNode.setUsable();
		overseer.start();

		System.out.println(toMarkdown(animate(graph, overseer.getLinkTrace())));
	}

	@Test
	void testMonitor() {
		// make a graph with 3 nodes and 2 arcs connecting them
		Arc arc1 = new AutoArc() {
			@Override
			public void run() {
				getOutputNode().setUsable();
			}
		};
		Arc arc2 = new AutoArc() {
			@Override
			public void run() {
				getOutputNode().setUsable();
			}
		};


		Node node1 = new EmptyNode();
		Node node2 = new EmptyNode();
		Node node3 = new EmptyNode();
		Link link1 = new Link(Set.of(node1), arc1, node2);
		Link link2 = new Link(Set.of(node2), arc2, node3);
		Graph graph = new Graph(link1, link2);
		Overseer overseer = new Overseer(graph);
		node1.setUsable();
		AtomicInteger tick2 = new AtomicInteger(0);
		overseer.setObserver((caller, tick, collection) -> {
			System.out.println(caller);
			if (tick == 1) {
				assertNull(caller);
				assertEquals(1, collection.size());
			} else if (tick == 2) {
				assertSame(node2, caller);
				assertEquals(1, collection.size());
			} else if (tick == 3) {
				assertSame(node3, caller);
				assertEquals(0, collection.size());
			}
			System.out.println("tick: " + tick);
			System.out.println("collection: " + collection);
			tick2.incrementAndGet();
		});
		overseer.start();
		assertEquals(2, tick2.get());
	}
}
