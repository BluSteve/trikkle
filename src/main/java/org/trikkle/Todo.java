package org.trikkle;

import java.util.List;
import java.util.Set;

public class Todo implements IGraph {
	private Set<Node> dependencies;
	private Arc arc;
	private Node outputNode;

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

	@Override
	public String toString() {
		return "Todo{" +
				"dependencies=" + dependencies +
				", arc=" + arc +
				", outputNode=" + outputNode +
				'}';
	}
}
