package net.egork.chelper.task;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Encoding {
	public static String encode(String s) {
		return s.replace(":", "/:").replace(";", "/;").replace("_", "/_").replace("\n", "/__");
	}

	public static String decode(String s) {
		return s.replace("/__", "\n").replace("/_", "_").replace("/:", ":").replace("/;", ";");
	}
}
