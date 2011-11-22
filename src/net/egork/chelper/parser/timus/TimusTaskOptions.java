package net.egork.chelper.parser.timus;

import net.egork.chelper.parser.TaskOptions;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class TimusTaskOptions implements TaskOptions {
	public static final TimusTaskOptions INSTANCE = new TimusTaskOptions();

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
