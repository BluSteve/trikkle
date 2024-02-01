package org.trikkle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TrikkleFunctionGroupTest {
	@TrikkleFunction(outputDatumName = "sum", inputDatumNames = {"a", "b"}, linkId = "sqdiff")
	public static double add(double a, double b) {
		return a + b;
	}

	@TrikkleFunction(outputDatumName = "difference", inputDatumNames = {"a", "b"}, linkId = "sqdiff")
	public static double difference(double a, double b) {
		return Math.abs(a - b);
	}

	@Test
	void test1() {
		// one inputnode with two datums -> two functions -> two outputdatums (two functions)
		LinkProcessor linkProcessor = new LinkProcessor();
		linkProcessor.addFunctionsOf(TrikkleFunctionGroupTest.class);
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

	@TrikkleFunction(outputDatumName = "power", inputDatumNames = {"a"}, linkId = "pow")
	@TrikkleFunction(outputDatumName = "power", inputDatumNames = {"b"}, linkId = "pow")
	public static double pow(double a, double b) {
		return Math.pow(a, b);
	}

	@Test
	void test2() {
		// two inputnodes with one datum each -> one function -> one outputdatum (one function)
		LinkProcessor linkProcessor = new LinkProcessor();
		linkProcessor.addFunctionsOf(TrikkleFunctionGroupTest.class);
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
	@TrikkleFunction(outputDatumName = "power", inputDatumNames = {"a"}, linkId = "asdf")
	@TrikkleFunction(outputDatumName = "power", inputDatumNames = {"b"}, linkId = "asdf2")
	public double pow2(double a, double b) {
		return Math.pow(a, b);
	}

	@Test
	void test3() {
		LinkProcessor linkProcessor = new LinkProcessor();
		try {
			linkProcessor.addFunctionsOf(new TrikkleFunctionGroupTest());
		} catch (IllegalArgumentException e) {
			assertEquals("All TrikkleFunctions in a group must have the same linkId!", e.getMessage());
		}
	}

	@TrikkleFunction(inputDatumNames = {"X", "Y"}, outputDatumName = "abc", linkId = "somelink")
	@TrikkleFunction(inputDatumNames = {"Z"}, outputDatumName = "abc", linkId = "somelink")
	@TrikkleFunction(inputDatumNames = {"sum", "sum", "sum"}, outputDatumName = "result", linkId = "finallink")
	public static double func1(double a, double b, double c) {
		return a * b / c;
	}

	@TrikkleFunction(inputDatumNames = {"Z"}, outputDatumName = "aa2", linkId = "somelink")
	public static double func2(double a) {
		return a * a / 2;
	}

	@TrikkleFunction(inputDatumNames = {"abc", "aa2"}, outputDatumName = "sum", linkId = "middlelink")
	public static double sum(double a, double b) {
		return a + b;
	}

	@Test
	void test4() {
		LinkProcessor linkProcessor = new LinkProcessor();
		linkProcessor.addFunctionsOf(TrikkleFunctionGroupTest.class);
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
