import org.trikkle.*;

import java.util.HashSet;
import java.util.Set;

public class Profiling {
	public static void main(String[] args) {
		Set<Link> manyLinks = new HashSet<>();
		for (int i = 0; i < 10000; i++) {
			int finalI = i;
			Arc arc = new AutoArc() {
				@Override
				public void run() {
					returnDatum("foo" + finalI, 1);
				}
			};
			manyLinks.add(new Link(Set.of(new Nodespace().discreteOf()), arc, new Nodespace().discreteOf("foo" + i)));
		}
		Overseer overseer = new Overseer(new Graph(manyLinks));
		for (Node startingNode : overseer.getGraph().startingNodes) {
			startingNode.setUsable();
		}
		overseer.setParallel(false);
		overseer.start();
		overseer.printLinkTrace();
	}
}
