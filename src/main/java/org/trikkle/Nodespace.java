package org.trikkle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.trikkle.EmptyNode.EMPTY_SET;

/**
 * A nodespace is a namespace for nodes. All nodes with the same pointers in a nodespace are the same object.
 * Useful for when you want to access the same node from different parts of your code.
 * <p>
 * The default nodespace is {@code Nodespace.}{@link Nodespace#DEFAULT}.
 * <p>
 * Named nodespaces which prefixed all its pointers with a custom string was attempted and abandoned as the
 * additional complexity was unjustified. The use case for it was intended to be when you want to combine two graphs
 * with overlapping pointers, but multiple nodespaces with separate graphs and overseers proved to be clearer. The
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
	 * Returns a {@link DiscreteNode} with the given pointers. If the node does not exist, it is created and added to
	 * the
	 * {@link Nodespace#nodeStore}.
	 *
	 * @param pointers the pointers
	 * @see DiscreteNode#DiscreteNode(Set)
	 */
	public DiscreteNode discreteOf(Set<String> pointers) {
		if (nodeStore.containsKey(pointers)) {
			return (DiscreteNode) nodeStore.get(pointers);
		} else {
			DiscreteNode node = new DiscreteNode(pointers);
			nodeStore.put(node.pointers, node);
			return node;
		}
	}

	/**
	 * Returns a {@link DiscreteNode} with the given pointers. If the node does not exist, it is created and added to
	 * the
	 * {@link Nodespace#nodeStore}.
	 *
	 * @param pointers the pointers
	 * @see DiscreteNode#DiscreteNode(Set)
	 */
	public DiscreteNode discreteOf(String... pointers) {
		return discreteOf(new HashSet<>(Arrays.asList(pointers)));
	}

	/**
	 * Returns a {@link StreamNode} with the given pointer. If the node does not exist, it is created and added to the
	 * {@link Nodespace#nodeStore}.
	 *
	 * @param pointer the pointer
	 * @see StreamNode#StreamNode(String)
	 */
	public StreamNode streamOf(String pointer) {
		Set<String> singleton = Collections.singleton(pointer);
		if (nodeStore.containsKey(singleton)) {
			return (StreamNode) nodeStore.get(singleton);
		} else {
			StreamNode node = new StreamNode(pointer);
			nodeStore.put(node.pointers, node);
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
	 * Adds the given node to the {@link Nodespace#nodeStore} based on the node's pointers.
	 *
	 * @param node the node
	 */
	public void add(Node node) {
		nodeStore.put(node.pointers, node);
	}

	/**
	 * Adds all the given nodes to the {@link Nodespace#nodeStore} based on the node's pointers.
	 *
	 * @param nodes the nodes
	 */
	public void addAll(Collection<Node> nodes) {
		for (Node node : nodes) {
			nodeStore.put(node.pointers, node);
		}
	}

	/**
	 * Removes the given node from the {@link Nodespace#nodeStore} based on the node's pointers. The node given and
	 * the node removed may not necessarily be the same object.
	 *
	 * @param node the node
	 */
	public void remove(Node node) {
		nodeStore.remove(node.pointers);
	}

	/**
	 * Removes all the given nodes from the {@link Nodespace#nodeStore} based on the node's pointers. The node given
	 * and the node removed may not necessarily be the same object.
	 *
	 * @param nodes the nodes
	 */
	public void removeAll(Collection<Node> nodes) {
		for (Node node : nodes) {
			nodeStore.remove(node.pointers);
		}
	}

	/**
	 * Returns all nodes with the given pointer.
	 *
	 * @param pointer the pointer
	 * @return all nodes with the given pointer
	 */
	public List<Node> nodesWithDatum(String pointer) {
		List<Node> nodes = new ArrayList<>();
		for (Node node : nodeStore.values()) {
			if (node.pointers.contains(pointer)) {
				nodes.add(node);
			}
		}
		return nodes;
	}
}
