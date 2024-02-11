package org.trikkle;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public final class Link implements Congruent<Link> {
	private final Set<Node> dependencies;
	private final Arc arc;
	private final Set<Node> outputNodes;

	public Link(Set<Node> dependencies, Arc arc, Node outputNode) {
		this(dependencies, arc, Collections.singleton(outputNode));
	}

	// todo can a link have no outputNodes?
	public Link(Set<Node> dependencies, Arc arc, Set<Node> outputNodes) {
		if (dependencies == null) throw new NullPointerException("Dependencies cannot be null!");
		if (arc == null) throw new NullPointerException("Arc cannot be null!");
		if (outputNodes == null) throw new NullPointerException("outputNodes cannot be null!");

		boolean hasStreamNode = dependencies.stream().anyMatch(node -> node instanceof StreamNode);
		boolean autoArc = arc instanceof AutoArc;
		if (hasStreamNode && autoArc) {
			throw new IllegalArgumentException("StreamNode cannot be the input of an AutoArc. Use Arc instead.");
		}

		this.dependencies = dependencies;
		this.arc = arc;
		this.outputNodes = outputNodes;
	}

	public Set<Node> getDependencies() {
		return dependencies;
	}

	public Arc getArc() {
		return arc;
	}

	public Node getOutputNode() {
		if (outputNodes.size() > 1) throw new IllegalStateException("Link has multiple output nodes!");
		return outputNodes.iterator().next();
	}

	public Set<Node> getOutputNodes() {
		return outputNodes;
	}

	@Override
	public boolean congruentTo(Link link) {
		return Congruent.setsCongruent(dependencies, link.dependencies) &&
				Congruent.setsCongruent(outputNodes, link.outputNodes);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Link link = (Link) o;
		return Objects.equals(dependencies, link.dependencies) && Objects.equals(arc, link.arc) &&
				Objects.equals(outputNodes, link.outputNodes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dependencies, arc, outputNodes);
	}
}
