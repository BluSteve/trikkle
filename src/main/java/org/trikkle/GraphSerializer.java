package org.trikkle;

import org.nustaq.serialization.FSTConfiguration;

/**
 * Serializes graphs.
 *
 * @see Graph
 * @since 0.1.1
 */
public class GraphSerializer {
	static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

	public static byte[] serialize(Graph graph) {
		return conf.asByteArray(graph);
	}

	public static Graph deserialize(byte[] bytes) {
		return (Graph) conf.asObject(bytes);
	}
}
