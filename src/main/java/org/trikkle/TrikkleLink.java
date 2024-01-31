package org.trikkle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TrikkleLink {
	String[] inputDatumNames() default {}; // WARNING: this default requires some compiler options to be enabled

	String arcName() default "";

	String outputDatumName();
}
