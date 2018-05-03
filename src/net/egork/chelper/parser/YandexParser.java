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
public class YandexParser implements Parser {
    public Icon getIcon() {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        return "Yandex";
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
            parser.advance(true, "<div class=\"contest-head__item contest-head__item_role_title\">");
            String contestName = parser.advance(false, "</div>");
            if (contestName.startsWith("<a")) {
                contestName = contestName.substring(contestName.indexOf(">") + 1, contestName.indexOf("</a>"));
            }
            parser.advance(true, "<div class=\"problem-statement\">");
            parser.advance(true, "<h1 class=\"title\">");
            String taskName = parser.advance(false, "</h1>");
            parser.advance(true, "<tr class=\"memory-limit\">");
            parser.advance(true, "<td>");
            String memoryLimit = parser.advance(false, "</td>");
            memoryLimit = memoryLimit.substring(0, memoryLimit.length() - 1);
            parser.advance(true, "<tr class=\"input-file\">");
            StreamConfiguration input = getStreamConfiguration(parser);
            parser.advance(true, "<tr class=\"output-file\">");
            StreamConfiguration output = getStreamConfiguration(parser);
            List<Test> tests = new ArrayList<Test>();
            while (parser.advanceIfPossible(true, "<table class=\"sample-tests\">") != null) {
                parser.advance(true, "<tbody>");
                parser.advance(true, "<td><pre>");
                String testInput = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre></td>"));
                parser.advance(true, "<td><pre>");
                String testOutput = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre></td>"));
                tests.add(new Test(testInput, testOutput, tests.size()));
            }
            parser.advance(true, "tabs-menu_role_problems");
            parser.advance(true, "tabs-menu__tab_active_yes");
            parser.advance(true, "href=");
            parser.advance(true, "/problems/");
            String letter = parser.advance(false, "/");
            return Collections.singleton(new Task(letter + " - " + taskName, defaultTestType(), input, output, tests.toArray(new Test[tests.size()]), null,
                    "-Xmx" + memoryLimit, "Main", "Task" + letter, TokenChecker.class.getCanonicalName(), "",
                    new String[0], null, contestName, true, null, null, false, false));
        } catch (ParseException e) {
            return Collections.emptyList();
        }
    }

    private StreamConfiguration getStreamConfiguration(StringParser parser) throws ParseException {
        StringParser inputParser = new StringParser(parser.advance(false, "</tr>"));
        if (inputParser.advanceIfPossible(true, "<td>") == null) {
            inputParser.advance(true, "<td colspan=\"");
            inputParser.advance(true, "\">");
        }
        String rawInput = removeTags(inputParser);
        StreamConfiguration input;
        if (rawInput.contains(" ") || rawInput.contains("/")) {
            input = StreamConfiguration.STANDARD;
        } else {
            input = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, rawInput);
        }
        return input;
    }

    private String removeTags(StringParser parser) throws ParseException {
        String rawInput = parser.advance(false, "</td>");
        if (rawInput.contains("<center>")) {
            rawInput = rawInput.substring(rawInput.indexOf("<center>") + 8, rawInput.indexOf("</center>"));
        }
        return rawInput;
    }
}
