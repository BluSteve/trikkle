package org.trikkle.structs;

import java.util.Arrays;
import java.util.Objects;

public class LongArrayBitmask implements IBitmask {
	private static final long MAX_LENGTH = (long) (Integer.MAX_VALUE - 8) * Long.SIZE;
	private final long length;
	private final long[] bitmask;

	public LongArrayBitmask(long length) {
		if (length > MAX_LENGTH) {
			throw new IllegalArgumentException("Bitmask length out of range!");
		}

		this.length = length;
		this.bitmask = new long[(int) ((length - 1) / Long.SIZE + 1)];
	}

	public static void main(String[] args) {
		// Test LongArrayBitmask
		IBitmask lb = new LongArrayBitmask(65);
		lb.set(0);
		lb.set(1);
		lb.set(63);
		lb.set(64);
		System.out.println(lb);
		System.out.println("MAX_LENGTH = " + MAX_LENGTH);
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
		if (index >= length) throw new IllegalArgumentException("Index out of range!");
		bitmask[(int) (index / Long.SIZE)] |= (1L << (index % Long.SIZE));
	}

	@Override
	public void unset(long index) {
		if (index >= length) throw new IllegalArgumentException("Index out of range!");
		bitmask[(int) (index / Long.SIZE)] &= ~(1L << (index % Long.SIZE));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (!(o instanceof LongArrayBitmask)) {
			if (o instanceof IBitmask) {
				return this.toString().equals(o.toString());
			} else {
				return false;
			}
		}
		LongArrayBitmask that = (LongArrayBitmask) o;
		return length == that.length && Arrays.equals(bitmask, that.bitmask);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(length);
		result = 31 * result + Arrays.hashCode(bitmask);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (long l : bitmask) {
			String replace = String.format("%64s", Long.toBinaryString(l)).replace(' ', '0');
			replace = new StringBuilder(replace).reverse().toString();
			s.append(replace);
		}

		return s.substring(0, (int) length);
	}

	@Override
	public boolean supersetOf(IBitmask o) {
		if (this == o) return true;

		LongArrayBitmask lb = (LongArrayBitmask) o;
		if (length != lb.length) {
			throw new IllegalArgumentException("Bitmask lengths do not match!");
		} else {
			for (int i = 0; i < bitmask.length; i++) {
				if ((bitmask[i] & lb.bitmask[i]) != lb.bitmask[i]) {
					return false;
				}
			}
		}

		return true;
	}
}
