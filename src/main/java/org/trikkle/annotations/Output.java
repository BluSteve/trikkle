package org.trikkle.annotations;

import org.trikkle.Arc;
import org.trikkle.Link;
import org.trikkle.StreamNode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field in an arc as a return datum. These are only returned via
 * {@link Arc#returnDatum(String, Object)} after the arc's {@link Arc#run()} method finishes.
 * Output streams (see
 * {@link StreamNode}) are supported but strongly not recommended due to lack of clarity.
 * <p>
 * If you do not assign a value to the field in the arc's run method, the default value of the
 * field will be returned. If the field is a primitive, the default value might be 0 or false. If the field is
 * an object, the default value will be null.
 *
 * @author Steve Cao
 * @see Arc
 * @see Arc#run()
 * @see Link
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
