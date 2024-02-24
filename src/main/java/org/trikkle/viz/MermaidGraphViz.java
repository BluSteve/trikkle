package org.trikkle.viz;

import org.trikkle.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A GraphViz that visualizes a graph using the Mermaid syntax.
 *
 * @since 0.1.0
 */
public class MermaidGraphViz implements GraphViz {
	private static final MermaidGraphViz mermaidGraphViz = new MermaidGraphViz();
	public final String namespace;

	public MermaidGraphViz() {
		namespace = "";
	}

	public MermaidGraphViz(String namespace) {
		this.namespace = namespace + "_";
	}

	public static String defaultVisualize(Graph... graphs) {
		return mermaidGraphViz.visualize(graphs);
	}

	private static String nodeToMermaid(Node node, String nodeId, NodeType nodeType) {
		String nodeText = node.datumNames.isEmpty() ? "<br>" : String.join("<br>", node.datumNames);
		StringBuilder sb = new StringBuilder();

		sb.append(nodeId);
		switch (nodeType) {
			case STARTING:
			case ENDING:
				sb.append("[");
				break;
			case MIDDLE:
				if (node instanceof DiscreteNode) {
					sb.append("((");
				} else if (node instanceof StreamNode) {
					sb.append("(((");
				}
				break;
		}
		sb.append(nodeText);
		switch (nodeType) {
			case STARTING:
			case ENDING:
				sb.append("]");
				break;
			case MIDDLE:
				if (node instanceof DiscreteNode) {
					sb.append("))");
				} else if (node instanceof StreamNode) {
					sb.append(")))");
				}
				break;
		}

		return sb.toString();
	}

	private static String arcToMermaid(Arc arc, String arcId) {
		return arcId + "{" + (arc.getName() == null ? arcId : arc.getName()) + "}";
	}

	private static String makeLink(Set<String> dependencyIds, String arcId, Set<String> outputNodeIds) {
		StringBuilder sb = new StringBuilder();

		String dependencyStr =
				dependencyIds.isEmpty() ? "NULL" + UUID.randomUUID() + ":::hidden" : String.join(" & ", dependencyIds);
		sb.append(dependencyStr);

		if (dependencyIds.size() > 1 || outputNodeIds.size() > 1) {
			sb.append(" --- ");
			sb.append(arcId);
			sb.append(" --> ");
		} else {
			sb.append(" -- ");
			sb.append(arcId);
			sb.append(" --> ");
		}

		String outputNodeStr =
				outputNodeIds.isEmpty() ? "NULL" + UUID.randomUUID() + ":::hidden" : String.join(" & ", outputNodeIds);
		sb.append(outputNodeStr);

		return sb.toString();
	}

	/**
	 * Visualize the given graphs using Mermaid. The done nodes (by object equivalence, not .equals()) will be colored
	 * green.
	 *
	 * @param doneNodes which nodes are done
	 * @param graphs    the graphs to visualize
	 * @return the Mermaid visualization
	 */
	public String visualize(Set<Node> doneNodes, Graph... graphs) {
		boolean single = graphs.length == 1;

		List<String> lines = new ArrayList<>();
		lines.add("flowchart LR");
		lines.add("classDef hidden display:none;");
		if (!doneNodes.isEmpty()) lines.add("classDef done fill:#90EE90;");

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

				String nodeLine = nodeToMermaid(node, nodeId, nodeType);
				if (doneNodes.contains(node)) nodeLine += ":::done";
				lines.add(nodeLine);
			}

			int k = 0;
			List<String> linkLines = new ArrayList<>();
			for (Link link : graph.links) {
				k++;
				String arcId = prefix + "arc" + k;

				Set<String> dependencyIds = link.getDependencies().stream()
						.map(nodeIdOfNode::get).collect(Collectors.toSet());
				Set<String> outputNodeIds = link.getOutputNodes().stream()
						.map(nodeIdOfNode::get).collect(Collectors.toSet());
				if (link.getDependencies().size() > 1 || link.getOutputNodes().size() > 1) {
					lines.add(arcToMermaid(link.getArc(), arcId));
					linkLines.add(makeLink(dependencyIds, arcId, outputNodeIds));
				} else {
					String arcName = link.getArc().getName() == null ? arcId : link.getArc().getName();
					linkLines.add(makeLink(dependencyIds, arcName, outputNodeIds));
				}
			}

			lines.addAll(linkLines);

			if (!single) {
				lines.add("end");
			}
		}

		return String.join(System.lineSeparator(), lines);
	}

	/**
	 * Visualize the given graphs using Mermaid without any done nodes.
	 *
	 * @param graphs the graphs to visualize
	 * @return the Mermaid visualization
	 */
	@Override
	public String visualize(Graph... graphs) {
		return visualize(Collections.emptySet(), graphs);
	}

	private enum NodeType {
		STARTING,
		MIDDLE,
		ENDING
	}
}
