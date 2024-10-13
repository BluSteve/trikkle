package org.trikkle;

import java.util.Random;

public class JavassistTest {
		static Random r = new Random(1);
		static String randomString = Long.toHexString(r.nextLong()); // effectively final variables are abstract fsr
	public static void main(String[] args) {
		String datum2$name = "b" + randomString;

		String outputName = "a+b";
		Arc arc = new AutoArc() {
			final String datum$name = "a" + randomString;
			// unless ignored
			@Override
			protected void run() {
				double a = getDatum(datum$name);
				double b = getDatum(datum2$name);
				returnDatum(outputName, a + b);
			}
		};

		System.out.println(arc.getInputDatumNames2());
	}
}
