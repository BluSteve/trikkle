import org.junit.jupiter.api.Test;
import org.trikkle.*;
import org.trikkle.annotation.LinkProcessor;
import org.trikkle.annotation.TrikkleFunction;
import org.trikkle.viz.MermaidGraphViz;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

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

	public static boolean serveOmeletteAndCoffee(Omelette omelette, Coffee coffee) {
		return omelette != null;
	}

	@Test
	void cafe() {
		LinkProcessor linkProcessor = new LinkProcessor();
		linkProcessor.addMethodsOf(IntegratedTests.class);
		linkProcessor.refreshLinks("makeOmelette", "serveOmelette");
		Graph graphA = linkProcessor.getGraph();

		linkProcessor.refreshLinks("expressOmelette1", "expressOmelette2", "serveExpressOmelette",
				"serveExpressOmelette2");
		Graph graphB = linkProcessor.getGraph();

		System.out.println(graphA);
		System.out.println(graphB);
		Graph graphC = Graph.mergeGraphs(List.of(graphA, graphB),
				Set.of(Nodespace.DEFAULT.discreteOf("customerHappy"), Nodespace.DEFAULT.discreteOf("customer2Happy")));
		System.out.println(graphC);

		Arc brewerArc = new AutoArc() {
			@Override
			public void run() {
				// make 100 ml of coffee at a time
				for (int i = 0; i < 7; i++) {
					returnDatum("brewer", 100.0);
					try {
						TimeUnit.MILLISECONDS.sleep(10);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		};
		brewerArc.setName("brew coffee");
		Node brewerNode = Nodespace.DEFAULT.streamOf("brewer");
		Link brewerLink = new Link(Set.of(), brewerArc, brewerNode);
		Arc coffeeArc = new Arc(false) {
			@Override
			public void run() {
				Queue<Double> volume = getDatum("brewer");
				List<Double> volumes = Arrays.asList(volume.toArray(new Double[0])); // thread-safe snapshot
				double sum = volumes.stream().mapToDouble(Double::doubleValue).sum();
				if (sum >= 500) {
					this.setStatus(ArcStatus.FINISHED);
					returnDatum("coffee", new Coffee(sum));
				} else {
					this.setStatus(ArcStatus.IDLE);
				}
			}
		};
		coffeeArc.setName("make coffee");
		Node coffeeNode = Nodespace.DEFAULT.discreteOf("coffee");
		Link coffeeLink = new Link(Set.of(brewerNode), coffeeArc, coffeeNode);
		Node omeletteNode = Nodespace.DEFAULT.discreteOf("omelette");
		Arc serveBothArc = new AutoArc() {
			@Override
			public void run() {
				Omelette omelette = getDatum("omelette");
				Coffee coffee = getDatum("coffee");
				returnDatum("customerVeryHappy", serveOmeletteAndCoffee(omelette, coffee));
			}
		};
		serveBothArc.setName("serve omelette and coffee");
		Node customerNode = Nodespace.DEFAULT.discreteOf("customerVeryHappy");
		Link serveBothLink = new Link(Set.of(omeletteNode, coffeeNode), serveBothArc, customerNode);

		Arc autoArc = new AutoArc() {
			@Override
			public void run() {
			}
		};
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			Link link = new Link(Set.of(brewerNode), autoArc, coffeeNode);
		});
		assertEquals("StreamNode cannot be the input of an AutoArc. Use Arc instead.", e.getMessage());

		Graph graphD = new Graph(brewerLink, coffeeLink, serveBothLink);
		graphA = Graph.concatGraphs(graphA, graphD);
		System.out.println(MermaidGraphViz.defaultVisualize(graphA, graphB));

		Overseer overseer = new Overseer(Graph.concatGraphs(graphA, graphD));
		overseer.addStartingDatum("chivesCount", 2);
		overseer.addStartingDatum("eggsCount", 3);
		overseer.start();
		System.out.println(overseer.getGraph().links);
		System.out.println(overseer.getBurstTimes(TimeUnit.NANOSECONDS));
		assertTrue((boolean) overseer.getResultCache().get("customerHappy"));
		assertTrue((boolean) overseer.getResultCache().get("customerVeryHappy"));
		System.out.println(overseer.getTick());
	}

	public record Omelette(int chivesCount, int eggsCount) {
	}

	public record Coffee(double volume) {
	}
}
