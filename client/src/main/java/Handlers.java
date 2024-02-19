import org.trikkle.*;

import java.net.Socket;
import java.util.Set;

public class Handlers {
	@Handler(dataType = 't')
	public static void handleData(byte[] data, Socket socket) {
		System.out.println("data = " + new String(data));
	}

	// over here there will be a handler 's' for instance (start)
	// one of the instances will send over this jar and then call s
	// inside s will be the full graph and distributed overseer and everything
	// you can't serialize lambdas so you have to send over the graph within the jar.
	// future graph "state" updates can be done with a serialized graph
	@Handler(dataType = 's')
	public static void handleStart(byte[] jarBytes, Socket socket) {
		Node inputNode = new DiscreteNode("x");
		Node outputNode = new DiscreteNode("y");
		Arc arc = new AutoArc() {
			public void run() {
				double x = (double) getDatum("x");
				double y = x * x;
				returnDatum("y", y);
			}
		};
		Link link = new Link(Set.of(inputNode), arc, Set.of(outputNode));
		Graph graph = new Graph(link);

		DistributedOverseer overseer = new DistributedOverseer(MachineMain.instance, graph);
		overseer.addStartingDatum("x", 5.0);
		overseer.start();
		System.out.println(new SerializableGraph(graph));
		System.out.println("y = " + overseer.getDatum("y"));
	}

	public static void main(String[] args) {
		handleStart(null, null);
	}
}
