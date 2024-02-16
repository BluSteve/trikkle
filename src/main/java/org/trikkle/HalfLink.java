package org.trikkle;

import java.util.*;

/**
 * @author Steve Cao
 * @since 0.1.0
 */
public class HalfLink {
	private final Arc arc;
	private final Set<String> inputNames;
	private final Set<Node> outputNodes;

	public HalfLink(Arc arc, Set<Node> outputNodes) {
		this.arc = arc;
		this.outputNodes = outputNodes;
		inputNames = arc.inputFields.keySet();
	}

	public HalfLink(Arc arc, Node outputNode) {
		this(arc, Collections.singleton(outputNode));
	}

	// build graph starting with the root node and then dynamically adding dependent nodes
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
			Set<Node> dependencies = new HashSet<>();
			Set<String> danglingInputs = new HashSet<>();
			for (String inputName : halfLink.inputNames) {
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
}
