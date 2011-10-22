package net.egork.chelper.task;


/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Test {
	public final int index;
	public final String input;
	public final String output;

	public static final String SEPARATOR = ";;";

	public Test(String input, String output, int index) {
		this.input = input;
		this.output = output;
		this.index = index;
	}

	@Override
	public String toString() {
		String inputRepresentation = input.replace('\n', ' ');
		inputRepresentation = inputRepresentation.length() > 15 ? inputRepresentation.substring(0, 12) + "..." :
			inputRepresentation;
		return "Test #" + index + ": " + inputRepresentation;
	}

	public static Test decode(int index, String test) {
		String[] tokens = test.split(SEPARATOR, -1);
		return new Test(Encoding.decode(tokens[0]), Encoding.decode(tokens[1]), index);
	}

	public Test setIndex(int index) {
		return new Test(input, output, index);
	}

	public String encode() {
		return Encoding.encode(input) + SEPARATOR + Encoding.encode(output);
	}
}
