package org.trikkle;

import java.util.*;

import static org.trikkle.EmptyNode.EMPTY_SET;

/**
 * A nodespace is a namespace for nodes. All nodes with the same datum names in a nodespace are the same object.
 * Useful for when you want to access the same node from different parts of your code.
 * <p>
 * The default nodespace is {@code Nodespace.}{@link Nodespace#DEFAULT}.
 * <p>
 * Named nodespaces which prefixed all its datum names with a custom string was attempted and abandoned as the
 * additional complexity was unjustified. The use case for it was intended to be when you want to combine two graphs
 * with overlapping datum names, but multiple nodespaces with separate graphs and overseers proved to be clearer. The
 * constructor {@link Overseer#Overseer(Graph, Map)} and the method {@link Overseer#fillStartingDatums(Map)} may be
 * useful in joining two overseers.
 *
 * @author Steve Cao
 * @see Node
 * @since 0.1.0
 */
public final class Nodespace {
	public final static Nodespace DEFAULT = new Nodespace();
	private final Map<Set<String>, Node> nodeCache = new HashMap<>();

	public DiscreteNode discreteOf(Set<String> datumNames) {
		if (nodeCache.containsKey(datumNames)) {
			return (DiscreteNode) nodeCache.get(datumNames);
		} else {
			DiscreteNode node = new DiscreteNode(datumNames);
			nodeCache.put(node.datumNames, node);
			return node;
		}
	}

	public DiscreteNode discreteOf(String... datumNames) {
		return discreteOf(new HashSet<>(Arrays.asList(datumNames)));
	}

	public StreamNode streamOf(String datumName) {
		Set<String> singleton = Collections.singleton(datumName);
		if (nodeCache.containsKey(singleton)) {
			return (StreamNode) nodeCache.get(singleton);
		} else {
			StreamNode node = new StreamNode(datumName);
			nodeCache.put(node.datumNames, node);
			return node;
		}
	}

	public EmptyNode emptyOf() {
		if (nodeCache.containsKey(EMPTY_SET)) {
			return (EmptyNode) nodeCache.get(EMPTY_SET);
		} else {
			EmptyNode node = new EmptyNode();
			nodeCache.put(EMPTY_SET, node);
			return node;
		}
	}

	/**
	 * Returns all nodes with the given datum name.
	 *
	 * @param datumName the datum name
	 * @return all nodes with the given datum name
	 */
	public List<Node> nodesWithDatum(String datumName) {
		List<Node> nodes = new ArrayList<>();
		for (Node node : nodeCache.values()) {
			if (node.datumNames.contains(datumName)) {
				nodes.add(node);
			}
		}
		return nodes;
	}
}
