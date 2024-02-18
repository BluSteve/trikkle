import org.trikkle.Graph;
import org.trikkle.Node;
import org.trikkle.Overseer;

import java.util.Map;

/*
distributed methods:
0. send graph to all machines cluster (doesn't have to be inside distributedoverseer)

1. get datum
2. update graph with local changes
3. request consensus to execute arc
 */
public class DistributedOverseer extends Overseer {
	public DistributedOverseer(Graph graph) {
		super(graph);
	}

	public DistributedOverseer(Graph graph, Map<String, Object> initialCache) {
		super(graph, initialCache);
	}

	@Override
	protected void ticktock(Node caller) {
		// this is when the local graph state changes.
		// it needs to inform others of the change before doing/ deciding whether to do arcs

		// afterwards it must request for permission to do an arc
		// so this whole method has to be overriden
	}

	// this is when some other DO sends a graph update. probably need to ticktock yourself
	public void foreignUpdate() {

	}

	// return datum doesn't need to be overriden
	// get datum needs to be overriden definitely. all requests to get datum should go through that method
}
