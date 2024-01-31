package org.trikkle;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Stream;

public class AnnotationTest {
	@TrikkleArc(outputDatumName = "squared")
	public static double square(double toSquare) {
		return toSquare * toSquare;
	}

	@Test
	void test() {
		for (Method method : AnnotationTest.class.getDeclaredMethods()) {
			if (method.isAnnotationPresent(TrikkleArc.class)) {
				TrikkleArc annotation = method.getAnnotation(TrikkleArc.class);
				String methodName = method.getName();
				List<String> parameterNames = Stream.of(method.getParameters()).map(Parameter::getName).toList();
				System.out.println(annotation.outputDatumName());
			}
		}
	}
}
