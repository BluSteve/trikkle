package org.trikkle.structs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StrictConcurrentHashMapTest {

	@Test
	void put() {
		StrictConcurrentHashMap<String, String> map = new StrictConcurrentHashMap<>();
		map.put("key", "value");
		Exception exception = assertThrows(IllegalArgumentException.class, () -> map.put("key", "value"));
		assertEquals("Key \"key\" already present in map!", exception.getMessage());
	}

	@Test
	void containsKey() {
		// check whether containsKey works
		StrictConcurrentHashMap<String, String> map = new StrictConcurrentHashMap<>();
		map.put("key", "value");
		assertTrue(map.containsKey("key"));
	}

	@Test
	void get() {
		// check whether get works
		StrictConcurrentHashMap<String, String> map = new StrictConcurrentHashMap<>();
		map.put("key", "value");
		assertEquals("value", map.get("key"));
	}
}
