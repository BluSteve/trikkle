import org.junit.jupiter.api.Test;
import org.trikkle.*;
import org.trikkle.viz.MermaidGraphViz;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegratedTests {
	@TrikkleFunction(output = "omelette", inputs = {"chivesCount"}, linkId = "makeOmelette")
	@TrikkleFunction(output = "omelette", inputs = {"eggsCount"}, linkId = "makeOmelette")
	public static Omelette makeOmelette(int chivesCount, int eggsCount) {
		return new Omelette(chivesCount, eggsCount);
	}

	@TrikkleFunction(output = "customerHappy", inputs = {"omelette"}, linkId = "serveOmelette")
	@TrikkleFunction(output = "customerHappy", inputs = {"omelette1"}, linkId = "serveExpressOmelette")
	@TrikkleFunction(output = "customer2Happy", inputs = {"omelette2"}, linkId = "serveExpressOmelette2")
	public static boolean serveOmelette(Omelette omelette) {
		return omelette != null;
	}

	@TrikkleFunction(output = "omelette1", inputs = {"omelettes"}, linkId = "expressOmelette1")
	@TrikkleFunction(output = "omelette1", inputs = {"index1"}, linkId = "expressOmelette1")
	@TrikkleFunction(output = "omelette2", inputs = {"omelettes"}, linkId = "expressOmelette2")
	@TrikkleFunction(output = "omelette2", inputs = {"index2"}, linkId = "expressOmelette2")
	public static Omelette grabExpressOmelette(Omelette[] omelettes, int index) {
		return omelettes[index];
	}

	@Test
	void cafe() {
		LinkProcessor linkProcessor = new LinkProcessor();
		linkProcessor.addFunctionsOf(IntegratedTests.class);
		linkProcessor.refreshLinks("makeOmelette", "serveOmelette");
		Graph graphA = linkProcessor.getGraph();

		linkProcessor.refreshLinks("expressOmelette1", "expressOmelette2", "serveExpressOmelette",
				"serveExpressOmelette2");
		Graph graphB = linkProcessor.getGraph();
		System.out.println(MermaidGraphViz.defaultVisualize(graphA, graphB));


		Graph graphC = Graph.mergeGraphs(List.of(graphA, graphB),
				Set.of(DiscreteNode.of("customerHappy"), DiscreteNode.of("customer2Happy")));
		System.out.println(graphC);

		Overseer overseer = new Overseer(graphA);
		overseer.addStartingDatum("chivesCount", 2);
		overseer.addStartingDatum("eggsCount", 3);
		overseer.start();
		assertTrue((boolean) overseer.getResultCache().get("customerHappy"));
	}

	public record Omelette(int chivesCount, int eggsCount) {
	}
}
