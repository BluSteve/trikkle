package org.trikkle.structs;

import java.util.Arrays;
import java.util.Objects;

public class ArrayBitmask implements IBitmask {
	private static final long MAX_LENGTH = Integer.MAX_VALUE - 8;
	private final int length;
	private final boolean[] array;

	public ArrayBitmask(int length) {
		if (length > MAX_LENGTH) {
			throw new IllegalArgumentException("Bitmask length out of range!");
		}
		this.length = length;
		array = new boolean[length];
	}

	@Override
	public long length() {
		return length;
	}

	@Override
	public long maxLength() {
		return MAX_LENGTH;
	}

	@Override
	public void set(long index) {
		if (index > length) throw new IllegalArgumentException("Index out of range!");
		array[(int) index] = true;
	}

	@Override
	public void unset(long index) {
		if (index > length) throw new IllegalArgumentException("Index out of range!");
		array[(int) index] = false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (!(o instanceof ArrayBitmask)) {
			if (o instanceof IBitmask) {
				return this.toString().equals(o.toString());
			} else {
				return false;
			}
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
	public boolean supersetOf(IBitmask o) {
		if (this == o) return true;

		ArrayBitmask ab = (ArrayBitmask) o;
		if (length != ab.length) {
			throw new IllegalArgumentException("Bitmask lengths do not match!");
		} else {
			for (int i = 0; i < array.length; i++) {
				if (ab.array[i] && !array[i]) {
					return false;
				}
			}
			return true;
		}
	}
}
