package org.trikkle;

import java.util.Random;

public class JavassistTest {
		static Random r = new Random(1);
		static String randomString = Long.toHexString(r.nextLong()); // effectively final variables are abstract fsr
	public static void main(String[] args) {
		String datum2$in = "b" + randomString;

		String output$out = "a+b";
		Arc arc = new AutoArc() {
			final String datum$in = "a" + randomString;
			// unless ignored
			@Override
			protected void run() {
				double a = getDatum(datum$in);
				double b = getDatum(datum2$in);
				returnDatum(output$out, a + b);
			}
		};

		System.out.println(arc.getInputDatumNames());
		System.out.println(arc.getOutputDatumNames());
	}
}
