package net.egork.chelper.parser;

import net.egork.chelper.task.Task;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public interface TaskParser extends Parser {
	public Task parse(String id, Task predefined);
}
