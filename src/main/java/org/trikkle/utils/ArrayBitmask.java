package org.trikkle.utils;

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
		if (index > length) throw new IllegalArgumentException("Index out of range!");
		array[index] = true;
	}

	@Override
	public void unset(int index) {
		if (index > length) throw new IllegalArgumentException("Index out of range!");
		array[index] = false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof ArrayBitmask)) return false;
		return this.hashCode() == obj.hashCode();
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(array);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (boolean b : array) {
			sb.append(b ? 1 : 0);
		}
		return sb.toString();
	}

	@Override
	public int compareTo(IBitmask o) {
		if (this == o) return 0;

		ArrayBitmask ab = (ArrayBitmask) o;
		if (length != ab.length) {
			throw new IllegalArgumentException("ArrayBitmasks not of the same length!");
		}
		else {
			boolean allSame = true;
			for (int i = 0; i < array.length; i++) {
				if (ab.array[i] != array[i]) {
					allSame = false;
				}
				if (ab.array[i] && !array[i]) {
					return -1;
				}
			}
			if (allSame) return 0;
			return 1;
		}
	}
}
