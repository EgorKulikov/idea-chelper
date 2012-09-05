package net.egork.chelper.parser;

import net.egork.chelper.task.Task;

import javax.swing.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public interface Parser {
    public static final Parser[] PARSERS = {new CodeforcesParser(), new CodeChefParser(), new TimusParser(),
		new EOlimpParser(), new RCCParser()};

	public Icon getIcon();
	public String getName();
    public void getContests(DescriptionReceiver receiver);
    public void parseContest(String id, DescriptionReceiver receiver);
    public Task parseTask(Description description);
}
