package org.trikkle;

import java.util.Arrays;

public class ArrayBitmask implements IBitmask {
	public final int length;
	private final boolean[] array;

	public ArrayBitmask(int length) {
		this.length = length;
		array = new boolean[length];
	}

	@Override
	public void set(int index) {
		array[index] = true;
	}

	@Override
	public void unset(int index) {
		array[index] = false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(array);
	}
}
