package net.egork.chelper.parser;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public interface TaskOptions {
	public boolean shouldProvideTestType();
	public boolean shouldProvideInputType();
	public boolean shouldProvideOutputType();
	public boolean shouldProvideHeapMemory();
	public boolean shouldProvideStackMemory();
}
