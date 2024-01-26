package org.trikkle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

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

	public static void main(String[] args) {
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
		Todo todo = new Todo(Arrays.asList(inputNode), arc, outputNode);


		Overseer overseer = new Overseer();
		overseer.addTodos(todo);
		overseer.setAsStarting(inputNode);
		overseer.setAsEnding(outputNode);

		inputNode.addDatum("toSquare", 2.0);
		overseer.start();


		Map<String, Object> results = overseer.getResultCache();
		System.out.println(results);
	}
}
