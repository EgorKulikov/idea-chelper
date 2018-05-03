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
 * @author egor@egork.net
 */
public class KattisParser implements Parser {
    public Icon getIcon() {
        return IconLoader.getIcon("/icons/kattis.png");
    }

    public String getName() {
        return "Kattis";
    }

    public void getContests(DescriptionReceiver receiver) {
        String contestsPage = FileUtilities.getWebPageContent("https://open.kattis.com/contests");
        if (contestsPage == null) {
            return;
        }
        List<Description> contests = new ArrayList<Description>();
        StringParser parser = new StringParser(contestsPage);
        try {
            for (int i = 0; i < 3; i++) {
                parser.advance(true, "<tbody>");
                StringParser currentPart = new StringParser(parser.advance(true, "</tbody>"));
                while (currentPart.advanceIfPossible(true, "<a href=\"/contests/") != null) {
                    String id = currentPart.advance(true, "\">");
                    String name = StringEscapeUtils.unescapeHtml(currentPart.advance(false, "</a>"));
                    contests.add(new Description(id, name));
                }
                if (!receiver.isStopped()) {
                    receiver.receiveDescriptions(contests);
                } else {
                    return;
                }
                contests = new ArrayList<Description>();
            }
        } catch (ParseException ignored) {
        }
        int left = -1;
        int right = 1;
        while (true) {
            if (isNotEmpty(right)) {
                left = right;
                right *= 2;
            } else {
                right--;
                break;
            }
        }
        while (left < right) {
            int middle = (left + right + 1) >> 1;
            if (isNotEmpty(middle)) {
                left = middle;
            } else {
                right = middle - 1;
            }
        }
        for (int i = 0; i <= left; i++) {
            contests.add(new Description("Archive " + i, "Archive page " + i));
        }
        receiver.receiveDescriptions(contests);
    }

    private boolean isNotEmpty(int index) {
        String page = FileUtilities.getWebPageContent("https://open.kattis.com/problems?page=" + index);
        if (page == null) {
            return false;
        }
        StringParser parser = new StringParser(page);
        try {
            parser.advance(true, "<tbody>");
            return parser.advanceIfPossible(true, "<td class=\"name_column\">") != null;
        } catch (ParseException e) {
            return false;
        }
    }

    public void parseContest(String id, DescriptionReceiver receiver) {
        if (id.indexOf(' ') == -1) {
            String mainPage = FileUtilities.getWebPageContent("https://open.kattis.com/contests/" + id + "/problems");
            if (mainPage == null) {
                return;
            }
            List<Description> ids = new ArrayList<Description>();
            StringParser parser = new StringParser(mainPage);
            try {
                parser.advance(true, "<table id=\"contest_problem_list\"");
                parser.advance(true, "<tbody>");
                parser = new StringParser(parser.advance(false, "</tbody>"));
                while (parser.advanceIfPossible(true, "<th class=\"problem_letter\">") != null) {
                    String letter = parser.advance(false, "</th>");
                    parser.advance(true, "<a href=\"");
                    String taskId = parser.advance(true, "\">");
                    String title = StringEscapeUtils.unescapeHtml(parser.advance(false, "</a>"));
                    ids.add(new Description(taskId + " " + letter, title));
                }
            } catch (ParseException ignored) {
            }
            if (!receiver.isStopped()) {
                receiver.receiveDescriptions(ids);
            }
        } else {
            String mainPage = FileUtilities.getWebPageContent(
                    "https://open.kattis.com/problems?page=" + id.substring(id.indexOf(' ') + 1));
            if (mainPage == null) {
                return;
            }
            List<Description> ids = new ArrayList<Description>();
            StringParser parser = new StringParser(mainPage);
            try {
                parser.advance(true, "<tbody>");
                while (parser.advanceIfPossible(true, "<td class=\"name_column\"><a href=\"") != null) {
                    String taskID = parser.advance(true, "\">");
                    String title = StringEscapeUtils.unescapeHtml(parser.advance(false, "</a>"));
                    ids.add(new Description(taskID, title));
                }
            } catch (ParseException ignored) {
            }
            if (!receiver.isStopped()) {
                receiver.receiveDescriptions(ids);
            }
        }
    }

    public Task parseTask(Description description) {
        int space = description.id.indexOf(' ');
        String id = space == -1 ?
                description.id : description.id.substring(0, space);
        String text = FileUtilities.getWebPageContent("https://open.kattis.com" + id);
        if (text == null) {
            return null;
        }
        Collection<Task> tasks = parseTaskFromHTML(text);
        if (!tasks.isEmpty()) {
            Task task = tasks.iterator().next();
            if (space != -1) {
                String letter = description.id.substring(space + 1);
                task = task.setTaskClass("Task" + letter);
            }
            return task;
        }
        return null;
    }

    public TestType defaultTestType() {
        return TestType.SINGLE;
    }

    public Collection<Task> parseTaskFromHTML(String html) {
        StringParser parser = new StringParser(html);
        try {
            String contestName = "Kattis Archive";
            if (parser.advanceIfPossible(true, "<div id=\"contest_time\">") != null) {
                parser.advance(true, "<h2 class=\"title\">");
                contestName = parser.advance(false, "</h2>");
            }
            parser.advance(true, "<div class=\"headline-wrapper\"><h1>");
            String taskName = StringEscapeUtils.unescapeHtml(parser.advance(false, "</h1>")).
                    replace("<br/>", " - ").replace("<br>", " - ");
            List<Test> tests = new ArrayList<Test>();
            while (parser.advanceIfPossible(true, "<table class=\"sample\" summary=\"sample data\">") != null) {
                parser.advance(true, "<pre>");
                String testInput = parser.advance(false, "</pre>").trim() + "\n";
                parser.advance(true, "<pre>");
                String testOutput = parser.advance(false, "</pre>").trim() + "\n";
                tests.add(new Test(StringEscapeUtils.unescapeHtml(testInput),
                        StringEscapeUtils.unescapeHtml(testOutput), tests.size()));
            }
            parser.advance(true, "<p><strong>Memory limit: </strong>");
            String heapMemory = parser.advance(false, " ") + "M";
            String taskClass = CodeChefParser.getTaskID(taskName);
            return Collections.singleton(new Task(taskName, defaultTestType(), StreamConfiguration.STANDARD,
                    StreamConfiguration.STANDARD, tests.toArray(new Test[tests.size()]), null,
                    "-Xmx" + heapMemory + " -Xss8M", "Main", taskClass, TokenChecker.class.getCanonicalName(), "",
                    new String[0], null, contestName, true, null, null, false, false));
        } catch (ParseException e) {
            return Collections.emptyList();
        }
    }
}
