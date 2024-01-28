package org.trikkle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Graph {
	private final Set<Todo> todos;

	private final Set<Node> nodes;
	private final Set<Arc> arcs;
	private final Set<Node> startingNodes;
	private final Set<Node> endingNodes;

	public Graph(Set<Todo> todos, Set<Node> startingNodes, Set<Node> endingNodes) {
		this.todos = todos;

		nodes = new HashSet<>();
		arcs = new HashSet<>();
		for (Todo todo : todos) {
			nodes.addAll(todo.getDependencies());
			arcs.add(todo.getArc());
			nodes.add(todo.getOutputNode());
		}

		for (Node node : startingNodes) {
			if (!nodes.contains(node)) {
				throw new IllegalArgumentException("Starting Node not part of Graph!");
			}
		}
		for (Node node : endingNodes) {
			if (!nodes.contains(node)) {
				throw new IllegalArgumentException("Ending Node not part of Graph!");
			}
		}

		this.startingNodes = startingNodes;
		this.endingNodes = endingNodes;
	}

	public static Graph mergeGraphs(List<Graph> graphs, Set<Node> startingNodes, Set<Node> endingNodes) {
		return null;
	}

	public Set<Todo> getTodos() {
		return todos;
	}

	public Set<Node> getNodes() {
		return nodes;
	}

	public Set<Arc> getArcs() {
		return arcs;
	}

	public Set<Node> getStartingNodes() {
		return startingNodes;
	}

	public Set<Node> getEndingNodes() {
		return endingNodes;
	}
}
