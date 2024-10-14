package org.trikkle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JavassistTest {
	static Random r = new Random(1);
	static String randomString = Long.toHexString(r.nextLong()); // effectively final variables are abstract fsr

	public static void main(String[] args) {
		String b$in = "b" + randomString;
		String a$in = "a" + randomString;

		String output$out = "a+b";
		Arc arc = new AutoArc() {
			String a$in = "a" + randomString;
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

		Link link = new Link(null, arc, null);
		List<Link> links = new ArrayList<>();
		links.add(link);
		Graph.preprocess(links, new Nodespace());
		System.out.println(link);
		Graph graph = new Graph(links);

		Overseer overseer = new Overseer(graph);
		overseer.addStartingDatum(a$in, 1.0);
		overseer.addStartingDatum(b$in, 2.0);
		overseer.start();
		System.out.println(overseer.getDatum(output$out));
	}
}
