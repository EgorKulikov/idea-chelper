package net.egork.chelper.parser;

import net.egork.chelper.task.Task;

import javax.swing.*;
import java.util.Collection;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public interface Parser {
    public static final Parser[] PARSERS = {new CodeforcesParser(), new CodeChefParser()};

	public Icon getIcon();
	public String getName();
    public Collection<Description> getContests(DescriptionReceiver receiver);
    public Collection<Description> parseContest(String id, DescriptionReceiver receiver);
    public Task parseTask(String id);
    public void stopAdditionalContestSending();
    public void stopAdditionalTaskSending();
}
