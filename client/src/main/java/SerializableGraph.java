import org.trikkle.ArcStatus;
import org.trikkle.Graph;

import java.io.Serializable;
import java.util.Arrays;

public class SerializableGraph implements Serializable {
	public ArcStatus[] arcsStatus;
	public double[] nodesProgress;
	public boolean[] nodesUsable;

	public SerializableGraph() {
	}

	public SerializableGraph(Graph graph) {
		arcsStatus = new ArcStatus[graph.arcArray.length];
		for (int i = 0; i < graph.arcArray.length; i++) {
			arcsStatus[i] = graph.arcArray[i].getStatus();
		}
		nodesProgress = new double[graph.nodeArray.length];
		nodesUsable = new boolean[graph.nodeArray.length];
		for (int i = 0; i < graph.nodeArray.length; i++) {
			nodesProgress[i] = graph.nodeArray[i].getProgress();
			nodesUsable[i] = graph.nodeArray[i].isUsable();
		}
	}

	@Override
	public String toString() {
		return "SerializableGraph{" +
				"arcsStatus=" + Arrays.toString(arcsStatus) +
				", nodesProgress=" + Arrays.toString(nodesProgress) +
				", nodesUsable=" + Arrays.toString(nodesUsable) +
				'}';
	}
}
