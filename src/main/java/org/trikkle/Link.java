package org.trikkle;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Stream;

public class Link {
	private final Set<Node> dependencies;
	private final Arc arc;
	private final Node outputNode;

	public Link(Set<Node> dependencies, Arc arc, Node outputNode) {
		this.dependencies = dependencies;
		this.arc = arc;
		this.outputNode = outputNode;
	}

	/**
	 * Detects all static methods in the given class that are annotated with {@link TrikkleLink} and returns a map of
	 * their method names to their corresponding {@link Link}s.
	 *
	 * @param clazz Class to detect methods from.
	 * @return A map of method names to {@link Link}s.
	 */
	public static Map<String, Link> getLinks(Class<?> clazz) {
		return getLinksPrivate(clazz.getMethods(), null);
	}

	/**
	 * Detects all instance methods in the given object that are annotated with {@link TrikkleLink} and returns a map
	 * of their method names to their corresponding {@link Link}s.
	 *
	 * @param object Instance methods will be called using this object.
	 * @return A map of method names to {@link Link}s.
	 */
	public static Map<String, Link> getLinks(Object object) {
		return getLinksPrivate(object.getClass().getMethods(), object);
	}

	private static Map<String, Link> getLinksPrivate(Method[] methods, Object object) {
		Map<String, Link> links = new HashMap<>();
		for (Method method : methods) {
			if (object == null && !Modifier.isStatic(method.getModifiers())) {
				continue;
			} else if (object != null && Modifier.isStatic(method.getModifiers())) {
				continue;
			}

			if (method.isAnnotationPresent(TrikkleLink.class)) {
				TrikkleLink annotation = method.getAnnotation(TrikkleLink.class);

				String arcName = !annotation.arcName().isEmpty() ? annotation.arcName() : method.getName();
				String[] datumNames = annotation.inputDatumNames().length > 0 ?
						annotation.inputDatumNames() :
						Stream.of(method.getParameters()).map(Parameter::getName).toArray(String[]::new);

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

		return links;
	}

	public Set<Node> getDependencies() {
		return dependencies;
	}

	public Arc getArc() {
		return arc;
	}

	public Node getOutputNode() {
		return outputNode;
	}

	public boolean congruentTo(Link link) {
		return dependencies.equals(link.dependencies) && outputNode.equals(link.outputNode);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Link link = (Link) o;
		return Objects.equals(dependencies, link.dependencies) && Objects.equals(arc, link.arc) &&
				Objects.equals(outputNode, link.outputNode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dependencies, arc, outputNode);
	}
}
