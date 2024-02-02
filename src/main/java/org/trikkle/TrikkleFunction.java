package org.trikkle;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(TrikkleFunctionGroup.class)
public @interface TrikkleFunction {
	String[] inputs();

	// if two functions have the same nodeId, they will be used to make one outputNode with all their outputDatumNames
	// 1:1 correspondence with Link
	String linkId();

	String output();
}
