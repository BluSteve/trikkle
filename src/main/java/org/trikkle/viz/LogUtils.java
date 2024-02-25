package org.trikkle.viz;

import org.trikkle.Graph;
import org.trikkle.Link;
import org.trikkle.Node;

import java.util.*;

/**
 * A utility class for logging and visualization.
 *
 * @since 0.1.0
 */
public class LogUtils {
	public static String linkTraceToString(Queue<Collection<Link>> linkTrace) {
		StringBuilder sb = new StringBuilder();
		int tick = 1;
		for (Collection<Link> links : linkTrace) {
			sb.append("Tick ").append(tick++).append(":\n");
			for (Link link : links) {
				sb.append(link).append("\n");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Animate a graph using a link trace with Mermaid. For each tick, the dependencies and output nodes of the links
	 * that are going to be run will be colored.
	 *
	 * @param graph     the graph to animate
	 * @param linkTrace the link trace to use
	 * @return a list of strings, each string being a mermaid graph visualization
	 */
	public static List<String> animate(Graph graph, Queue<Collection<Link>> linkTrace) {
		List<String> animations = new ArrayList<>();
		MermaidGraphViz mermaidGraphViz = new MermaidGraphViz();

		Set<Node> done = new HashSet<>();
		for (Collection<Link> links : linkTrace) {
			if (links.isEmpty()) continue;
			for (Link link : links) {
				done.addAll(link.getDependencies());
				done.addAll(link.getOutputNodes());
			}
			animations.add(mermaidGraphViz.visualize(done, graph));
		}

		return animations;
	}

	/**
	 * Convert a list of Mermaid animations to a single markdown document.
	 *
	 * @param animations the list of animations
	 * @return the markdown string
	 */
	public static String toMarkdown(List<String> animations) {
		StringBuilder sb = new StringBuilder();
		for (String animation : animations) {
			sb.append("```mermaid\n");
			sb.append(animation);
			sb.append("\n```\n");
		}
		return sb.toString();
	}
}
