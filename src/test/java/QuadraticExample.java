import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.trikkle.*;
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
				double a = getDatum("a");

				returnDatum("2a", 2 * a);
			}
		};
		links.add(new Link(Set.of(nodeA), arc1, Set.of(node2A)));

		Arc arc2 = new AutoArc("square") {
			@Override
			protected void run() {
				double b = getDatum("b");

				returnDatum("b^2", b * b);
			}
		};
		links.add(new Link(Set.of(nodeB), arc2, Set.of(nodeBsq)));

		Arc arc3 = new AutoArc("make 4ac") {
			@Override
			protected void run() {
				double twiceA = getDatum("2a");
				double c = getDatum("c");

				returnDatum("4ac", 2 * twiceA * c);
			}
		};
		links.add(new Link(Set.of(node2A, nodeC), arc3, Set.of(node4AC)));

		Arc arc4 = new AutoArc("determinant") {
			@Override
			protected void run() {
				double bsq = getDatum("b^2");
				double fourAC = getDatum("4ac");

				returnDatum("sqrt(b^2 - 4ac)", Math.sqrt(bsq - fourAC));
				returnDatum("-sqrt(b^2 - 4ac)", -Math.sqrt(bsq - fourAC));
			}
		};
		links.add(new Link(Set.of(nodeBsq, node4AC), arc4, Set.of(nodeDetsqrt)));

		Arc arc5 = new AutoArc("quadratic<br>formula") {
			@Override
			protected void run() {
				double b = getDatum("b");
				double twiceA = getDatum("2a");
				double detsqrtpos = getDatum("sqrt(b^2 - 4ac)");
				double detsqrtneg = getDatum("-sqrt(b^2 - 4ac)");

				returnDatum("larger root", (-b + detsqrtpos) / twiceA);
				returnDatum("smaller root", (-b + detsqrtneg) / twiceA);
			}
		};
		links.add(new Link(Set.of(nodeDetsqrt), arc5, Set.of(nodePosSoln, nodeNegSoln)));

		Graph graph = new Graph(links);
		System.out.println(graph);

		return graph;
	}

	static Graph simple() {
		List<Link> links = new ArrayList<>();

		String a$in, b$in, c$in, a2$in, bsq$in, fourac$in, detsqrtpos$in, detsqrtneg$in;
		String a2$out, bsq$out, fourac$out, detsqrtpos$out, detsqrtneg$out, pos$out, neg$out;

		a$in = "a";
		b$in = "b";
		c$in = "c";
		a2$in = a2$out = "2a";
		bsq$in = bsq$out = "b^2";
		fourac$in = fourac$out = "4ac";
		detsqrtpos$in = detsqrtpos$out = "sqrt(b^2 - 4ac)";
		detsqrtneg$in = detsqrtneg$out = "-sqrt(b^2 - 4ac)";
		pos$out = "larger root";
		neg$out = "smaller root";

		Arc arc1 = new AutoArc("x2") {
			String[] s = {a$in, a2$out};
			double a, a2;

			@Override
			protected void run() {
				a2 = 2 * a;
			}
		};
		links.add(new Link(arc1));

		Arc arc2 = new AutoArc("square") {
			String[] s = {b$in, bsq$out};
			double b, bsq;

			@Override
			protected void run() {
				bsq = b * b;
			}
		};
		links.add(new Link(arc2));

		Arc arc3 = new AutoArc("make 4ac") {
			String[] s = {a2$in, c$in, fourac$out};
			double a2, c, fourac;

			@Override
			protected void run() {
				fourac = 2 * a2 * c;
			}
		};
		links.add(new Link(arc3));

		Arc arc4 = new AutoArc("determinant") {
			String[] s = {bsq$in, fourac$in, detsqrtpos$out, detsqrtneg$out};
			double bsq, fourac, detsqrtpos, detsqrtneg;

			@Override
			protected void run() {
				detsqrtpos = Math.sqrt(bsq - fourac);
				detsqrtneg = -Math.sqrt(bsq - fourac);
			}
		};
		links.add(new Link(arc4));

		Arc arc5 = new AutoArc("quadratic<br>formula") {
			String[] s = {b$in, a2$in, detsqrtpos$in, detsqrtneg$in, pos$out, neg$out};
			double b, a2, detsqrtpos, detsqrtneg, pos, neg;

			@Override
			protected void run() {
				pos = (-b + detsqrtpos) / a2;
				neg = (-b + detsqrtneg) / a2;
			}
		};
		links.add(new Link(null, arc5, Set.of(new DiscreteNode(pos$out), new DiscreteNode(neg$out))));

		Graph.preprocess(links, new Nodespace());

		Graph graph = new Graph(links);
		graph.optimizeDependencies();
		System.out.println(graph);
		return graph;
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
		Graph annotationGraph = simple();
		Graph verboseGraph = verbose();
		Assertions.assertTrue(annotationGraph.congruentTo(verboseGraph));
		testGraph(annotationGraph);
		testGraph(verboseGraph);
	}
}
