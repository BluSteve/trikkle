package core;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.trikkle.structs.IBitmask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Fork(value = 1, warmups = 0)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 3, time = 7)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class BitmaskMark {
	public static void main(String[] args) throws Exception {
		org.openjdk.jmh.Main.main(args);
	}

	@Benchmark
	public static void run(State state, Blackhole blackhole) {
		List<IBitmask> list = new ArrayList<>(state.N);
		for (int i = 0; i < state.N; i++) {
			if (state.query.supersetOf(state.bitmasks[i])) {
				list.add(state.bitmasks[i]);
			}
		}
		blackhole.consume(list);
	}

	@org.openjdk.jmh.annotations.State(Scope.Benchmark)
	public static class State {
		final int C  = 65;
		final int N  = 100000;
		final IBitmask[] bitmasks = new IBitmask[N];
		final IBitmask query = IBitmask.getBitmask(C);
		Random random = new Random(123);

		@Setup(Level.Trial)
		public void setup() {
			for (int i = 0; i < N; i++) {
				bitmasks[i] = IBitmask.getBitmask(C);
				for (int j = 0; j < C; j++) {
					if (random.nextBoolean()) {
						bitmasks[i].set(j);
					}
				}
			}

			for (int j = 0; j < C; j++) {
				if (j % 2 == 0 || random.nextBoolean()) {
					query.set(j);
				}
			}

			System.out.println(query.getClass().getSimpleName());
		}
	}
}
