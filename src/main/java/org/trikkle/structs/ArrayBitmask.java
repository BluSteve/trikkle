package org.trikkle.structs;

import java.util.Arrays;
import java.util.Objects;

class ArrayBitmask implements IBitmask {
	public final int length;
	private final boolean[] array;

	ArrayBitmask(int length) {
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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (!(o instanceof ArrayBitmask)) {
			if (o instanceof IBitmask) return this.toString().equals(o.toString());
			else return false;
		}
		ArrayBitmask that = (ArrayBitmask) o;
		return length == that.length && Arrays.equals(array, that.array);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(length);
		result = 31 * result + Arrays.hashCode(array);
		return result;
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
			throw new IllegalArgumentException("Bitmask lengths do not match!");
		} else {
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
