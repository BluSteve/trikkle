package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.annotations.Input;
import org.trikkle.annotations.Output;

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
		Arc arc = new AutoArc() {
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
}
