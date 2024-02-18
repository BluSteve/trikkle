import org.trikkle.Graph;
import org.trikkle.MachineMain;
import org.trikkle.Node;
import org.trikkle.Overseer;

import java.util.Map;

/*
distributed methods:
0. send graph to all machines cluster (doesn't have to be inside distributedoverseer)

1. get datum
2. update graph with local changes
3. request consensus to execute arc

datatypes needed:
g: broadcast graph state
d: request for a datum
c: request permission to run arc
C: reply. content contains yes or no. another possibility is no response. all must respond with yes to run arc.
 */
public class DistributedOverseer extends Overseer {
	MachineMain machine;

	public DistributedOverseer(MachineMain machine, Graph graph) {
		this(machine, graph, null);
	}

	public DistributedOverseer(MachineMain machine, Graph graph, Map<String, Object> initialCache) {
		super(graph, initialCache);
		this.machine = machine;

		// add handlers for the datatypes



	}

	@Override
	protected void ticktock(Node caller) {
		// this is when the local graph state changes.
		// it needs to inform others of the change before doing/ deciding whether to do arcs

		// afterwards it must request for permission to do an arc
		// so this whole method has to be overriden
	}

	// this is when some other DO sends a graph update. probably need to ticktock yourself
	public void foreignUpdate(SerializableGraph sGraph) {

	}

	// return datum doesn't need to be overriden
	// get datum needs to be overriden definitely. all requests to get datum should go through that method
	protected Object getDatumProtected(String datumName) {
		return null;
	}
}
