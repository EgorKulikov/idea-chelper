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

	public String encode() {
		StringBuilder builder = new StringBuilder();
		for (String argument : arguments)
			builder.append(Encoding.encode(argument)).append(Test.SEPARATOR);
		builder.append(Encoding.encode(result));
		return builder.toString();
	}

	public static TopCoderTest decode(int index, String s) {
		String[] tokens = s.split(Test.SEPARATOR, -1);
		String[] arguments = new String[tokens.length - 1];
		for (int i = 0; i < arguments.length; i++)
			arguments[i] = Encoding.decode(tokens[i]);
		return new TopCoderTest(arguments, Encoding.decode(tokens[arguments.length]), index);
	}
}
