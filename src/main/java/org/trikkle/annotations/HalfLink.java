package org.trikkle.annotations;

import org.trikkle.*;

import java.util.*;

/**
 * A link that has not yet been connected to its dependencies. Dependencies may be inferred through
 * the method {@link #toFullLinks(List)}, where HalfLinks are converted into full links by using each other as context.
 *
 * @see Link
 * @since 0.1.0
 */
public final class HalfLink {
	private Arc arc;
	private Set<Node> outputNodes;

	private Set<Node> dependencies;

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
	 * Create a half link with the given arc. The output node is generated from {@link Arc#getOutputFields()}.
	 *
	 * @param arc the arc of the half link, should have annotations {@link Input} and {@link Output}
	 * @see Link#Link(Arc)
	 * @see Arc#getOutputFields()
	 */
	public HalfLink(Arc arc) {
		this(arc, Collections.singleton(arc.getOutputFields().keySet().isEmpty() ? new EmptyNode() :
				new DiscreteNode(new HashSet<>(arc.getOutputFields().keySet()))));
	}

	/**
	 * Create a half link with the given link. The dependencies, arc and output nodes are copied from the link.
	 *
	 * @param link the link to copy
	 */
	public HalfLink(Link link) {
		this(link.getArc(), link.getOutputNodes());
		dependencies = link.getDependencies();
	}

	/**
	 * Converts a list of half links into a list of full links by using one another as context. For every HalfLink, all
	 * starting datums (i.e ., those that are not returned by any other half links in the list) will be combined into a
	 * single starting {@link DiscreteNode}. Any half link that already has dependencies manually set will be added to
	 * the list as is.
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
			if (halfLink.dependencies != null) {
				fullLinks.add(new Link(halfLink.dependencies, halfLink.arc, halfLink.outputNodes));
				continue;
			}

			Set<Node> dependencies = new HashSet<>();
			Set<String> danglingInputs = new HashSet<>();
			for (String inputName : halfLink.arc.getInputFields().keySet()) {
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

	public Set<Node> getOutputNodes() {
		return outputNodes;
	}

	public void setOutputNodes(Set<Node> outputNodes) {
		this.outputNodes = outputNodes;
	}

	public Set<Node> getDependencies() {
		return dependencies;
	}

	/**
	 * Optionally manually set dependencies
	 *
	 * @param dependencies the dependencies of the HalfLink
	 */
	public void setDependencies(Set<Node> dependencies) {
		this.dependencies = dependencies;
	}

	@Override
	public String toString() {
		if (dependencies == null) {
			return arc + " -> " + outputNodes;
		}
		return dependencies + " -> " + arc + " -> " + outputNodes;
	}
}
