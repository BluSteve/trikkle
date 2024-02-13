package org.trikkle.viz;

import org.trikkle.Link;

import java.util.Collection;
import java.util.Queue;

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
}
