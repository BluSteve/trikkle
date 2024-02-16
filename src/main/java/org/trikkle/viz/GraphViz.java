package org.trikkle.viz;

import org.trikkle.Graph;

/**
 * A GraphViz is a class that can visualize a graph.
 *
 * @since 0.1.0
 */
public interface GraphViz {
	String visualize(Graph... graph);
}
