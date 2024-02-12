package core;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.trikkle.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Fork(value = 1, warmups = 0)
@Warmup(iterations = 2, time = 5)
@Measurement(iterations = 3, time = 5)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class Benchmarks {
	public static void main(String[] args) throws Exception {
		org.openjdk.jmh.Main.main(args);
	}

	@Benchmark
	public static void run(MyState state, Blackhole blackhole) {
		state.overseer.start();
	}

	@Benchmark
	public static void runList(MyState state, Blackhole blackhole) {
		for (int i = 0; i < 10000; i++) {
			System.out.println("hi");
		}
	}

	@State(Scope.Benchmark)
	public static class MyState {
		Overseer overseer;

		@Setup(Level.Invocation)
		public void setup() {
			Set<Link> manyLinks = new HashSet<>();
			for (int i = 0; i < 10000; i++) {
				Arc arc = new AutoArc() {
					@Override
					public void run() {
						System.out.println("hi");
						getOutputNode().setProgress(1);
					}
				};
				manyLinks.add(new Link(Set.of(new Nodespace().emptyOf()), arc, new Nodespace().emptyOf()));
			}
			overseer = new Overseer(new Graph(manyLinks));
			for (Node startingNode : overseer.getGraph().startingNodes) {
				startingNode.setUsable();
			}
			overseer.setParallel(false);
		}
	}
}
