package net.egork.chelper.parser.codeforces;

import net.egork.chelper.parser.TaskOptions;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CodeforcesTaskOptions implements TaskOptions {
	public static final CodeforcesTaskOptions INSTANCE = new CodeforcesTaskOptions();

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
