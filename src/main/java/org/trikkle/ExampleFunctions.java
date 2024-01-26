package org.trikkle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

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
		Node inputNode = new DiscreteNode(new String[]{"toSquare"});
		Arc arc = new Arc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = simpleFn(toSquare);
				returnDatum("squared", squared);
			}
		};
		Node outputNode = new DiscreteNode(new String[]{"squared"});
		Todo todo = new Todo(Set.of(inputNode), arc, outputNode);


		Overseer overseer = new Overseer();
		overseer.addTodos(Set.of(todo));
		overseer.setAsStarting(Set.of(inputNode));
		overseer.setAsEnding(Set.of(outputNode));

		inputNode.addDatum("toSquare", 2.0);
		overseer.start();


		Map<String, Object> results = overseer.getResultCache();
		System.out.println(results);
	}

	static void complexTest() {
		Node inputNode = new DiscreteNode(new String[]{"toSquare"});
		Arc arc = new Arc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = simpleFn(toSquare);
				returnDatum("squared", squared);
			}
		};
		Node node2 = new DiscreteNode(new String[]{"squared"});
		Todo todo = new Todo(Set.of(inputNode), arc, node2);


		Arc arc2 = new Arc() {
			@Override
			public void run() {
				double squared = (double) getDatum("squared");
				double toSquare = (double) getDatum("toSquare");
				double result = squared + 1.5 * toSquare;

				returnDatum("result", result);
			}
		};
		Node node3 = new DiscreteNode(new String[]{"result"});

		Todo todo2 = new Todo(Set.of(inputNode, node2), arc2, node3);


		Overseer overseer = new Overseer();
		overseer.addTodos(Set.of(todo, todo2));
		overseer.setAsStarting(Set.of(inputNode));
		overseer.setAsEnding(Set.of(node3));

		inputNode.addDatum("toSquare", 2.0);
		overseer.start();


		Map<String, Object> results = overseer.getResultCache();
		System.out.println(results);
	}

	public static void main(String[] args) {
		complexTest();

	}
}
