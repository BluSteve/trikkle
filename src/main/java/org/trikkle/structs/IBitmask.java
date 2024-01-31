package org.trikkle.structs;

public interface IBitmask extends Comparable<IBitmask> {
	static IBitmask getBitmask(int length) {
		if (length < 0) throw new IllegalArgumentException("Length cannot be < 0!");
		if (length > 64) return new ArrayBitmask(length);
		else return new LongBitmask(length);
	}

	void set(int index);

	void unset(int index);

	@Override
	boolean equals(Object o);

	@Override
	int hashCode();
}
