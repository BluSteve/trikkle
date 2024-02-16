package org.trikkle;

import java.util.Set;

/**
 * A class that can be compared to another class for congruence. This is useful for comparing two objects that are
 * logically equivalent but not necessarily the same object.
 *
 * @param <T> the type of the class to compare to
 * @since 0.1.0
 */
public interface Congruent<T> {
	/**
	 * Compare two sets for congruence. Two sets are congruent if they are the same size and for every element in one
	 * set there is a congruent element in the other set.
	 *
	 * @param set1 the first set
	 * @param set2 the second set
	 * @param <T>  the type of the set
	 * @return true if the sets are congruent
	 */
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

	/**
	 * Compare two objects for congruence.
	 *
	 * @param t the object to compare to
	 * @return true if the objects are congruent
	 */
	boolean congruentTo(T t);
}
