import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.trikkle.*;
import org.trikkle.annotations.HalfLink;
import org.trikkle.annotations.Input;
import org.trikkle.annotations.Output;
import org.trikkle.viz.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Imagine that arithmetic takes a lot of time.
class QuadraticExample {
	static Graph verbose() {
		Node nodeA = new DiscreteNode("a");
		Node nodeB = new DiscreteNode("b");
		Node nodeC = new DiscreteNode("c");

		Node node2A = new DiscreteNode("2a");
		Node nodeBsq = new DiscreteNode("b^2");
		Node node4AC = new DiscreteNode("4ac");

		Node nodeDetsqrt = new DiscreteNode("sqrt(b^2 - 4ac)", "-sqrt(b^2 - 4ac)");

		Node nodePosSoln = new DiscreteNode("larger root");
		Node nodeNegSoln = new DiscreteNode("smaller root");

		List<Link> links = new ArrayList<>();

		Arc arc1 = new AutoArc("x2") {
			@Override
			protected void run() {
				double a = (double) getDatum("a");

				returnDatum("2a", 2 * a);
			}
		};
		links.add(new Link(Set.of(nodeA), arc1, node2A));

		Arc arc2 = new AutoArc("square") {
			@Override
			protected void run() {
				double b = (double) getDatum("b");

				returnDatum("b^2", b * b);
			}
		};
		links.add(new Link(Set.of(nodeB), arc2, nodeBsq));

		Arc arc3 = new AutoArc("make 4ac") {
			@Override
			protected void run() {
				double twiceA = (double) getDatum("2a");
				double c = (double) getDatum("c");

				returnDatum("4ac", 2 * twiceA * c);
			}
		};
		links.add(new Link(Set.of(node2A, nodeC), arc3, node4AC));

		Arc arc4 = new AutoArc("determinant") {
			@Override
			protected void run() {
				double bsq = (double) getDatum("b^2");
				double fourAC = (double) getDatum("4ac");

				returnDatum("sqrt(b^2 - 4ac)", Math.sqrt(bsq - fourAC));
				returnDatum("-sqrt(b^2 - 4ac)", -Math.sqrt(bsq - fourAC));
			}
		};
		links.add(new Link(Set.of(nodeBsq, node4AC), arc4, nodeDetsqrt));

		Arc arc5 = new AutoArc("quadratic<br>formula") {
			@Override
			protected void run() {
				double b = (double) getDatum("b");
				double twiceA = (double) getDatum("2a");
				double detsqrtpos = (double) getDatum("sqrt(b^2 - 4ac)");
				double detsqrtneg = (double) getDatum("-sqrt(b^2 - 4ac)");

				returnDatum("larger root", (-b + detsqrtpos) / twiceA);
				returnDatum("smaller root", (-b + detsqrtneg) / twiceA);
			}
		};
		links.add(new Link(Set.of(nodeDetsqrt), arc5, Set.of(nodePosSoln, nodeNegSoln)));

		Graph graph = new Graph(links);
		System.out.println(graph);

		return graph;
	}

	static Graph annotation() {
		Arc arc1 = new AutoArc("x2") {
			@Input
			double a;
			@Output(name = "2a")
			double twiceA;

			@Override
			protected void run() {
				twiceA = 2 * a;
			}
		};

		Arc arc2 = new AutoArc("square") {
			@Input
			double b;
			@Output(name = "b^2")
			double bsq;

			@Override
			protected void run() {
				bsq = b * b;
			}
		};

		Arc arc3 = new AutoArc("make 4ac") {
			@Input(name = "2a")
			double twiceA;
			@Input
			double c;
			@Output(name = "4ac")
			double fourAC;

			@Override
			protected void run() {
				fourAC = 2 * twiceA * c;
			}
		};

		Arc arc4 = new AutoArc("determinant") {
			@Input
			double bsq, fourAC;

			{
				alias("bsq", "b^2");
				alias("fourAC", "4ac");
			}

			@Override
			protected void run() {
				returnDatum("sqrt(b^2 - 4ac)", Math.sqrt(bsq - fourAC));
				returnDatum("-sqrt(b^2 - 4ac)", -Math.sqrt(bsq - fourAC));
			}
		};
		arc4.setOutputDatumNames("sqrt(b^2 - 4ac)", "-sqrt(b^2 - 4ac)");

		Arc arc5 = new AutoArc("quadratic<br>formula") {
			@Override
			protected void run() {
				double b = (double) getDatum("b");
				double twiceA = (double) getDatum("2a");
				double detsqrtpos = (double) getDatum("sqrt(b^2 - 4ac)");
				double detsqrtneg = (double) getDatum("-sqrt(b^2 - 4ac)");

				returnDatum("larger root", (-b + detsqrtpos) / twiceA);
				returnDatum("smaller root", (-b + detsqrtneg) / twiceA);
			}
		};
		arc5.setInputDatumNames("b", "2a", "sqrt(b^2 - 4ac)", "-sqrt(b^2 - 4ac)");
		arc5.setOutputDatumNames("larger root", "smaller root");

		List<HalfLink> halfLinks = new ArrayList<>();
		halfLinks.add(new HalfLink(arc1));
		halfLinks.add(new HalfLink(arc2));
		halfLinks.add(new HalfLink(arc3));
		halfLinks.add(new HalfLink(arc4));
		halfLinks.add(new HalfLink(arc5, Set.of(
				new DiscreteNode("larger root"),
				new DiscreteNode("smaller root"))));

		List<Link> links = HalfLink.toFullLinks(halfLinks);

		Graph graph = new Graph(links);

		Graph optimizedGraph = new Graph(graph);
		optimizedGraph.optimizeDependencies();

		System.out.println(graph);
		System.out.println(optimizedGraph);

		return optimizedGraph;
	}

	private static void testGraph(Graph graph) {
		Overseer overseer = new Overseer(graph);
		overseer.setLogging(true);
		overseer.addStartingDatum("a", 1.0);
		overseer.addStartingDatum("b", 5.0);
		overseer.addStartingDatum("c", 6.0);
		overseer.start();

		System.out.println("Final tick: " + overseer.getTick());

		System.out.println("\nLink trace:");
		System.out.print(LogUtils.linkTraceToString(overseer.getLinkTrace()));

		System.out.println("\nFinal cache:");
		for (Map.Entry<String, Object> stringObjectEntry : overseer.getCacheCopy().entrySet()) {
			System.out.println(stringObjectEntry);
		}

		Assertions.assertEquals(-2.0, overseer.getResultCache().get("larger root"));
		Assertions.assertEquals(-3.0, overseer.getResultCache().get("smaller root"));

		System.out.println(LogUtils.toMarkdown(LogUtils.animate(graph, overseer.getLinkTrace())));
	}

	@Test
	void test() {
		Graph annotationGraph = annotation();
		Graph verboseGraph = verbose();
		Assertions.assertTrue(annotationGraph.congruentTo(verboseGraph));
		testGraph(annotationGraph);
		testGraph(verboseGraph);
	}
}
