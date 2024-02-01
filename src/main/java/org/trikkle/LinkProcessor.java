package org.trikkle;

import org.trikkle.structs.MultiHashMap;
import org.trikkle.structs.MultiMap;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class LinkProcessor {
	private final MultiMap<String, Function> functionsOfOutputNodeId = new MultiHashMap<>();
	private final Map<Function, String> outputDatumNameOfFunction = new HashMap<>();
	private Map<String, Link> links;

	/**
	 * Detects all static methods in the given class that are annotated with {@link TrikkleFunction} and returns a
	 * map of
	 * their method names to their corresponding {@link Link}s.
	 *
	 * @param clazz Class to detect methods from.
	 * @return A map of method names to {@link Link}s.
	 */
	public void addFunctionsOf(Class<?> clazz) {
		addFunctions(clazz.getMethods(), null);
	}

	/**
	 * Detects all instance methods in the given object that are annotated with {@link TrikkleFunction} and returns a
	 * map
	 * of their method names to their corresponding {@link Link}s.
	 *
	 * @param object Instance methods will be called using this object.
	 * @return A map of method names to {@link Link}s.
	 */
	public void addFunctionsOf(Object object) {
		addFunctions(object.getClass().getMethods(), object);
	}

	private void addFunctions(Method[] methods, Object object) {
		for (Method method : methods) {
			if (object == null && !Modifier.isStatic(method.getModifiers())) {
				continue;
			} else if (object != null && Modifier.isStatic(method.getModifiers())) {
				continue;
			}

			Function function = new Function(method, object);
			if (method.isAnnotationPresent(TrikkleFunction.class)) {
				TrikkleFunction trikkleFunction = method.getAnnotation(TrikkleFunction.class);
				functionsOfOutputNodeId.putOne(trikkleFunction.outputNodeId(), function);
				outputDatumNameOfFunction.put(function, trikkleFunction.outputDatumName());
			} else if (method.isAnnotationPresent(TrikkleFunctionGroup.class)) {
				TrikkleFunction[] trikkleFunctions = method.getAnnotation(TrikkleFunctionGroup.class).value();
				for (TrikkleFunction trikkleFunction : trikkleFunctions) {
					functionsOfOutputNodeId.putOne(trikkleFunction.outputNodeId(), function);
					outputDatumNameOfFunction.put(function, trikkleFunction.outputDatumName());
				}
			}
		}
	}

	public void refreshLinks() {
		links = new HashMap<>();
		for (Map.Entry<String, Set<Function>> functionEntry : functionsOfOutputNodeId.entrySet()) {
			Set<Node> inputNodes = new HashSet<>();
			Map<Function, String[]> inputDatumNamesOfFunction = new HashMap<>();
			Set<String> outputDatumNames = new HashSet<>();

			String arcName = null;
			for (Function function : functionEntry.getValue()) {
				Method method = function.method;
				TrikkleFunction annotation = method.getAnnotation(TrikkleFunction.class);

				String larcName = !annotation.arcName().isEmpty() ? annotation.arcName() : method.getName();
				if (arcName == null) {
					arcName = larcName;
				} else if (!arcName.equals(larcName)) {
					throw new IllegalArgumentException(
							"All functions with the same outputNodeId must have the same arcName");
				}

				String[] datumNames;
				if (annotation.inputDatumNames().length > 0) {
					if (annotation.inputDatumNames().length != method.getParameters().length) {
						throw new IllegalArgumentException(
								"inputDatumNames must have the same length as the method's parameters");
					}
					datumNames = annotation.inputDatumNames();
				} else {
					datumNames = Stream.of(method.getParameters()).map(Parameter::getName).toArray(String[]::new);
				}

				inputDatumNamesOfFunction.put(function, datumNames);
				inputNodes.add(new DiscreteNode(datumNames));
				outputDatumNames.add(outputDatumNameOfFunction.get(function));
			}

			Arc arc = new Arc.AutoArc() {
				@Override
				public void run() {
					try {
						for (Map.Entry<Function, String[]> entry : inputDatumNamesOfFunction.entrySet()) {
							Function function = entry.getKey();
							String[] datumNames = entry.getValue();
							Object[] datums = new Object[datumNames.length];
							for (int i = 0; i < datumNames.length; i++) {
								datums[i] = getDatum(datumNames[i]);
							}
							Object outputDatum = function.method.invoke(function.object, datums);
							returnDatum(outputDatumNameOfFunction.get(function), outputDatum);
						}
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				}
			};
			arc.name = arcName;

			Node outputNode = new DiscreteNode(outputDatumNames);
			Link link = new Link(inputNodes, arc, outputNode);
			links.put(functionEntry.getKey(), link);
		}
	}

	public Map<String, Link> getLinks() {
		return links;
	}

	public Graph getGraph() {
		return new Graph(new HashSet<>(getLinks().values()));
	}

	private static class Function {
		private final Method method;
		private final Object object;

		public Function(Method method, Object object) {
			this.method = method;
			this.object = object;
		}
	}
}
