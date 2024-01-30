package org.trikkle.utils;

import java.util.Objects;

class LongBitmask implements IBitmask {
	public final int length;
	private long bitmask;

	LongBitmask(int length) {
		if (length > 64) {
			throw new IllegalArgumentException("Bitmask length out of range!");
		}

		this.length = length;
		this.bitmask = 0;
	}

	public static void main(String[] args) {
		IBitmask lb1 = new ArrayBitmask(64);
		lb1.set(2);
		lb1.set(3);
		System.out.println("lb1 = " + lb1);

		IBitmask lb2 = new ArrayBitmask(64);
		System.out.println("lb2 = " + lb2);

		System.out.println(lb1.compareTo(lb2));
	}

	@Override
	public void set(int index) {
		if (index >= length) throw new IllegalArgumentException("Index out of range!");
		bitmask = bitmask | (1L << index);
	}

	@Override
	public void unset(int index) {
		if (index >= length) throw new IllegalArgumentException("Index out of range!");
		bitmask = bitmask & ~(1L << index);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
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
		return s.toString();
	}

	@Override
	public int compareTo(IBitmask o) {
		if (this == o) return 0;

		LongBitmask lb = (LongBitmask) o;
		if (length != lb.length) {
			throw new IllegalArgumentException("LongBitmasks not of the same length!");
		} else {
			if (bitmask == lb.bitmask) {
				return 0;
			} else if (bitmask == (bitmask | lb.bitmask)) {
				return 1;
			} else return -1;
		}
	}
}
