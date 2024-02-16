package org.trikkle;

import java.util.*;

/**
 * A {@link Link} that has not yet been connected to its dependencies. Dependencies may be inferred through
 * the method {@link #toFullLinks(List)}, where HalfLinks are converted into full links by using each other as context.
 *
 * @see Link
 * @since 0.1.0
 */
public class HalfLink {
	protected Arc arc;
	protected Set<Node> outputNodes;

	/**
	 * Create a half link with the given arc and output nodes.
	 *
	 * @param arc         the arc of the link
	 * @param outputNodes the output nodes of the link
	 * @throws NullPointerException if arc or outputNodes is null
	 */
	public HalfLink(Arc arc, Set<Node> outputNodes) {
		if (arc == null) throw new NullPointerException("Arc cannot be null!");
		if (outputNodes == null) throw new NullPointerException("outputNodes cannot be null!");
		this.arc = arc;
		this.outputNodes = outputNodes;
	}

	public HalfLink(Arc arc, Node outputNode) {
		this(arc, Collections.singleton(outputNode));
	}

	/**
	 * Converts a list of half links into a list of full links by using one another as context. All starting datums (i.e
	 * ., those that are not returned by any other half links in the list) will be combined into a single starting
	 * {@link DiscreteNode}. Any half link that is already a full link will be added to the list as is.
	 *
	 * @param halfLinks the half links to convert
	 * @return a list of full links
	 * @see Link
	 * @see DiscreteNode
	 */
	public static List<Link> toFullLinks(List<HalfLink> halfLinks) {
		Map<String, Node> nodeOfDatum = new HashMap<>();
		for (HalfLink halfLink : halfLinks) {
			for (Node outputNode : halfLink.outputNodes) {
				for (String datumName : outputNode.datumNames) {
					Node rvalue = nodeOfDatum.put(datumName, outputNode);
					if (rvalue != null) {
						throw new IllegalArgumentException("Datum name " + datumName + " is already declared by " + rvalue);
					}
				}
			}
		}

		List<Link> fullLinks = new ArrayList<>(halfLinks.size());
		for (HalfLink halfLink : halfLinks) {
			if (halfLink instanceof Link) {
				fullLinks.add((Link) halfLink);
				continue;
			}

			Set<Node> dependencies = new HashSet<>();
			Set<String> danglingInputs = new HashSet<>();
			for (String inputName : halfLink.arc.inputFields.keySet()) {
				if (!nodeOfDatum.containsKey(inputName)) {
					danglingInputs.add(inputName);
					continue;
				}
				dependencies.add(nodeOfDatum.get(inputName));
			}

			if (!danglingInputs.isEmpty()) {
				Node startingNode = new DiscreteNode(danglingInputs); // combines all danglingInputs into one starting node
				dependencies.add(startingNode);
			}

			fullLinks.add(new Link(dependencies, halfLink.arc, halfLink.outputNodes));
		}

		return fullLinks;
	}

	public Arc getArc() {
		return arc;
	}

	public void setArc(Arc arc) {
		this.arc = arc;
	}

	/**
	 * Convenience method to get the single output node of the link. If there are multiple output nodes, an exception is
	 * thrown.
	 *
	 * @return the output node of the link
	 * @throws IllegalStateException if there are multiple output nodes
	 */
	public Node getOutputNode() {
		if (outputNodes.size() > 1) throw new IllegalStateException("Link has multiple output nodes!");
		return outputNodes.iterator().next();
	}

	public Set<Node> getOutputNodes() {
		return outputNodes;
	}

	public void setOutputNodes(Set<Node> outputNodes) {
		this.outputNodes = outputNodes;
	}
}
