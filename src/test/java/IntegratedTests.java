import org.junit.jupiter.api.Test;
import org.trikkle.DiscreteNode;
import org.trikkle.Graph;
import org.trikkle.LinkProcessor;
import org.trikkle.TrikkleFunction;

import java.util.List;
import java.util.Set;

public class IntegratedTests {
	public record Omelette(int chivesCount, int eggsCount) {
	}

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
		System.out.println(graphA);

		linkProcessor.refreshLinks("expressOmelette1", "expressOmelette2", "serveExpressOmelette", "serveExpressOmelette2");
		Graph graphB = linkProcessor.getGraph();
		System.out.println(graphB);


		Graph graphC = Graph.mergeGraphs(List.of(graphA, graphB), Set.of(new DiscreteNode("customerHappy")));
		System.out.println(graphC);
	}
}
