package org.trikkle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TrikkleLinkGroup {
	// use this class to either take in datums from multiple nodes
	// or use the same arc for multiple links
	TrikkleLink[] value();
}
