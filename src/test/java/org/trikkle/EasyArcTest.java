package org.trikkle;

import org.junit.jupiter.api.Test;
import org.trikkle.annotations.Input;
import org.trikkle.annotations.Output;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EasyArcTest {
	public static void fillArc(Arc arc) {
		Field[] fields = arc.getClass().getDeclaredFields();
		try {
			for (Field field : fields) {
				if (field.isAnnotationPresent(Input.class)) {
					field.set(arc, arc.getDatum(field.getName()));
				}
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static void returnFromArc(Arc arc) {
		Field[] fields = arc.getClass().getDeclaredFields();
		try {
			for (Field field : fields) {
				if (field.isAnnotationPresent(Output.class)) {
					arc.returnDatum(field.getName(), field.get(arc));
				}
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void testEasyArc() {
		Arc arc = new AutoArc() {
			@Input
			double input1;
			@Input
			String input2;
			@Output
			int output;

			@Override
			public void run() {
				output = (int) input1 + input2.length();
				System.out.println("output = " + output);
			}
		};
		Node inputNode = new DiscreteNode("input1", "input2");
		Node outputNode = new DiscreteNode("output");
		Link link = new Link(Set.of(inputNode), arc, outputNode);
		Graph graph = new Graph(link);
		Overseer overseer = new Overseer(graph);
		overseer.addStartingDatum("input1", 2.5);
		overseer.addStartingDatum("input2", "hello");

		fillArc(arc);
		arc.runWrapper();
		returnFromArc(arc);
		System.out.println(overseer.getCache());
		assertEquals(7, overseer.getResultCache().get("output"));

		/*
		System.out.println(Arrays.toString(arc.getClass().getDeclaredFields()));
		Field[] fields = arc.getClass().getDeclaredFields();
		for (Field field : fields) {
			System.out.println(field.getName());
			System.out.println(field.getType());
		}

		try {
			System.out.println("annotations:" + Arrays.toString(fields[0].getAnnotations()));
			fields[0].setDouble(arc, 2.5);
			fields[1].set(arc, "hello");
			arc.runWrapper();
			System.out.println(fields[2].get(arc));
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		 */
	}
}
