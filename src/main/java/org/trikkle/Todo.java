package org.trikkle;

import java.util.Objects;
import java.util.Set;

public class Todo {
	private final Set<Node> dependencies;
	private final Arc arc;
	private final Node outputNode;

	public Todo(Set<Node> dependencies, Arc arc, Node outputNode) {
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

	public boolean congruentTo(Todo todo) {
		return dependencies.equals(todo.dependencies) && outputNode.equals(todo.outputNode);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Todo todo = (Todo) o;
		return Objects.equals(dependencies, todo.dependencies) && Objects.equals(arc, todo.arc) &&
				Objects.equals(outputNode, todo.outputNode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dependencies, arc, outputNode);
	}

	@Override
	public String toString() {
		return "Todo{" +
				"dependencies=" + dependencies +
				", arc=" + arc +
				", outputNode=" + outputNode +
				'}';
	}
}
