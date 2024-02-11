package org.trikkle.structs;

public interface IBitmask {
	static IBitmask getBitmask(int length) {
		if (length < 0) throw new IllegalArgumentException("Length cannot be < 0!");
		if (length > 64) return new ArrayBitmask(length);
		else return new LongBitmask(length);
	}

	long length();

	long maxLength();

	void set(long index);

	void unset(long index);

	boolean supersetOf(IBitmask o);

	@Override
	String toString();

	@Override
	boolean equals(Object o);

	@Override
	int hashCode();
}
