package org.trikkle.utils;

public interface IBitmask extends Comparable<IBitmask> {
	void set(int index);

	void unset(int index);

	@Override
	int hashCode();

	@Override
	boolean equals(Object o);
}
