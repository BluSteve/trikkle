package org.trikkle.annotation;

import org.trikkle.*;
import org.trikkle.annotation.TrikkleFunction;
import org.trikkle.annotation.TrikkleFunctionGroup;
import org.trikkle.structs.MultiHashMap;
import org.trikkle.structs.MultiMap;

import java.lang.reflect.Modifier;
import java.util.*;

public class LinkProcessor {
	private final MultiMap<String, Method> methodsOfLinkId = new MultiHashMap<>();
	private Map<String, Link> links;

	/**
	 * Detects all static methods in the given class that are annotated with {@link TrikkleFunction} and returns a
	 * map of
	 * their method names to their corresponding {@link Link}s.
	 *
	 * @param clazz Class to detect methods from.
	 */
	public void addMethodsOf(Class<?> clazz) {
		addMethods(clazz.getMethods(), null);
	}

	/**
	 * Detects all instance methods in the given object that are annotated with {@link TrikkleFunction} and returns a
	 * map
	 * of their method names to their corresponding {@link Link}s.
	 *
	 * @param object Instance methods will be called using this object.
	 */
	public void addMethodsOf(Object object) {
		addMethods(object.getClass().getMethods(), object);
	}

	private void addMethods(java.lang.reflect.Method[] jmethods, Object object) {
		for (java.lang.reflect.Method jmethod : jmethods) {
			if (object == null && !Modifier.isStatic(jmethod.getModifiers())) {
				continue;
			} else if (object != null && Modifier.isStatic(jmethod.getModifiers())) {
				continue;
			}

			if (jmethod.isAnnotationPresent(TrikkleFunction.class)) {
				TrikkleFunction trikkleFunction = jmethod.getAnnotation(TrikkleFunction.class);
				Method method = new Method(jmethod, object, new TrikkleFunction[]{trikkleFunction});
				methodsOfLinkId.putOne(trikkleFunction.linkId(), method);
			} else if (jmethod.isAnnotationPresent(TrikkleFunctionGroup.class)) {
				TrikkleFunction[] trikkleFunctions = jmethod.getAnnotation(TrikkleFunctionGroup.class).value();
				Map<String, List<TrikkleFunction>> tfOfLinkId = new HashMap<>();
				for (TrikkleFunction trikkleFunction : trikkleFunctions) {
					if (!tfOfLinkId.containsKey(trikkleFunction.linkId())) {
						tfOfLinkId.put(trikkleFunction.linkId(), new ArrayList<>());
					}

					tfOfLinkId.get(trikkleFunction.linkId()).add(trikkleFunction);
				}

				for (Map.Entry<String, List<TrikkleFunction>> entry : tfOfLinkId.entrySet()) {
					// check that all tfs in the group have the same outputDatumName
					String outputDatumName = null;
					for (TrikkleFunction tf : entry.getValue()) {
						if (outputDatumName == null) {
							outputDatumName = tf.output();
						} else if (!outputDatumName.equals(tf.output())) {
							throw new IllegalArgumentException(
									"All TrikkleFunctions with the same linkId must have the same output!");
						}
					}

					Method method = new Method(jmethod, object, entry.getValue().toArray(new TrikkleFunction[0]));
					methodsOfLinkId.putOne(entry.getKey(), method);
				}
			}
		}
	}

	public void refreshLinks(String... linkIds) {
		links = new HashMap<>();
		// for each link, for each method in link, for each annotation on method.

		// this has to be for each method, then for each annotation on the method
		// then the inputDatumNames on the annotations on one method must add up to the total number of parameters
		// of the method.

		Set<String> linkIds2 = linkIds.length == 0 ? methodsOfLinkId.keySet() :
				new HashSet<>(Arrays.asList(linkIds));
		for (String linkId : linkIds2) {
			Set<Method> methods = methodsOfLinkId.get(linkId);

			Set<Node> dependencies = new HashSet<>();
			Map<Method, Map<String, List<String>>> inputsOfOutput = new HashMap<>();
			Set<String> outputDatumNames = new HashSet<>();

			for (Method method : methods) {
				Set<Node> localDependencies = new HashSet<>();
				Map<String, List<String>> mm = new HashMap<>();

				for (TrikkleFunction tf : method.annotations) {
					Node dependency = DiscreteNode.of(tf.inputs());
					localDependencies.add(dependency);

					if (!mm.containsKey(tf.output())) {
						mm.put(tf.output(), new ArrayList<>());
					}
					for (String inputDatumName : tf.inputs()) {
						mm.get(tf.output()).add(inputDatumName);
					}

					outputDatumNames.add(tf.output());
				}
				inputsOfOutput.put(method, mm);
				dependencies.addAll(localDependencies);
			}

			Node outputNode = DiscreteNode.of(outputDatumNames);

			Arc arc = new AutoArc() {
				@Override
				public void run() {
					try {
						for (Method method : methods) {
							Map<String, List<String>> mm = inputsOfOutput.get(method);
							for (Map.Entry<String, List<String>> stringSetEntry : mm.entrySet()) {
								List<String> inputNames = stringSetEntry.getValue();

								Object[] datums = new Object[inputNames.size()];
								int i = 0;
								for (String inputName : inputNames) {
									datums[i] = getDatum(inputName);
									i++;
								}

								Object outputDatum = method.jmethod.invoke(method.object, datums);
								String outputName = stringSetEntry.getKey();
								returnDatum(outputName, outputDatum);
							}
						}
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				}
			};
			arc.setName(linkId);

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

	private static class Method {
		private final java.lang.reflect.Method jmethod;
		private final Object object;
		private final TrikkleFunction[] annotations;

		public Method(java.lang.reflect.Method jmethod, Object object, TrikkleFunction[] annotations) {
			this.jmethod = jmethod;
			this.object = object;
			this.annotations = annotations;
		}
	}
}
