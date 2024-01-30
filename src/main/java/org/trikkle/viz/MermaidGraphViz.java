package org.trikkle.viz;

import org.trikkle.Arc;
import org.trikkle.Graph;
import org.trikkle.Link;
import org.trikkle.Node;

import java.util.*;
import java.util.stream.Collectors;

public class MermaidGraphViz implements IGraphViz {
	public final String namespace;

	public MermaidGraphViz() {
		namespace = "";
	}

	public MermaidGraphViz(String namespace) {
		this.namespace = namespace + "_";
	}

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

		String dependencyStr =
				dependencyIds.isEmpty() ? "NULL" + arcId + ":::hidden" : String.join(" & ", dependencyIds);
		sb.append(dependencyStr);

		if (dependencyIds.size() > 1) {
			sb.append(" --- ");
			sb.append(arcId);
			sb.append(" --> ");
		} else {
			sb.append(" -- ");
			sb.append(arcId);
			sb.append(" --> ");
		}

		sb.append(outputNodeId);

		return sb.toString();
	}

	@Override
	public String visualize(Graph... graphs) {
		boolean single = graphs.length == 1;

		List<String> lines = new ArrayList<>();
		lines.add("flowchart LR");
		lines.add("classDef hidden display: none;");

		int I = 0;
		for (int j = graphs.length - 1; j >= 0; j--) {
			Graph graph = graphs[j];
			I++;

			String prefix;
			if (!single) {
				String graphId = namespace + "graph" + I;
				lines.add("subgraph " + graphId);
				lines.add("direction LR");
				prefix = graphId + "_";
			} else {
				prefix = namespace;
			}

			Map<Node, String> nodeIdOfNode = new HashMap<>();
			int i = 0;
			for (Node node : graph.nodes) {
				i++;
				String nodeId = prefix + "node" + i;
				nodeIdOfNode.put(node, nodeId);

				NodeType nodeType = NodeType.MIDDLE;
				if (graph.startingNodes.contains(node)) {
					nodeType = NodeType.STARTING;
				} else if (graph.endingNodes.contains(node)) {
					nodeType = NodeType.ENDING;
				}

				lines.add(nodeToMermaid(node, nodeId, nodeType));
			}

			int k = 0;
			List<String> linkLines = new ArrayList<>();
			for (Link link : graph.links) {
				k++;
				String arcId = prefix + "arc" + k;

				Set<String> dependencyIds = link.getDependencies().stream()
						.map(nodeIdOfNode::get).collect(Collectors.toSet());
				if (link.getDependencies().size() > 1) {
					lines.add(arcToMermaid(link.getArc(), arcId));
					linkLines.add(makeLink(dependencyIds, arcId, nodeIdOfNode.get(link.getOutputNode())));
				} else {
					String arcName = link.getArc().name == null ? arcId : link.getArc().name;
					linkLines.add(makeLink(dependencyIds, arcName, nodeIdOfNode.get(link.getOutputNode())));
				}
			}

			lines.addAll(linkLines);

			if (!single) {
				lines.add("end");
			}
		}

		return String.join(System.lineSeparator(), lines);
	}

	private enum NodeType {
		STARTING,
		MIDDLE,
		ENDING
	}
}
