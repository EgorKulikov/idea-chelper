package net.egork.chelper.parser;

import java.util.Collection;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public interface ContestParser extends Parser {
	public Collection<String> parse(String id);
	public TaskParser getTaskParser();
}
