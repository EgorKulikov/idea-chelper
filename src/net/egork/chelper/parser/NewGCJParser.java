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
public class NewGCJParser implements Parser {
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
        return TestType.MULTI_NUMBER;
    }

    public Collection<Task> parseTaskFromHTML(String html) {
        StringParser parser = new StringParser(html);
        try {
            parser.advance(true, "<div class=\"challenge__title\"><h4>");
            String contestName = parser.advance(false, "</h4>");
            parser.advance(true, "class=\"collection-item router-link-exact-active active\">");
            String taskName = parser.advance(false, "</a>", "<br");
            parser.advance(true, "<h3>Limits</h3>");
            parser.advance(true, "Memory limit: ");
            String memoryLimit = parser.advance(false, "B").replace(" ", "");
            StreamConfiguration input = StreamConfiguration.STANDARD;
            StreamConfiguration output = StreamConfiguration.STANDARD;
            List<Test> tests = new ArrayList<Test>();
            if (parser.advanceIfPossible(true, "<pre class=\"io-content\">") != null) {
                String testInput = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre></td>"));
                parser.advance(true, "<pre class=\"io-content\">");
                String testOutput = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre></td>"));
                tests.add(new Test(testInput, testOutput, tests.size()));
            }
            return Collections.singleton(new Task(taskName, defaultTestType(), input, output, tests.toArray(new Test[tests.size()]), null,
                    "-Xmx" + memoryLimit, "Solution", CodeChefParser.getTaskID(taskName), TokenChecker.class.getCanonicalName(), "",
                    new String[0], null, contestName, true, null, null, false, false));
        } catch (ParseException e) {
            return Collections.emptyList();
        }
    }

}
