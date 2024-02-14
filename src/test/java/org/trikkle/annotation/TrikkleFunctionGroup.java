package org.trikkle.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TrikkleFunctionGroup {
	// use this class to either take in datums from multiple nodes
	// or use the same arc for multiple links
	TrikkleFunction[] value();
}
