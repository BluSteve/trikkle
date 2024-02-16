package org.trikkle.annotations;

import org.trikkle.Arc;
import org.trikkle.Link;
import org.trikkle.StreamNode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field in an arc as an input datum. These are filled by {@link Arc#getDatum(String)} before the
 * arc's run method starts. Input streams (see {@link StreamNode}) are generally supported.
 *
 * @see Arc
 * @see Link
 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Input {
	/**
	 * The name of the input datum. If empty, the name of the field will be used.
	 *
	 * @return the name of the input datum
	 */
	String name() default "";
}
