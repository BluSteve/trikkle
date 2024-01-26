package org.trikkle;

public interface IBitmask extends Comparable<IBitmask> {
	void set(int index);

	void unset(int index);

	@Override
	int hashCode();
}
