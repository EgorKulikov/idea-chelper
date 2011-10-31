package net.egork.chelper.task;


/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Test {
	public final int index;
	public final String input;
	public final String output;
	public final boolean active;

	public Test(String input, String output, int index) {
		this(input, output, index, true);
	}

	public Test(String input, String output, int index, boolean active) {
		this.input = input;
		this.output = output;
		this.index = index;
		this.active = active;
	}

	@Override
	public String toString() {
		String inputRepresentation = input.replace('\n', ' ');
		inputRepresentation = inputRepresentation.length() > 15 ? inputRepresentation.substring(0, 12) + "..." :
			inputRepresentation;
		return "Test #" + index + ": " + inputRepresentation;
	}

	public Test setIndex(int index) {
		return new Test(input, output, index, active);
	}

	public Test setActive(boolean active) {
		return new Test(input, output, index, active);
	}
}
