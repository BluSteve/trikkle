package org.trikkle.structs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BitmaskTest {
	@Test
	void test() {
		IBitmask lb1 = new LongBitmask(64);
		lb1.set(2);
		lb1.set(3);

		IBitmask lb2 = new LongBitmask(64);
		lb2.set(2);
		lb2.set(3);
		lb2.unset(3);

		assertTrue(lb1.compareTo(lb2) > 0);
		assertTrue(lb2.compareTo(lb1) < 0);

		IBitmask lb = new LongBitmask(63);
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			lb.compareTo(lb2);
		});
		assertTrue(exception.getMessage().contains("Bitmask lengths do not match!"));
	}

	@Test
	void test2() {
		IBitmask lb1 = new ArrayBitmask(64);
		lb1.set(2);
		lb1.set(3);

		IBitmask lb2 = new ArrayBitmask(64);
		lb2.set(2);
		lb2.set(3);
		lb2.unset(3);

		assertTrue(lb1.compareTo(lb2) > 0);
		assertTrue(lb2.compareTo(lb1) < 0);
		IBitmask lb = new ArrayBitmask(63);
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			lb.compareTo(lb2);
		});
		assertTrue(exception.getMessage().contains("Bitmask lengths do not match!"));
	}

	@Test
	void test3() {
		IBitmask lb1 = new ArrayBitmask(64);
		lb1.set(2);
		lb1.set(3);

		IBitmask lb2 = new LongBitmask(64);
		lb2.set(2);
		lb2.set(3);

		assertEquals(lb1, lb2);
		assertEquals(lb2, lb1);
	}

	@Test
	void test4() {
		IBitmask lb1 = new LongBitmask(64);
		lb1.set(2);
		lb1.set(3);

		IBitmask lb2 = new LongBitmask(64);
		lb2.set(2);
		lb2.set(3);

		assertEquals(lb1, lb2);

		lb1 = new ArrayBitmask(64);
		lb1.set(2);
		lb1.set(3);

		lb2 = new ArrayBitmask(64);
		lb2.set(2);
		lb2.set(3);

		assertEquals(lb1, lb2);
	}
}