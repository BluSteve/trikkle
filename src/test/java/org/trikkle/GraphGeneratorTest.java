package org.trikkle;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphGeneratorTest {

	@Test
	void generateGraph() {
		Graph graph = GraphGenerator.generateGraph(3, 2);

		Node node1 = new DiscreteNode("A");
		Node node2 = new DiscreteNode("B");
		Node node3 = new DiscreteNode("C");

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

		Graph manualGraph = new Graph(List.of(
				new Link(Set.of(node2, node3), arc1, node1),
				new Link(Set.of(node2), arc2, node3)
		));

		assertTrue(graph.congruentTo(manualGraph));
	}
}
