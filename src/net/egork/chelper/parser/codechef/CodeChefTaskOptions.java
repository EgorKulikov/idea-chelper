package net.egork.chelper.parser.codechef;

import net.egork.chelper.parser.TaskOptions;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class CodeChefTaskOptions implements TaskOptions {
	public static final CodeChefTaskOptions INSTANCE = new CodeChefTaskOptions();

	private CodeChefTaskOptions() {
	}

	public boolean shouldProvideTestType() {
		return true;
	}

	public boolean shouldProvideInputType() {
		return false;
	}

	public boolean shouldProvideOutputType() {
		return false;
	}

	public boolean shouldProvideHeapMemory() {
		return false;
	}

	public boolean shouldProvideStackMemory() {
		return false;
	}
}
