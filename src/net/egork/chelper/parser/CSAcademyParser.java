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
public class CSAcademyParser implements Parser {
    public Icon getIcon() {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        return "CS Academy";
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
            String prefix = parser.advance(true, "<div class=\"text-center\"><h1>");
            String taskName = parser.advance(false, "</h1>");
            parser.advance(true, "<br>Memory limit: <em>");
            String memoryLimit = parser.advance(false, "B").replace(" ", "");
            StreamConfiguration input = StreamConfiguration.STANDARD;
            StreamConfiguration output = StreamConfiguration.STANDARD;
            List<Test> tests = new ArrayList<Test>();
            while (parser.advanceIfPossible(true, "<td><pre>") != null) {
                String testInput = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre></td>"));
                parser.advance(true, "<td><pre>");
                String testOutput = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre></td>"));
                tests.add(new Test(testInput, testOutput, tests.size()));
            }
            parser = new StringParser(prefix);
            parser.advance(true, "<a href=\"/contest/archive/\"");
            parser.advance(true, "<a href=\"/contest/");
            String contestName = parser.advance(false, "/");
            contestName = contestName.replace('-', ' ');
            for (int i = 0; i < contestName.length(); i++) {
                if (i == 0 || contestName.charAt(i - 1) == ' ') {
                    contestName = contestName.substring(0, i) + Character.toUpperCase(contestName.charAt(i)) +
                            contestName.substring(i + 1);
                }
            }
            return Collections.singleton(new Task(taskName, defaultTestType(), input, output, tests.toArray(new Test[tests.size()]), null,
                    "-Xmx" + memoryLimit, "Main", CodeChefParser.getTaskID(taskName), TokenChecker.class.getCanonicalName(), "",
                    new String[0], null, contestName, true, null, null, false, false));
        } catch (ParseException e) {
            return Collections.emptyList();
        }
    }

}
