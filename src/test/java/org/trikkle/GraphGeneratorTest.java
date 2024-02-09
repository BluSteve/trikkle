package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.utils.GraphGenerator;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphGeneratorTest {

	@Test
	void generateGraph() {
		Graph graph = GraphGenerator.generateGraph(3, 2);

		Node node1 = DiscreteNode.of("A");
		Node node2 = DiscreteNode.of("B");
		Node node3 = DiscreteNode.of("C");

		Arc arc1 = new AutoArc() {
			@Override
			public void run() {
			}
		};
		arc1.setName("1");
		Arc arc2 = new AutoArc() {
			@Override
			public void run() {
			}
		};
		arc2.setName("2");

		Graph manualGraph = new Graph(Set.of(
				new Link(Set.of(node2, node3), arc1, node1),
				new Link(Set.of(node2), arc2, node3)
		));

		assertTrue(graph.congruentTo(manualGraph));
	}
}
