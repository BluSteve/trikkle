import org.trikkle.*;
import org.trikkle.viz.LogUtils;

import java.util.*;

// Imagine that arithmetic takes a lot of time.
public class QuadraticExample {
	public static void main(String[] args) {
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

		Overseer overseer = new Overseer(graph);
		overseer.setLogging(true);
		overseer.addStartingDatum("a", 1.0);
		overseer.addStartingDatum("b", 5.0);
		overseer.addStartingDatum("c", 6.0);
		overseer.start();

		System.out.println("Final tick: " + overseer.getTick());
		System.out.println("\nLink trace:");
		for (Collection<Link> linkCollection : overseer.getLinkTrace()) {
			System.out.println(linkCollection);
		}
		System.out.println("\nFinal cache:");
		for (Map.Entry<String, Object> stringObjectEntry : overseer.getCacheCopy().entrySet()) {
			System.out.println(stringObjectEntry);
		}

		System.out.println(LogUtils.toMarkdown(LogUtils.animate(graph, overseer.getLinkTrace())));
	}
}
