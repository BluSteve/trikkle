package org.trikkle.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field in an arc as an input datum.
 *
 * @author Steve Cao
 * @see org.trikkle.Arc
 * @see org.trikkle.Link
 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Output {
}
