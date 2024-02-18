import org.trikkle.Handler;

public class Handlers {
	@Handler(dataType = 't')
	public static void handleData(byte[] data) {
		System.out.println("data = " + new String(data));
	}

	// over here there will be a handler 's' for instance (start)
	// one of the instances will send over this jar and then call s
	// inside s will be the full graph and distributed overseer and everything
	// you can't serialize lambdas so you have to send over the graph within the jar.
	// future graph "state" updates can be done with a serialized graph
}
