package org.trikkle;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AnnotationTest {
	@TrikkleLink(outputDatumName = "squared", inputDatumNames = {"toSquare"})
	public static double square(double toSquare) {
		return toSquare * toSquare;
	}

	@TrikkleLink(outputDatumName = "squared", arcName = "square", inputDatumNames = {"toSquare"})
	public static double asdf(double toSquare) {
		return toSquare * toSquare;
	}

	@TrikkleLink(outputDatumName = "squared", inputDatumNames = {"toSquare"})
	public double squareInstance(double toSquare) {
		return toSquare * toSquare;
	}

	@Test
	void arcNameTest() {
		LinkProcessor linkProcessor = new LinkProcessor();
		linkProcessor.addLinks(AnnotationTest.class);
		Link link1 = linkProcessor.links.get("square");
		Link link2 = linkProcessor.links.get("asdf");
		assertEquals(link1.getArc().name, link2.getArc().name);
	}

	@Test
	void test() {
		LinkProcessor linkProcessor = new LinkProcessor();
		linkProcessor.addLinks(AnnotationTest.class);

		assertFalse(linkProcessor.links.containsKey("squareInstance"));

		Node inputNode = new DiscreteNode("toSquare");
		Node outputNode = new DiscreteNode("squared");
		Arc arc = new Arc.AutoArc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = square(toSquare);
				returnDatum("squared", squared);
			}
		};
		arc.name = "square";
		Link manualLink = new Link(Set.of(inputNode), arc, outputNode);

		assertTrue(manualLink.congruentTo(linkProcessor.links.get("square")));

		Graph graph = new Graph(linkProcessor.links.get("square"));
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
		linkProcessor.addLinks(new AnnotationTest());

		assertFalse(linkProcessor.links.containsKey("square"));

		Node inputNode = new DiscreteNode("toSquare");
		Node outputNode = new DiscreteNode("squared");
		Arc arc = new Arc.AutoArc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = new AnnotationTest().squareInstance(toSquare);
				returnDatum("squared", squared);
			}
		};
		arc.name = "square";
		Link manualLink = new Link(Set.of(inputNode), arc, outputNode);

		assertTrue(manualLink.congruentTo(linkProcessor.links.get("squareInstance")));

		Graph graph = new Graph(linkProcessor.links.get("squareInstance"));
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
