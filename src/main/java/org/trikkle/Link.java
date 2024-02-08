package org.trikkle;

import java.util.Objects;
import java.util.Set;

public final class Link {
	private final Set<Node> dependencies;
	private final Arc arc;
	private final Node outputNode;

	public Link(Set<Node> dependencies, Arc arc, Node outputNode) {
		if (dependencies == null) throw new NullPointerException("Dependencies cannot be null!");
		if (arc == null) throw new NullPointerException("Arc cannot be null!");
		if (outputNode == null) throw new NullPointerException("Output Node cannot be null!");

		boolean hasStreamNode = dependencies.stream().anyMatch(node -> node instanceof StreamNode);
		boolean autoArc = arc instanceof AutoArc;
		if (hasStreamNode && autoArc) {
			throw new IllegalArgumentException("StreamNode cannot be the input of an AutoArc. Use Arc instead.");
		}

		this.dependencies = dependencies;
		this.arc = arc;
		this.outputNode = outputNode;
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
