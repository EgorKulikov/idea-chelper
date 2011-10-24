package net.egork.chelper.task;

import java.util.Arrays;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderTest {
	public final String[] arguments;
	public final String result;
	public final int index;

	public TopCoderTest(String[] arguments, String result, int index) {
		this.arguments = arguments;
		this.result = result;
		this.index = index;
	}

	public String toString() {
		String representation = Arrays.toString(arguments);
		if (representation.length() > 15)
			representation = representation.substring(0, 12) + "...";
		return "Test #" + index + ": " + representation;
	}

	public TopCoderTest setIndex(int index) {
		return new TopCoderTest(arguments, result, index);
	}
}
