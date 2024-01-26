package org.trikkle;

import java.util.List;

public class Todo implements IGraph {
	private List<Node> dependencies;
	private Arc arc;
	private Node outputNode;

	public Todo(List<Node> dependencies, Arc arc, Node outputNode) {
		this.dependencies = dependencies;
		this.arc = arc;
		this.outputNode = outputNode;
	}

	public List<Node> getDependencies() {
		return dependencies;
	}

	public Arc getArc() {
		return arc;
	}

	public Node getOutputNode() {
		return outputNode;
	}
}
