package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.annotations.Input;
import org.trikkle.annotations.Output;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
	}
}
