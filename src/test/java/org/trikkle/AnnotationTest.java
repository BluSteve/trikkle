package org.trikkle;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AnnotationTest {
	@TrikkleLink(outputDatumName = "squared", inputDatumNames = {"toSquare"})
	public static double square(double toSquare) {
		return toSquare * toSquare;
	}

	@TrikkleLink(outputDatumName = "squared", inputDatumNames = {"toSquare"})
	public double squareInstance(double toSquare) {
		return toSquare * toSquare;
	}

	@TrikkleLink(outputDatumName = "squared", arcName = "square", inputDatumNames = {"toSquare"})
	public static double asdf(double toSquare) {
		return toSquare * toSquare;
	}

	@Test
	void arcNameTest() {
		Link link1 = LinkGenerator.getLinks(AnnotationTest.class).get("square");
		Link link2 = LinkGenerator.getLinks(AnnotationTest.class).get("asdf");
		assertEquals(link1.getArc().name, link2.getArc().name);
	}

	@Test
	void test() {
		Map<String, Link> links = LinkGenerator.getLinks(AnnotationTest.class);

		assertFalse(links.containsKey("squareInstance"));

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

		assertTrue(manualLink.congruentTo(links.get("square")));

		Graph graph = new Graph(links.get("square"));
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
		Map<String, Link> links = LinkGenerator.getLinks(new AnnotationTest());

		assertFalse(links.containsKey("square"));

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

		assertTrue(manualLink.congruentTo(links.get("squareInstance")));

		Graph graph = new Graph(links.get("squareInstance"));
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
