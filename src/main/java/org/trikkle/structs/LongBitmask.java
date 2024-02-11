package org.trikkle.structs;

import java.util.Objects;

public class LongBitmask implements IBitmask {
	private static final long MAX_LENGTH = Long.SIZE;
	private final int length;
	private long bitmask;

	public LongBitmask(int length) {
		if (length > MAX_LENGTH) {
			throw new IllegalArgumentException("Bitmask length out of range!");
		}
		this.length = length;
		this.bitmask = 0;
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
		bitmask = bitmask | (1L << index);
	}

	@Override
	public void unset(long index) {
		if (index >= length) throw new IllegalArgumentException("Index out of range!");
		bitmask = bitmask & ~(1L << index);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (!(o instanceof LongBitmask)) {
			if (o instanceof IBitmask) {
				return this.toString().equals(o.toString());
			} else {
				return false;
			}
		}
		LongBitmask that = (LongBitmask) o;
		return length == that.length && bitmask == that.bitmask;
	}

	@Override
	public int hashCode() {
		return Objects.hash(length, bitmask);
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder(Long.toBinaryString(bitmask));
		int ld = length - s.length();
		if (ld > 0) {
			for (int i = 0; i < ld; i++) {
				s.insert(0, "0");
			}
		}
		s.reverse();
		return s.toString();
	}

	@Override
	public boolean supersetOf(IBitmask o) {
		if (this == o) return true;

		LongBitmask lb = (LongBitmask) o;
		if (length != lb.length) {
			throw new IllegalArgumentException("Bitmask lengths do not match!");
		} else {
			return bitmask == lb.bitmask || bitmask == (bitmask | lb.bitmask);
		}
	}
}
