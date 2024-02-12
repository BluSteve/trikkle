package core;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.trikkle.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Fork(value = 1, warmups = 0)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 3, time = 7)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class Benchmark {
	public static void main(String[] args) throws Exception {
		org.openjdk.jmh.Main.main(args);
	}

	@org.openjdk.jmh.annotations.Benchmark
	public static void run(State state, Blackhole blackhole) {
		Set<Link> manyLinks = new HashSet<>();
		for (int i = 0; i < 10000; i++) {
			Arc arc = new AutoArc() {
				@Override
				public void run() {

				}
			};
			manyLinks.add(new Link(Set.of(new Nodespace().discreteOf()), arc, Set.of()));
		}
		Overseer overseer = new Overseer(new Graph(manyLinks));
		for (Node startingNode : overseer.getGraph().startingNodes) {
			startingNode.setUsable();
		}
		overseer.start();
	}

	@org.openjdk.jmh.annotations.State(Scope.Benchmark)
	public static class State {
		@Setup(Level.Trial)
		public void setup() {
		}
	}
}
