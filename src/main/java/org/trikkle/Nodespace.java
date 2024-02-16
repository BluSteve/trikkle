package org.trikkle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
 * <p>
 * This class is thread-safe.
 *
 * @see Node
 * @since 0.1.0
 */
public final class Nodespace {
	public final static Nodespace DEFAULT = new Nodespace();
	public final Map<Set<String>, Node> nodeStore = new ConcurrentHashMap<>();

	/**
	 * Returns a {@link DiscreteNode} with the given datum names. If the node does not exist, it is created and added to
	 * the
	 * {@link Nodespace#nodeStore}.
	 *
	 * @param datumNames the datum names
	 * @see DiscreteNode#DiscreteNode(Set)
	 */
	public DiscreteNode discreteOf(Set<String> datumNames) {
		if (nodeStore.containsKey(datumNames)) {
			return (DiscreteNode) nodeStore.get(datumNames);
		} else {
			DiscreteNode node = new DiscreteNode(datumNames);
			nodeStore.put(node.datumNames, node);
			return node;
		}
	}

	/**
	 * Returns a {@link DiscreteNode} with the given datum names. If the node does not exist, it is created and added to
	 * the
	 * {@link Nodespace#nodeStore}.
	 *
	 * @param datumNames the datum names
	 * @see DiscreteNode#DiscreteNode(Set)
	 */
	public DiscreteNode discreteOf(String... datumNames) {
		return discreteOf(new HashSet<>(Arrays.asList(datumNames)));
	}

	/**
	 * Returns a {@link StreamNode} with the given datum name. If the node does not exist, it is created and added to the
	 * {@link Nodespace#nodeStore}.
	 *
	 * @param datumName the datum name
	 * @see StreamNode#StreamNode(String)
	 */
	public StreamNode streamOf(String datumName) {
		Set<String> singleton = Collections.singleton(datumName);
		if (nodeStore.containsKey(singleton)) {
			return (StreamNode) nodeStore.get(singleton);
		} else {
			StreamNode node = new StreamNode(datumName);
			nodeStore.put(node.datumNames, node);
			return node;
		}
	}

	/**
	 * Returns an {@link EmptyNode}. If the node does not exist, it is created and added to the
	 * {@link Nodespace#nodeStore}.
	 *
	 * @see EmptyNode#EmptyNode()
	 */
	public EmptyNode emptyOf() {
		if (nodeStore.containsKey(EMPTY_SET)) {
			return (EmptyNode) nodeStore.get(EMPTY_SET);
		} else {
			EmptyNode node = new EmptyNode();
			nodeStore.put(EMPTY_SET, node);
			return node;
		}
	}

	/**
	 * Adds the given node to the {@link Nodespace#nodeStore} based on the node's datum names.
	 *
	 * @param node the node
	 */
	public void add(Node node) {
		nodeStore.put(node.datumNames, node);
	}

	/**
	 * Adds all the given nodes to the {@link Nodespace#nodeStore} based on the node's datum names.
	 *
	 * @param nodes the nodes
	 */
	public void addAll(Collection<Node> nodes) {
		for (Node node : nodes) {
			nodeStore.put(node.datumNames, node);
		}
	}

	/**
	 * Removes the given node from the {@link Nodespace#nodeStore} based on the node's datum names. The node given and
	 * the node removed may not necessarily be the same object.
	 *
	 * @param node the node
	 */
	public void remove(Node node) {
		nodeStore.remove(node.datumNames);
	}

	/**
	 * Removes all the given nodes from the {@link Nodespace#nodeStore} based on the node's datum names. The node given
	 * and the node removed may not necessarily be the same object.
	 *
	 * @param nodes the nodes
	 */
	public void removeAll(Collection<Node> nodes) {
		for (Node node : nodes) {
			nodeStore.remove(node.datumNames);
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
		for (Node node : nodeStore.values()) {
			if (node.datumNames.contains(datumName)) {
				nodes.add(node);
			}
		}
		return nodes;
	}
}
