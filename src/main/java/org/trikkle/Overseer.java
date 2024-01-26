package org.trikkle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Overseer {
	private final Map<IBitmask, Todo> todos = new HashMap<>(); // hardcode ArrayBitmask for now.
	private final Map<String, Object> cache = new HashMap<>();
	private List<Node> startNodes, endNodes;
	private Node[] nodes;
	private Arc[] arcs;

	public void ticktock() {

	}

	public void addTodo(Todo todo) {

	}

	public Node getOutputNode(Arc arc) {
		return null;
	}



}
