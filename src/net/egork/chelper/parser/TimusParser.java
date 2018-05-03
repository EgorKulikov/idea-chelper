package net.egork.chelper.parser;

import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;
import net.egork.chelper.util.FileUtilities;
import org.apache.commons.lang.StringEscapeUtils;

import javax.swing.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class TimusParser implements Parser {
    public Icon getIcon() {
        return IconLoader.getIcon("/icons/timus.png");
    }

    public String getName() {
        return "Timus";
    }

    public void getContests(DescriptionReceiver receiver) {
        String currentContestPage = FileUtilities.getWebPageContent("http://acm.timus.ru/schedule.aspx?locale=en");
        if (currentContestPage != null) {
            StringParser parser = new StringParser(currentContestPage);
            try {
                List<Description> contests = new ArrayList<Description>();
                while (parser.advanceIfPossible(true, "<A HREF=\"contest.aspx?id=") != null) {
                    String id = parser.advance(true, "\">");
                    String description = parser.advance(false, "</A>");
                    contests.add(new Description(id, description));
                }
                if (!receiver.isStopped()) {
                    receiver.receiveDescriptions(contests);
                } else {
                    return;
                }
            } catch (ParseException ignored) {
            }
        }
        String problemsetPage = FileUtilities.getWebPageContent("http://acm.timus.ru/problemset.aspx?locale=en");
        if (problemsetPage != null) {
            StringParser parser = new StringParser(problemsetPage);
            int index = 1;
            List<Description> volumes = new ArrayList<Description>();
            while (parser.advanceIfPossible(true, "<A HREF=\"problemset.aspx?space=1&amp;page=") != null) {
                volumes.add(new Description(Integer.toString(-index), "Volume " + index));
                index++;
            }
            if (!receiver.isStopped()) {
                receiver.receiveDescriptions(volumes);
            } else {
                return;
            }
        }
        String archivePage = FileUtilities.getWebPageContent("http://acm.timus.ru/archive.aspx?locale=en");
        if (archivePage != null) {
            StringParser parser = new StringParser(archivePage);
            List<Description> contests = new ArrayList<Description>();
            String prefix;
            try {
                while ((prefix = parser.advanceIfPossible(true, "<A HREF=\"monitor.aspx?id=")) != null) {
                    String id = parser.advance(false, "\">");
                    StringParser prefixParser = new StringParser(prefix);
                    prefixParser.advance(true, "<H3 CLASS=\"title\">");
                    String description = StringEscapeUtils.unescapeHtml(prefixParser.advance(false, "</H3>"));
                    contests.add(new Description(id, description));
                }
            } catch (ParseException ignored) {
            }
            if (!receiver.isStopped()) {
                receiver.receiveDescriptions(contests);
            }
        }
    }

    public void parseContest(String id, DescriptionReceiver receiver) {
        int index = 1;
        String url = "http://acm.timus.ru/problemset.aspx?space=" + id + "&locale=en";
        if (Integer.parseInt(id) < 0) {
            url = "http://acm.timus.ru/problemset.aspx?space=1&page=" + id.substring(1) + "&locale=en";
            index = 1000 + 100 * (-Integer.parseInt(id) - 1);
            id = "1";
            if (!receiver.isStopped()) {
                receiver.receiveDescriptions(Collections.<Description>emptyList());
            } else {
                return;
            }
        }
        String mainPage = FileUtilities.getWebPageContent(url);
        if (mainPage == null) {
            return;
        }
        List<Description> tasks = new ArrayList<Description>();
        StringParser parser = new StringParser(mainPage);
        while (true) {
            try {
                parser.advance(true, "<A HREF=\"problem.aspx?space=" + id + "&amp;num=" + index);
                parser.advance(true, "\">");
                String description = StringEscapeUtils.unescapeHtml(parser.advance(false, "</A>"));
                if (index < 1000) {
                    description = ((char) (index - 1 + 'A')) + " - " + description;
                } else {
                    description = index + " - " + description;
                }
                tasks.add(new Description(id + " " + index++, description));
            } catch (ParseException e) {
                break;
            }
        }
        if (!receiver.isStopped()) {
            receiver.receiveDescriptions(tasks);
        }
    }

    public Task parseTask(Description description) {
        String id = description.id;
        String[] tokens = id.split(" ");
        String url;
        if (tokens.length != 2) {
            return null;
        }
        String index;
        if (!tokens[0].equals("1")) {
            index = Character.toString((char) ('A' - 1 + Integer.parseInt(tokens[1])));
        } else {
            index = tokens[1];
        }
        url = "http://acm.timus.ru/problem.aspx?space=" + tokens[0] + "&num=" + tokens[1] + "&locale=en";
        String text = FileUtilities.getWebPageContent(url);
        if (text == null) {
            return null;
        }
        StringParser parser = new StringParser(text);
        try {
            parser.advance(true, "Memory limit: ");
            Integer heapMemory = Integer.parseInt(parser.advance(false, " "));
            List<Test> tests = new ArrayList<Test>();
            parser.advance(false, "<TABLE CLASS=\"sample\">");
            parser = new StringParser(parser.advance(false, "</TABLE>"));
            while (true) {
                try {
                    parser.advance(true, "<PRE>");
                    String input = parser.advance(false, "</PRE>");
                    parser.advance(true, "<PRE>");
                    String output = parser.advance(false, "</PRE>");
                    tests.add(new Test(StringEscapeUtils.unescapeHtml(input),
                            StringEscapeUtils.unescapeHtml(output), tests.size()));
                } catch (ParseException e) {
                    break;
                }
            }
            return new Task(description.description, null, StreamConfiguration.STANDARD,
                    StreamConfiguration.STANDARD, tests.toArray(new Test[tests.size()]), null,
                    "-Xmx" + heapMemory + "M -Xss" + Math.min(heapMemory, 64) + "M", "Main", "Task" + index,
                    TokenChecker.class.getCanonicalName(), "", new String[0], null, null, true, null, null, false, false);
        } catch (ParseException e) {
            return null;
        }
    }

    public TestType defaultTestType() {
        return TestType.SINGLE;
    }

    public Collection<Task> parseTaskFromHTML(String html) {
        throw new UnsupportedOperationException();
    }
}
