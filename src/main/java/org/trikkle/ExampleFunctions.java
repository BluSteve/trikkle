package org.trikkle;

import java.util.List;
import java.util.Map;
import java.util.Queue;
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
		Node inputNode = new DiscreteNode(Set.of("toSquare"));
		Arc arc = new Arc.AutoArc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = simpleFn(toSquare);
				returnDatum("squared", squared);
			}
		};
		Node outputNode = new DiscreteNode(Set.of("squared"));
		Todo todo = new Todo(Set.of(inputNode), arc, outputNode);


		Graph graph = new Graph(Set.of(todo), Set.of(inputNode), Set.of(outputNode));
		Overseer overseer = new Overseer(graph);

		inputNode.addDatum("toSquare", 2.0);
		overseer.start();


		Map<String, Object> results = overseer.getResultCache();
		System.out.println(results);
	}

	static void complexTest() {
		Node inputNode2 = new DiscreteNode(Set.of("finalMultiplier", "finalExponent"));
		Node inputNode = new DiscreteNode(Set.of("toSquare"));
		Arc arc = new Arc.AutoArc() {
			@Override
			public void run() {
				double toSquare = (double) getDatum("toSquare");
				double squared = simpleFn(toSquare);
				returnDatum("squared", squared);
			}
		};
		Node node2 = new DiscreteNode(Set.of("squared"));
		Todo todo = new Todo(Set.of(inputNode), arc, node2);


		Arc arc2 = new Arc.AutoArc() {
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

		Arc arc3 = new Arc.AutoArc() {
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


		Arc phantomArc1 = new Arc.AutoArc() {
			@Override
			public void run() {
				returnDatum("toSquare", 2.0);
			}
		};
		Todo phantomTodo1 = new Todo(Set.of(), phantomArc1, inputNode);

		Arc phantomArc2 = new Arc.AutoArc() {
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

		Graph graph = new Graph(Set.of(todo, todo2, todo3, phantomTodo1, phantomTodo2), Set.of(), Set.of(node4));
		Overseer overseer = new Overseer(graph);
		overseer.start();


		Map<String, Object> results = overseer.getResultCache();
		System.out.println(results);
	}

	static void streamTest() {
		Arc inputArc = new Arc.AutoArc() {
			@Override
			public void run() {
				for (int i = 1; i < 10; i++) {
					returnDatum("stream1", (double) i);
					outputNode.setProgress(i / 10.0);
				}

				outputNode.setProgress(1);
			}
		};
		Node streamNode = new StreamNode(Set.of("stream1"));
		Todo todo = new Todo(Set.of(), inputArc, streamNode);

		Arc consumerArc = new Arc() {
			double total = 0; // is this a pure function?
			@Override
			public void run() {
				Queue<Double> queue = (Queue<Double>) getDatum("stream1");
				Node stream1Node = overseer.getOutputNodeOfDatum("stream1");

				double sum = 0;
				synchronized (queue) {
					if (queue.size() >= 3) {
						this.status = ArcStatus.IN_PROGRESS;
						while (!queue.isEmpty()) {
							sum += queue.poll();
						}

						total += sum;
						// prevents stream1Node from calling this arc again when another node ticktock().
						stream1Node.setUsable(false);
					}
				}

				System.out.println("total = " + total);

				if (stream1Node.getProgress() == 1) {
					stream1Node.setUsable(false);
					this.status = ArcStatus.FINISHED;
					returnDatum("result1", total); // this must be the last line as it's a recursive call
				}
				else this.status = ArcStatus.IDLE;
			}
		};
		Node outputNode = new DiscreteNode(Set.of("result1"));
		Todo todo2 = new Todo(Set.of(streamNode), consumerArc, outputNode);

		Graph graph = new Graph(Set.of(todo, todo2), Set.of(), Set.of(outputNode));
		Overseer overseer = new Overseer(graph);
		overseer.start();

		Map<String, Object> results = overseer.getResultCache();
		System.out.println(results);
	}

	static void mergeTest() {
		Node paramNode = new DiscreteNode(Set.of("param"));
		Node magicNode = new DiscreteNode(Set.of("magic"));
		Node hfNode = new DiscreteNode(Set.of("hf"));
		Node matrixNode = new DiscreteNode(Set.of("matrix"));
		Node dipoleNode = new DiscreteNode(Set.of("dipole"));

		Arc arc1 = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};
		Arc arc2 = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};
		Arc arc3 = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};
		Arc arc4 = new Arc.AutoArc() {
			@Override
			public void run() {

			}
		};

		Todo todo1 = new Todo(Set.of(magicNode, paramNode), arc1, hfNode);
		Todo todo4 = new Todo(Set.of(paramNode), arc4, dipoleNode);
		Todo todo2 = new Todo(Set.of(matrixNode), arc2, hfNode);
		Todo todo3 = new Todo(Set.of(matrixNode), arc3, dipoleNode);

		Graph graph1 = new Graph(Set.of(todo1, todo4), Set.of(paramNode), Set.of(hfNode));
		Graph graph2 = new Graph(Set.of(todo2, todo3), Set.of(matrixNode), Set.of(hfNode, dipoleNode));
		Graph graph3 = Graph.mergeGraphs(List.of(graph1, graph2), null, Set.of(hfNode, dipoleNode));

		for (Todo todo : graph3.getTodos()) {
			System.out.println(todo);
		}
		System.out.println();
	}

	public static void main(String[] args) {
		complexTest();
		streamTest();
		mergeTest();
	}
}
