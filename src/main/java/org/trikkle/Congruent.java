package org.trikkle;

import java.util.Set;

public interface Congruent<T> {
	boolean congruentTo(T t);

	static <T extends Congruent<T>> boolean setsCongruent(Set<T> set1, Set<T> set2) {
		if (set1.size() != set2.size()) {
			return false;
		}
		
		for (T congruent1 : set1) {
			boolean found = false;
			for (T congruent2 : set2) {
				if (congruent1.congruentTo(congruent2)) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}
}
