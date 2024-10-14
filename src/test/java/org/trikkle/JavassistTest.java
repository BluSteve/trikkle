package org.trikkle;

import java.util.Random;

public class JavassistTest {
		static Random r = new Random(1);
		static String randomString = Long.toHexString(r.nextLong()); // effectively final variables are abstract fsr
	public static void main(String[] args) {
		String b$in = "b" + randomString;

		String output$out = "a+b";
		Arc arc = new AutoArc() {
			final String a$in = "a" + randomString;
			String[] placeholder = {a$in, b$in, output$out};
			double a, b, output;

			// unless ignored
			@Override
			protected void run() {
				output = a + b;
			}
		};

		System.out.println(arc.getInputDatumNames());
		System.out.println(arc.getOutputDatumNames());
	}
}
