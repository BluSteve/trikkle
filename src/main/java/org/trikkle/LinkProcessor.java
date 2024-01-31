package org.trikkle;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Stream;

public class LinkProcessor {
	public final Map<String, Link> links = new HashMap<>();

	/**
	 * Detects all static methods in the given class that are annotated with {@link TrikkleLink} and returns a map of
	 * their method names to their corresponding {@link Link}s.
	 *
	 * @param clazz Class to detect methods from.
	 * @return A map of method names to {@link Link}s.
	 */
	public void addLinks(Class<?> clazz) {
		addLinksPrivate(clazz.getMethods(), null);
	}

	/**
	 * Detects all instance methods in the given object that are annotated with {@link TrikkleLink} and returns a map
	 * of their method names to their corresponding {@link Link}s.
	 *
	 * @param object Instance methods will be called using this object.
	 * @return A map of method names to {@link Link}s.
	 */
	public void addLinks(Object object) {
		addLinksPrivate(object.getClass().getMethods(), object);
	}

	private void addLinksPrivate(Method[] methods, Object object) {
		for (Method method : methods) {
			if (object == null && !Modifier.isStatic(method.getModifiers())) {
				continue;
			} else if (object != null && Modifier.isStatic(method.getModifiers())) {
				continue;
			}

			if (method.isAnnotationPresent(TrikkleLink.class)) {
				TrikkleLink annotation = method.getAnnotation(TrikkleLink.class);

				String arcName = !annotation.arcName().isEmpty() ? annotation.arcName() : method.getName();
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

				Arc arc = new Arc.AutoArc() {
					@Override
					public void run() {
						try {
							Object[] datums = new Object[datumNames.length];
							for (int i = 0; i < datumNames.length; i++) {
								datums[i] = getDatum(datumNames[i]);
							}
							Object outputDatum = method.invoke(object, datums);
							returnDatum(annotation.outputDatumName(), outputDatum);
						} catch (ReflectiveOperationException e) {
							throw new RuntimeException(e);
						}
					}
				};
				arc.name = arcName;

				Node inputNode = new DiscreteNode(datumNames);
				Node outputNode = new DiscreteNode(annotation.outputDatumName());
				Link link = new Link(Collections.singleton(inputNode), arc, outputNode);
				links.put(method.getName(), link);
			}
		}
	}

	public Graph getGraph() {
		return new Graph(new HashSet<>(links.values()));
	}
}
