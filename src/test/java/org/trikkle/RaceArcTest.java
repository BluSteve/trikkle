package org.trikkle;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RaceArcTest {
	private static int sumUntil(int n) {
		int sum = 0;
		for (int i = 0; i <= n; i++) {
			if (Thread.currentThread().isInterrupted()) {
				System.out.println("Interrupted! i = " + i);
				break;
			}
			sum += i;
		}
		System.out.println("sum = " + sum);
		return sum;
	}

	private static int sumUntil2(int n) {
		return (n * (n + 1)) / 2;
	}

	@Test
	void raceTest() {
		RaceArc raceArc = new RaceArc(
				Set.of(datumMap -> {
					Map<String, Object> resultMap = new HashMap<>();
					resultMap.put("sum", sumUntil((int) datumMap.get("n")));
					return resultMap;
				}, datumMap -> {
					Map<String, Object> resultMap = new HashMap<>();
					resultMap.put("sum", sumUntil2((int) datumMap.get("n")));
					return resultMap;
				})
		);

		Node inputNode = DiscreteNode.of("n");
		Node outputNode = DiscreteNode.of("sum");
		Link link = new Link(Set.of(inputNode), raceArc, outputNode);
		Graph graph = new Graph(link);
		Overseer overseer = new Overseer(graph);

		int n = 100000;
		inputNode.addDatum("n", n);
		overseer.start();
		Map<String, Object> results = overseer.getResultCache();
		assertEquals(sumUntil(n), sumUntil2(n));
		assertEquals(sumUntil2(n), results.get("sum"));
	}
}
