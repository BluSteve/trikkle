package org.trikkle.viz;

import org.trikkle.Graph;
import org.trikkle.Link;
import org.trikkle.Node;

import java.util.*;

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

	public static List<String> animate(Graph graph, Queue<Collection<Link>> linkTrace) {
		List<String> animations = new ArrayList<>();
		MermaidGraphViz mermaidGraphViz = new MermaidGraphViz();

		Set<Node> done = new HashSet<>();
		for (Collection<Link> links : linkTrace) {
			if (links.isEmpty()) continue;
			for (Link link : links) {
				done.addAll(link.getDependencies());
			}
			animations.add(mermaidGraphViz.visualize(done, graph));
		}

		animations.add(mermaidGraphViz.visualize(graph.nodes, graph));

		return animations;
	}

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
