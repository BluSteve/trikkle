package org.trikkle;

import java.util.*;

public class ExampleFunctions {
	static double fn1() {
		return 1;
	}

	static double fn2() {
		return 2;
	}

	static double fn3() {
		return 3;
	}

	static double[] fn4(double[] input) {
		double[] output = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = input[i] * input[i];
		}
		return output;
	}

	static double simpleFn(double x) {
		return x * x;
	}

	static void simpleTest() {
		Node inputNode = new DiscreteNode(Set.of("toSquare"));
		Arc arc = new Arc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = simpleFn(toSquare);
				returnDatum("squared", squared);
			}
		};
		Node outputNode = new DiscreteNode(Set.of("squared"));
		Todo todo = new Todo(Set.of(inputNode), arc, outputNode);


		Overseer overseer = new Overseer(Set.of(todo));
		overseer.setAsStarting(Set.of(inputNode));
		overseer.setAsEnding(Set.of(outputNode));

		inputNode.addDatum("toSquare", 2.0);
		overseer.start();


		Map<String, Object> results = overseer.getResultCache();
		System.out.println(results);
	}

	static void complexTest() {
		Node inputNode2 = new DiscreteNode(Set.of("finalMultiplier", "finalExponent"));
		Node inputNode = new DiscreteNode(Set.of("toSquare"));
		Arc arc = new Arc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = simpleFn(toSquare);
				returnDatum("squared", squared);
			}
		};
		Node node2 = new DiscreteNode(Set.of("squared"));
		Todo todo = new Todo(Set.of(inputNode), arc, node2);


		Arc arc2 = new Arc() {
			@Override
			public void run() {
				double squared = (double) getDatum("squared");
				double toSquare = (double) getDatum("toSquare");
				double result = squared + 1.5 * toSquare;

				returnDatum("result1", result);
			}
		};
		Node node3 = new DiscreteNode(Set.of("result1"));
		Todo todo2 = new Todo(Set.of(inputNode, node2), arc2, node3);

		Arc arc3 = new Arc() {
			@Override
			public void run() {
				double result1 = (double) getDatum("result1");
				double finalMultiplier = (double) getDatum("finalMultiplier");
				double finalExponent = (double) getDatum("finalExponent");
				returnDatum("result2", Math.pow(result1 * finalMultiplier, finalExponent));
			}
		};
		Node node4 = new DiscreteNode(Set.of("result2"));
		Todo todo3 = new Todo(Set.of(node3, inputNode2), arc3, node4);


		Arc phantomArc1 = new Arc() {
			@Override
			public void run() {
				returnDatum("toSquare", 2.0);
			}
		};
		Todo phantomTodo1 = new Todo(Set.of(), phantomArc1, inputNode);

		Arc phantomArc2 = new Arc() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				returnDatum("finalMultiplier", 3.0);
				returnDatum("finalExponent", 1.2);
			}
		};
		Todo phantomTodo2 = new Todo(Set.of(), phantomArc2, inputNode2);

		Overseer overseer = new Overseer(Set.of(todo, todo2, todo3, phantomTodo1, phantomTodo2));
		overseer.setAsStarting(Set.of());
		overseer.setAsEnding(Set.of(node4));
		overseer.start();


		Map<String, Object> results = overseer.getResultCache();
		System.out.println(results);
	}

	public static void main(String[] args) {
		simpleTest();
		complexTest();
	}
}
