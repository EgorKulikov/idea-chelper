package net.egork.chelper.parser.eolimp;

import net.egork.chelper.parser.TaskOptions;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class EOlimpOptions implements TaskOptions {
	public static final EOlimpOptions INSTANCE = new EOlimpOptions();

	public boolean shouldProvideTestType() {
		return true;
	}

	public boolean shouldProvideInputType() {
		return true;
	}

	public boolean shouldProvideOutputType() {
		return true;
	}

	public boolean shouldProvideHeapMemory() {
		return false;
	}

	public boolean shouldProvideStackMemory() {
		return false;
	}
}
