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

	@TrikkleFunctionGroup({
			@TrikkleFunction(outputDatumName = "power", inputDatumNames = {"a"}, linkId = "pow"),
			@TrikkleFunction(outputDatumName = "power", inputDatumNames = {"b"}, linkId = "pow")
	})
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
}
