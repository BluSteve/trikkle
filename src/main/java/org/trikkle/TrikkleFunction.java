package org.trikkle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TrikkleFunction {
	String[] inputDatumNames();

	String arcName() default "";

	// if two functions have the same nodeId, they will be used to make one outputNode with all their outputDatumNames
	// 1:1 correspondence with Link
	String linkId();

	String outputDatumName();
}
