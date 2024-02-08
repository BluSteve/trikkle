package org.trikkle.structs;

public interface IBitmask {
	static IBitmask getBitmask(int length) {
		if (length < 0) throw new IllegalArgumentException("Length cannot be < 0!");
		if (length > 64) return new ArrayBitmask(length);
		else return new LongBitmask(length);
	}

	void set(int index);

	void unset(int index);

	boolean supersetOf(IBitmask o);

	@Override
	String toString();

	@Override
	boolean equals(Object o);

	@Override
	int hashCode();
}
