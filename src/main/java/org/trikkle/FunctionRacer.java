package org.trikkle;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.function.Function;

/**
 * A class that races functions to return the result of the first function that finishes. The other functions are
 * interrupted.
 * This class is useful for when you have multiple functions that can return the same result, and you want to use the
 * result
 * that comes back first.
 *
 * @author Steve Cao
 * @since 0.1.0
 */
public final class FunctionRacer {
	public final Set<Function<Map<String, Object>, Map<String, Object>>> functions;
	private final Notifier notifier = new Notifier();
	private Map<String, Object> result = null;

	public FunctionRacer(Set<Function<Map<String, Object>, Map<String, Object>>> functions) {
		this.functions = functions;
	}

	public Map<String, Object> apply(Map<String, Object> cache) {
		List<RecursiveAction> tasks = new ArrayList<>();

		for (Function<Map<String, Object>, Map<String, Object>> function : functions) {
			// Wrapping in Thread is necessary for it to be interruptible
			Thread thread = new Thread(() -> returnResult(function.apply(cache)));
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

		if (!notifier.wasCalled()) {
			throw new IllegalStateException("No functions were called!");
		}
		return result;
	}

	private synchronized void returnResult(Map<String, Object> map) {
		if (!notifier.wasCalled()) {
			result = map;
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
