package org.trikkle;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.function.Function;

public final class RaceArc extends AutoArc {
	public final Set<Function<Map<String, Object>, Map<String, Object>>> subArcs;
	private final Notifier notifier = new Notifier();

	public RaceArc(Set<Function<Map<String, Object>, Map<String, Object>>> subArcs) {
		this.subArcs = subArcs;
	}

	@Override
	public void run() {
		List<RecursiveAction> tasks = new ArrayList<>();

		for (Function<Map<String, Object>, Map<String, Object>> arc : subArcs) {
			// Wrapping in Thread is necessary for it to be interruptible
			Thread thread = new Thread(() -> returnResult(arc.apply(overseer.getCache())));
			notifier.addListener(thread::interrupt);

			RecursiveAction task = new RecursiveAction() {
				@Override
				protected void compute() {
					thread.start();
					try {
						thread.join();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			};
			tasks.add(task);
		}
		ForkJoinTask.invokeAll(tasks);
	}

	private synchronized void returnResult(Map<String, Object> map) {
		if (!notifier.wasCalled()) {
			overseer.getCache().putAll(map);
			notifier.call();
		}
	}

	private static class Notifier {
		private final List<Runnable> listeners = new LinkedList<>();
		private boolean called = false;

		public void addListener(Runnable runnable) {
			listeners.add(runnable);
		}

		public void call() {
			called = true;
			for (Runnable listener : listeners) {
				listener.run();
			}
		}

		public boolean wasCalled() {
			return called;
		}
	}
}
