package org.trikkle.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field in an arc as a return datum. These are only returned via
 * {@link org.trikkle.Arc#returnDatum(String, Object)} after the arc's run method finishes.
 *
 * @author Steve Cao
 * @see org.trikkle.Arc
 * @see org.trikkle.Link
 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Output {
	/**
	 * The name of the return datum. If empty, the name of the field will be used.
	 *
	 * @return the name of the return datum
	 */
	String name() default "";
}
