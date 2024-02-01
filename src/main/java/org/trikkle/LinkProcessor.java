package org.trikkle;

import org.trikkle.structs.MultiHashMap;
import org.trikkle.structs.MultiMap;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class LinkProcessor {
	private final MultiMap<String, Function> functionsOfLinkId = new MultiHashMap<>();
	private Map<String, Link> links;

	/**
	 * Detects all static methods in the given class that are annotated with {@link TrikkleFunction} and returns a
	 * map of
	 * their method names to their corresponding {@link Link}s.
	 *
	 * @param clazz Class to detect methods from.
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

			if (method.isAnnotationPresent(TrikkleFunction.class)) {
				TrikkleFunction trikkleFunction = method.getAnnotation(TrikkleFunction.class);
				Function function = new Function(method, object, new TrikkleFunction[]{trikkleFunction});
				functionsOfLinkId.putOne(trikkleFunction.linkId(), function);
			} else if (method.isAnnotationPresent(TrikkleFunctionGroup.class)) {
				TrikkleFunction[] trikkleFunctions = method.getAnnotation(TrikkleFunctionGroup.class).value();
				MultiMap<String, TrikkleFunction> tfOfLinkId = new MultiHashMap<>();
				for (TrikkleFunction trikkleFunction : trikkleFunctions) {
					tfOfLinkId.putOne(trikkleFunction.linkId(), trikkleFunction);
				}

				for (Map.Entry<String, Set<TrikkleFunction>> entry : tfOfLinkId.entrySet()) {
					Function function = new Function(method, object, entry.getValue().toArray(new TrikkleFunction[0]));
					functionsOfLinkId.putOne(entry.getKey(), function);
				}
			}
		}
	}

	public void refreshLinks(String... linkIds) {
		links = new HashMap<>();
		// for each link, for each function in link, for each annotation on function.

		// this has to be for each function, then for each annotation on the function
		// then the inputDatumNames on the annotations on one function must add up to the total number of parameters
		// of the function.

		Set<String> linkIds2 = linkIds.length == 0 ? functionsOfLinkId.keySet() :
				new HashSet<>(Arrays.asList(linkIds));
		for (String linkId : linkIds2) {
			Set<Function> functions = functionsOfLinkId.get(linkId);

			Set<Node> dependencies = new HashSet<>();
			Map<Function, Map<String, List<String>>> inputsOfOutput = new HashMap<>();
			Set<String> outputDatumNames = new HashSet<>();

			for (Function function : functions) {
				Set<Node> localDependencies = new HashSet<>();
				Map<String, List<String>> mm = new HashMap<>();

				for (TrikkleFunction tf : function.annotations) {
					Node dependency = new DiscreteNode(tf.inputDatumNames());
					localDependencies.add(dependency);

					if (!mm.containsKey(tf.outputDatumName())) {
						mm.put(tf.outputDatumName(), new ArrayList<>());
					}
					for (String inputDatumName : tf.inputDatumNames()) {
						mm.get(tf.outputDatumName()).add(inputDatumName);
					}

					outputDatumNames.add(tf.outputDatumName());
				}
				inputsOfOutput.put(function, mm);
				dependencies.addAll(localDependencies);
			}

			Node outputNode = new DiscreteNode(outputDatumNames);

			Arc arc = new Arc.AutoArc() {
				@Override
				public void run() {
					try {
						for (LinkProcessor.Function function : functions) {
							Map<String, List<String>> mm = inputsOfOutput.get(function);
							for (Map.Entry<String, List<String>> stringSetEntry : mm.entrySet()) {
								List<String> inputNames = stringSetEntry.getValue();

								Object[] datums = new Object[inputNames.size()];
								int i = 0;
								for (String inputName : inputNames) {
									datums[i] = getDatum(inputName);
									i++;
								}

								Object outputDatum = function.method.invoke(function.object, datums);
								String outputName = stringSetEntry.getKey();
								returnDatum(outputName, outputDatum);
							}
						}
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				}
			};

			Link link = new Link(dependencies, arc, outputNode);
			links.put(linkId, link);
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
		private final TrikkleFunction[] annotations;

		public Function(Method method, Object object, TrikkleFunction[] annotations) {
			this.method = method;
			this.object = object;
			this.annotations = annotations;
		}
	}
}
