package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.annotation.LinkProcessor;
import org.trikkle.annotation.TrikkleFunction;

import static org.junit.jupiter.api.Assertions.*;

public class TrikkleMethodGroupTest {
	@TrikkleFunction(output = "sum", inputs = {"a", "b"}, linkId = "sqdiff")
	public static double add(double a, double b) {
		return a + b;
	}

	@TrikkleFunction(output = "difference", inputs = {"a", "b"}, linkId = "sqdiff")
	public static double difference(double a, double b) {
		return Math.abs(a - b);
	}

	@Test
	void test1() {
		// one inputnode with two datums -> two functions -> two outputdatums (two functions)
		LinkProcessor linkProcessor = new LinkProcessor();
		linkProcessor.addMethodsOf(TrikkleMethodGroupTest.class);
		linkProcessor.refreshLinks("sqdiff");
		assertEquals(1, linkProcessor.getLinks().size());

		Graph graph = linkProcessor.getGraph();
		Overseer overseer = new Overseer(graph);

		System.out.println(overseer.getStartingDatumNames());
		overseer.addStartingDatum("a", 5);
		overseer.addStartingDatum("b", 3);
		overseer.start();

		var resultCache = overseer.getResultCache();
		assertEquals(8.0, resultCache.get("sum"));
		assertEquals(2.0, resultCache.get("difference"));
	}

	@TrikkleFunction(output = "power", inputs = {"a"}, linkId = "pow")
	@TrikkleFunction(output = "power", inputs = {"b"}, linkId = "pow")
	public static double pow(double a, double b) {
		return Math.pow(a, b);
	}

	@Test
	void test2() {
		// two inputnodes with one datum each -> one function -> one outputdatum (one function)
		LinkProcessor linkProcessor = new LinkProcessor();
		linkProcessor.addMethodsOf(TrikkleMethodGroupTest.class);
		linkProcessor.refreshLinks("pow");
		assertEquals(1, linkProcessor.getLinks().size());

		Graph graph = new Graph(linkProcessor.getLinks().get("pow"));
		Overseer overseer = new Overseer(graph);

		System.out.println(overseer.getStartingDatumNames());
		overseer.addStartingDatum("a", 5);
		overseer.addStartingDatum("b", 3);
		overseer.start();

		var resultCache = overseer.getResultCache();
		assertEquals(125.0, resultCache.get("power"));
	}

	// test two trikklefunctions with different linkid. it should throw an error
	@TrikkleFunction(output = "power", inputs = {"a"}, linkId = "asdf")
	@TrikkleFunction(output = "power2", inputs = {"b"}, linkId = "asdf")
	public double pow2(double a, double b) {
		return Math.pow(a, b);
	}

	@Test
	void test3() {
		LinkProcessor linkProcessor = new LinkProcessor();
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			linkProcessor.addMethodsOf(new TrikkleMethodGroupTest());
		});
		assertTrue(exception.getMessage()
				.contains("All TrikkleFunctions with the same linkId must have the same output!"));
	}

	@TrikkleFunction(inputs = {"X", "Y"}, output = "abc", linkId = "somelink")
	@TrikkleFunction(inputs = {"Z"}, output = "abc", linkId = "somelink")
	@TrikkleFunction(inputs = {"sum", "sum", "sum"}, output = "result", linkId = "finallink")
	public static double func1(double a, double b, double c) {
		return a * b / c;
	}

	@TrikkleFunction(inputs = {"Z"}, output = "aa2", linkId = "somelink")
	public static double func2(double a) {
		return a * a / 2;
	}

	@TrikkleFunction(inputs = {"abc", "aa2"}, output = "sum", linkId = "middlelink")
	public static double sum(double a, double b) {
		return a + b;
	}

	@Test
	void test4() {
		LinkProcessor linkProcessor = new LinkProcessor();
		linkProcessor.addMethodsOf(TrikkleMethodGroupTest.class);
		linkProcessor.refreshLinks("somelink", "middlelink", "finallink");
		assertEquals(3, linkProcessor.getLinks().size());

		Graph graph = linkProcessor.getGraph();
		System.out.println(graph);
		Overseer overseer = new Overseer(graph);

		assertEquals(2, graph.startingNodes.size());

		System.out.println(overseer.getStartingDatumNames());
		overseer.addStartingDatum("X", 5);
		overseer.addStartingDatum("Y", 3);
		overseer.addStartingDatum("Z", 2);
		overseer.start();

		var resultCache = overseer.getResultCache();
		assertEquals(9.5, resultCache.get("result"));
	}
}
