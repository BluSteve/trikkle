package org.trikkle;

import java.util.Random;

public class JavassistTest {
	public static void main(String[] args) {

		Random r = new Random(1);
		String randomString = Long.toHexString(r.nextLong()); // effectively final variables are abstract fsr

		Arc arc = new AutoArc() {
			// unless ignored
			public String datumName = "a" + randomString;
			public String datumName1 = "b" + randomString;
			String outputName = "a+b";

			@Override
			protected void run() {
				double a = getDatum(datumName);
				double b = getDatum(datumName1);
				returnDatum(outputName, a + b);
			}
		};

		System.out.println(arc.getInputDatumNames2());
	}
}
