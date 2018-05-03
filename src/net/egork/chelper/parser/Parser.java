package net.egork.chelper.parser;

import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;

import javax.swing.*;
import java.util.Collection;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public interface Parser {
    public static final Parser[] PARSERS = {new CodeforcesParser(), new TimusParser(),
            new RCCParser()};

    public Icon getIcon();

    public String getName();

    public void getContests(DescriptionReceiver receiver);

    public void parseContest(String id, DescriptionReceiver receiver);

    public Task parseTask(Description description);

    public TestType defaultTestType();

    public Collection<Task> parseTaskFromHTML(String html);
}
