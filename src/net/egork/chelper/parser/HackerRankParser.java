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
public class HackerRankParser implements Parser {
    public Icon getIcon() {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        return "HackerRank";
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
            parser.advance(true, "data-analytics=\"Breadcrumb\"");
            List<String> breadCrumbs = new ArrayList<>();
            while (parser.advanceIfPossible(true, "data-analytics=\"Breadcrumb\"") != null) {
                if (parser.advanceIfPossible(true, "data-attr1=\"") != null) {
                    breadCrumbs.add(parser.advance(true, "\""));
                }

            }
            String contestName = breadCrumbs.get(breadCrumbs.size() - 2);
            String taskName = breadCrumbs.get(breadCrumbs.size() - 1);
            String taskClass = CodeChefParser.getTaskID(taskName);
            StreamConfiguration input = StreamConfiguration.STANDARD;
            StreamConfiguration output = StreamConfiguration.STANDARD;
            List<Test> tests = new ArrayList<Test>();
            while (parser.advanceIfPossible(true, "<div class=\"challenge_sample_input\">") != null) {
                parser.advance(true, "<span class=\"err\">");
                String testInput = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre>")).
                        replace("</span>", "").replace("<span class=\"err\">", "");
                parser.advance(true, "<span class=\"err\">");
                String testOutput = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre>")).
                        replace("</span>", "").replace("<span class=\"err\">", "");
                tests.add(new Test(testInput, testOutput, tests.size()));
            }
            return Collections.singleton(new Task(taskName, defaultTestType(), input, output, tests.toArray(new Test[tests.size()]), null,
                    "-Xmx256M", "Solution", taskClass, TokenChecker.class.getCanonicalName(), "",
                    new String[0], null, contestName, true, null, null, false, false));
        } catch (ParseException e) {
            return Collections.emptyList();
        }
    }
}
