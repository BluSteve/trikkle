import org.trikkle.*;
import org.trikkle.viz.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Profiling {
	public static void main(String[] args) {
		List<Link> manyLinks = new ArrayList<>();
		for (int i = 0; i < 10000; i++) {
			int finalI = i;
			Arc arc = new AutoArc() {
				@Override
				public void run() {
					getOutputNode().setUsable();
				}
			};
			manyLinks.add(new Link(Set.of(new Nodespace().emptyOf()), arc, new Nodespace().emptyOf()));
		}
		Overseer overseer = new Overseer(new Graph(manyLinks));
		for (Node startingNode : overseer.getGraph().startingNodes) {
			startingNode.setUsable();
		}
		overseer.setParallel(false);
		overseer.start();
		System.out.println(LogUtils.linkTraceToString(overseer.getLinkTrace()));
	}
}
