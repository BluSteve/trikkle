package org.trikkle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TrikkleFunction {
	String[] inputDatumNames() default {}; // WARNING: this default requires some compiler options to be enabled

	String arcName() default "";

	// if two functions have the same nodeId, they will be used to make one outputNode with all their outputDatumNames
	String outputNodeId();

	String outputDatumName();
}
