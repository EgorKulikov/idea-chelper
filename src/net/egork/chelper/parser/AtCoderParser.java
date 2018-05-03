package net.egork.chelper.parser;

import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;
import org.apache.commons.lang.StringEscapeUtils;

import javax.swing.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author egorku@yandex-team.ru
 */
public class AtCoderParser implements Parser {
    public Icon getIcon() {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        return "AtCoder";
    }

    public void getContests(DescriptionReceiver receiver) {
        throw new UnsupportedOperationException();
    }

    public void parseContest(String id, DescriptionReceiver receiver) {
        throw new UnsupportedOperationException();
    }

    public Task parseTask(Description description) {
        throw new UnsupportedOperationException();
    }

    public TestType defaultTestType() {
        return TestType.SINGLE;
    }

    public Collection<Task> parseTaskFromHTML(String html) {
        StringParser parser = new StringParser(html);
        try {
            parser.advance(true, "<span class=\"contest-name\">");
            String contestName = parser.advance(false, "</span>");
            parser.advance(true, "<h2>");
            String taskName = parser.advance(false, "</h2>");
            parser.advance(true, "Memory limit</span>");
            parser.advance(true, " : ");
            String memoryLimit = parser.advance(false, "</p>");
            memoryLimit = memoryLimit.substring(0, memoryLimit.length() - 1);
            StreamConfiguration input = StreamConfiguration.STANDARD;
            StreamConfiguration output = StreamConfiguration.STANDARD;
            List<Test> tests = new ArrayList<Test>();
            while (parser.advanceIfPossible(true, "<h3>Sample Input") != null) {
                parser.advance(true, "<pre>");
                String testInput = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre>"));
                parser.advance(true, "<pre>");
                String testOutput = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre>"));
                tests.add(new Test(testInput, testOutput, tests.size()));
            }
            String letter = Character.toString(taskName.charAt(0));
            return Collections.singleton(new Task(taskName, defaultTestType(), input, output, tests.toArray(new Test[tests.size()]), null,
                    "-Xmx" + memoryLimit, "Main", "Task" + letter, TokenChecker.class.getCanonicalName(), "",
                    new String[0], null, contestName, true, null, null, false, false));
        } catch (ParseException e) {
            return Collections.emptyList();
        }
    }

}
