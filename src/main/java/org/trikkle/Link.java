package org.trikkle;

import org.trikkle.annotations.Input;
import org.trikkle.annotations.Output;

import java.util.Collections;
import java.util.HashSet;
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
public final class Link extends HalfLink implements Congruent<Link> {
	private final Set<Node> dependencies;

	/**
	 * Create a link with the given dependencies, arc, and output nodes.
	 *
	 * @param dependencies the dependency nodes of the link
	 * @param arc          the arc of the link
	 * @param outputNodes  the output nodes of the link
	 * @throws NullPointerException     if dependencies, arc, or outputNodes is null
	 * @throws IllegalArgumentException if a StreamNode is the input of an AutoArc
	 */
	public Link(Set<Node> dependencies, Arc arc, Set<Node> outputNodes) {
		super(arc, outputNodes);
		if (dependencies == null) throw new NullPointerException("Dependencies cannot be null!");

		boolean hasStreamNode = dependencies.stream().anyMatch(node -> node instanceof StreamNode);
		boolean autoArc = arc instanceof AutoArc;
		if (hasStreamNode && autoArc) {
			throw new IllegalArgumentException("StreamNode cannot be the input of an AutoArc. Use Arc instead.");
		}

		this.dependencies = dependencies;
	}

	public Link(Set<Node> dependencies, Arc arc, Node outputNode) {
		this(dependencies, arc, Collections.singleton(outputNode));
	}

	/**
	 * Create a link with the given arc. The dependency and output node are automatically generated from the arc via
	 * annotations. All input datums will be added to a single dependency node, and all output datums will be added to a
	 * single output node. The nodes created will either be a {@link DiscreteNode} or an {@link EmptyNode}, if there are
	 * no datums.
	 *
	 * @param arc the arc of the link, should have annotations {@link Input} and {@link Output}
	 * @see Input
	 * @see Output
	 * @see DiscreteNode
	 * @see EmptyNode
	 */
	public Link(Arc arc) {
		super(arc, new HashSet<>());
		dependencies = new HashSet<>();

		Set<String> inputNames = new HashSet<>(arc.inputFields.keySet());
		Set<String> outputNames = new HashSet<>(arc.outputFields.keySet());

		dependencies.add(inputNames.isEmpty() ? new EmptyNode() : new DiscreteNode(inputNames));
		outputNodes.add(outputNames.isEmpty() ? new EmptyNode() : new DiscreteNode(outputNames));
	}

	/**
	 * Check if the link is runnable. A link is runnable if all of its dependencies are usable and the arc is not yet
	 * finished.
	 *
	 * @return true if the link is runnable
	 */
	public boolean runnable() {
		if (arc.getStatus() == ArcStatus.FINISHED) return false;
		for (Node dependency : dependencies) {
			if (!dependency.isUsable()) {
				return false;
			}
		}
		return true;
	}

	public Set<Node> getDependencies() {
		return dependencies;
	}

	public Arc getArc() {
		return arc;
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

	@Override
	public boolean congruentTo(Link link) {
		return Congruent.setsCongruent(dependencies, link.dependencies) &&
				Congruent.setsCongruent(outputNodes, link.outputNodes);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Link link = (Link) o;
		return Objects.equals(dependencies, link.dependencies) && Objects.equals(arc, link.arc) &&
				Objects.equals(outputNodes, link.outputNodes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dependencies, arc, outputNodes);
	}

	@Override
	public String toString() {
		return dependencies + " -> " + arc + " -> " + outputNodes;
	}
}
