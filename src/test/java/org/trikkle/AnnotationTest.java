package org.trikkle;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AnnotationTest {
	@TrikkleFunction(output = "squared", inputs = {"toSquare"}, linkId = "node1")
	public static double square(double toSquare) {
		return toSquare * toSquare;
	}

	@TrikkleFunction(output = "squared", inputs = {"toSquare"}, linkId = "node2")
	public static double asdf(double toSquare) {
		return toSquare * toSquare;
	}

	@TrikkleFunction(output = "squared", inputs = {"toSquare"}, linkId = "node3")
	public double squareInstance(double toSquare) {
		return toSquare * toSquare;
	}

	@Test
	void test() {
		LinkProcessor linkProcessor = new LinkProcessor();
		linkProcessor.addMethodsOf(AnnotationTest.class);
		linkProcessor.refreshLinks();

		assertFalse(linkProcessor.getLinks().containsKey("squareInstance"));

		Node inputNode = DiscreteNode.of("toSquare");
		Node outputNode = DiscreteNode.of("squared");
		Arc arc = new AutoArc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = square(toSquare);
				returnDatum("squared", squared);
			}
		};
		arc.setName("square");
		Link manualLink = new Link(Set.of(inputNode), arc, outputNode);

		assertTrue(manualLink.congruentTo(linkProcessor.getLinks().get("node2")));

		Graph graph = new Graph(linkProcessor.getLinks().get("node2"));
		Overseer overseer = new Overseer(graph);
		overseer.addStartingDatum("toSquare", 2.0);
		overseer.start();

		Graph manualGraph = new Graph(manualLink);
		Overseer manualOverseer = new Overseer(manualGraph);
		manualOverseer.addStartingDatum("toSquare", 2.0);
		manualOverseer.start();

		assertEquals(overseer.getResultCache(), manualOverseer.getResultCache());
	}

	@Test
	void testInstance() {
		LinkProcessor linkProcessor = new LinkProcessor();
		linkProcessor.addMethodsOf(new AnnotationTest());
		linkProcessor.refreshLinks();

		assertFalse(linkProcessor.getLinks().containsKey("square"));

		Node inputNode = DiscreteNode.of("toSquare");
		Node outputNode = DiscreteNode.of("squared");
		Arc arc = new AutoArc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = new AnnotationTest().squareInstance(toSquare);
				returnDatum("squared", squared);
			}
		};
		arc.setName("square");
		Link manualLink = new Link(Set.of(inputNode), arc, outputNode);

		assertTrue(manualLink.congruentTo(linkProcessor.getLinks().get("node3")));

		Graph graph = new Graph(linkProcessor.getLinks().get("node3"));
		Overseer overseer = new Overseer(graph);
		overseer.addStartingDatum("toSquare", 2.0);
		overseer.start();

		Graph manualGraph = new Graph(manualLink);
		Overseer manualOverseer = new Overseer(manualGraph);
		manualOverseer.addStartingDatum("toSquare", 2.0);
		manualOverseer.start();

		assertEquals(overseer.getResultCache(), manualOverseer.getResultCache());
	}
}
