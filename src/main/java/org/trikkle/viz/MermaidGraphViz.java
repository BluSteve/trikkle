package org.trikkle.viz;

import org.trikkle.Arc;
import org.trikkle.Graph;
import org.trikkle.Node;
import org.trikkle.Todo;

import java.util.*;
import java.util.stream.Collectors;

public class MermaidGraphViz implements IGraphViz {
	private static String nodeToMermaid(Node node, String nodeId, NodeType nodeType) {
		String nodeText = String.join("<br>", node.datumNames);
		StringBuilder sb = new StringBuilder();

		sb.append(nodeId);
		switch (nodeType) {
			case STARTING:
			case ENDING:
				sb.append("[");
				break;
			case MIDDLE:
				sb.append("((");
				break;
		}
		sb.append(nodeText);
		switch (nodeType) {
			case STARTING:
			case ENDING:
				sb.append("]");
				break;
			case MIDDLE:
				sb.append("))");
				break;
		}

		return sb.toString();
	}

	private static String arcToMermaid(Arc arc, String arcId) {
		return arcId + "{" + (arc.name == null ? arcId : arc.name) + "}";
	}

	private static String makeLink(Set<String> dependencyIds, String arcId, String outputNodeId) {
		StringBuilder sb = new StringBuilder();

		String dependencyStr = dependencyIds.isEmpty() ? "NULL" : String.join(" & ", dependencyIds);
		sb.append(dependencyStr);

		if (dependencyIds.size() > 1) {
			sb.append(" --- ");
			sb.append(arcId);
			sb.append(" --> ");
		}
		else {
			sb.append(" -- ");
			sb.append(arcId);
			sb.append(" --> ");
		}

		sb.append(outputNodeId);

		return sb.toString();
	}

	@Override
	public String visualize(Graph graph) {
		Map<Node, String> nodeIdOfNode = new HashMap<>();

		List<String> lines = new ArrayList<>();
		lines.add("flowchart LR");

		int i = 0;
		for (Node node : graph.getNodes()) {
			i++;
			String nodeId = "node" + i;
			nodeIdOfNode.put(node, nodeId);

			NodeType nodeType = NodeType.MIDDLE;
			if (graph.getStartingNodes().contains(node)) {
				nodeType = NodeType.STARTING;
			}
			else if (graph.getEndingNodes().contains(node)) {
				nodeType = NodeType.ENDING;
			}

			lines.add(nodeToMermaid(node, nodeId, nodeType));
		}

		int k = 0;
		List<String> linkLines = new ArrayList<>();
		for (Todo todo : graph.getTodos()) {
			k++;
			String arcId = "arc" + k;

			// todo what happens when no dependencies?
			Set<String> dependencyIds = todo.getDependencies().stream()
					.map(nodeIdOfNode::get).collect(Collectors.toSet());
			if (todo.getDependencies().size() > 1) {
				lines.add(arcToMermaid(todo.getArc(), arcId));
				linkLines.add(makeLink(dependencyIds, arcId, nodeIdOfNode.get(todo.getOutputNode())));
			}
			else {
				String arcName = todo.getArc().name == null ? arcId : todo.getArc().name;
				linkLines.add(makeLink(dependencyIds, arcName, nodeIdOfNode.get(todo.getOutputNode())));
			}
		}

		lines.addAll(linkLines);

		return String.join("\n    ", lines);
	}

	private enum NodeType {
		STARTING,
		MIDDLE,
		ENDING
	}
}
