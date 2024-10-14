package org.trikkle;

import java.util.Objects;
import java.util.Set;

/**
 * Analogous to an adjacency list entry in a graph, except there are multiple input nodes and output nodes.
 * It is actually a directed hypergraph.
 *
 * @see Node
 * @see Arc
 * @see Graph
 * @since 0.1.0
 */
public final class Link implements Congruent<Link> {
	private Set<Node> inputNodes, outputNodes;
	private Arc arc;

	/**
	 * Create a link with the given input nodes, arc, and output nodes.
	 *
	 * @param inputNodes the dependency nodes of the link
	 * @param arc          the arc of the link
	 * @param outputNodes  the output nodes of the link
	 * @throws NullPointerException     if input nodes, arc, or outputNodes is null
	 * @throws IllegalArgumentException if a StreamNode is the input of an AutoArc
	 */
	public Link(Set<Node> inputNodes, Arc arc, Set<Node> outputNodes) {
		if (arc == null) throw new NullPointerException("Arc cannot be null!");

		if (inputNodes != null) {
			boolean hasStreamNode = inputNodes.stream().anyMatch(node -> node instanceof StreamNode);
			boolean autoArc = arc instanceof AutoArc;
			if (hasStreamNode && autoArc) {
				throw new IllegalArgumentException("StreamNode cannot be the input of an AutoArc. Use Arc instead.");
			}
		}

		this.inputNodes = inputNodes;
		this.arc = arc;
		this.outputNodes = outputNodes;
	}

	/**
	 * Copy constructor. The input nodes, arc, and output nodes are copied from the link.
	 *
	 * @param link the link to copy
	 */
	public Link(Link link) {
		this(link.inputNodes, link.arc, link.outputNodes);
	}

	/**
	 * Check if the link is runnable. A link is runnable if it has no input nodes OR all of its input nodes are usable,
	 * AND the arc is not yet finished.
	 *
	 * @return true if the link is runnable
	 */
	public boolean runnable() {
		if (arc.getStatus() == ArcStatus.FINISHED) return false;
		for (Node dependency : inputNodes) {
			if (!dependency.isUsable()) {
				return false;
			}
		}
		return true;
	}

	public Set<Node> getInputNodes() {
		return inputNodes;
	}

	public void setInputNodes(Set<Node> inputNodes) {
		this.inputNodes = inputNodes;
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
		if (outputNodes == null) throw new NullPointerException("outputNodes cannot be null!");
		this.outputNodes = outputNodes;
	}

	/**
	 * Two links are congruent if they have congruent input nodes and output nodes.
	 *
	 * @param link the link to compare to
	 * @return true if the links are congruent
	 */
	@Override
	public boolean congruentTo(Link link) {
		return Congruent.setsCongruent(inputNodes, link.inputNodes) &&
				Congruent.setsCongruent(outputNodes, link.outputNodes);
	}

	/**
	 * Two links are equal if they have the same input nodes, arc, and output nodes.
	 *
	 * @param o the object to compare to
	 * @return true if the links are equal
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Link link = (Link) o;
		return Objects.equals(inputNodes, link.inputNodes) && Objects.equals(arc, link.arc) &&
				Objects.equals(outputNodes, link.outputNodes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(inputNodes, arc, outputNodes);
	}

	@Override
	public String toString() {
		return inputNodes + " -> " + arc + " -> " + outputNodes;
	}
}
