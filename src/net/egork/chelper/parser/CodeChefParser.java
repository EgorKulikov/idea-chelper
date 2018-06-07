package net.egork.chelper.parser;

import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.TaskUtilities;
import org.apache.commons.lang.StringEscapeUtils;

import javax.swing.*;
import java.text.ParseException;
import java.util.*;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class CodeChefParser implements Parser {
    private final static String EASY_ID = "problems/easy";
    private final static String MEDIUM_ID = "problems/medium";
    private final static String HARD_ID = "problems/hard";
    private final static String CHALLENGE_ID = "challenge/easy";
    private final static String PEER_ID = "problems/extcontest";
    private final static String SCHOOL_ID = "problems/school";
    private final static List<String> SPECIAL = Arrays.asList(EASY_ID, MEDIUM_ID, HARD_ID, CHALLENGE_ID, PEER_ID, SCHOOL_ID);

    public Icon getIcon() {
        return IconLoader.getIcon("/icons/codechef.png");
    }

    public String getName() {
        return "CodeChef";
    }

    public void getContests(DescriptionReceiver receiver) {
        String mainPage;
        while (true) {
            mainPage = FileUtilities.getWebPageContent("http://www.codechef.com/contests");
            if (mainPage != null) {
                break;
            }
            if (receiver.isStopped()) {
                return;
            }
        }
        StringParser parser = new StringParser(mainPage);
        List<Description> contests = new ArrayList<Description>();
        try {
            parser.advance(true, "<h3>Present Contests</h3>");
            StringParser nonPastContestParser = new StringParser(parser.advance(false, "<h3>Past Contests</h3>"));
            while (nonPastContestParser.advanceIfPossible(true, "<tr>") != null) {
                nonPastContestParser.advance(true, "<td>");
                String id = nonPastContestParser.advance(false, "</td>");
                nonPastContestParser.advance(true, "<a href=\"");
                nonPastContestParser.advance(true, "\">");
                String name = StringEscapeUtils.unescapeHtml(nonPastContestParser.advance(false, "</a>"));
                contests.add(new Description(id, name));
            }
        } catch (ParseException ignored) {
        }
        contests.addAll(buildSpecial());
        if (!receiver.isStopped()) {
            receiver.receiveDescriptions(contests);
        } else {
            return;
        }
        contests = new ArrayList<Description>();
        try {
            while (parser.advanceIfPossible(true, "<tr>") != null) {
                parser.advance(true, "<td>");
                String id = parser.advance(false, "</td>");
                parser.advance(true, "<a href=\"");
                parser.advance(true, "\">");
                String name = StringEscapeUtils.unescapeHtml(parser.advance(false, "</a>"));
                contests.add(new Description(id, name));
            }
        } catch (ParseException ignored) {
        }
        if (!receiver.isStopped()) {
            receiver.receiveDescriptions(contests);
        }
    }

    private Collection<Description> buildSpecial() {
        List<Description> special = new ArrayList<Description>();
        special.add(new Description(EASY_ID, "Easy problems"));
        special.add(new Description(MEDIUM_ID, "Medium problems"));
        special.add(new Description(HARD_ID, "Hard problems"));
        special.add(new Description(CHALLENGE_ID, "Challenge problems"));
        special.add(new Description(PEER_ID, "External contests problems"));
        special.add(new Description(SCHOOL_ID, "School problems"));
        return special;
    }

    public void parseContest(String id, DescriptionReceiver receiver) {
        String mainPage = FileUtilities.getWebPageContent("http://www.codechef.com/" + id);
        if (mainPage == null) {
            return;
        }
        if (SPECIAL.contains(id)) {
            id = "";
            if (receiver.isStopped()) {
                return;
            }
            receiver.receiveDescriptions(Collections.<Description>emptyList());
        }
        List<Description> tasks = new ArrayList<Description>();
        StringParser parser = new StringParser(mainPage);
        try {
            parser.advance(true, "Accuracy</a></th>");
            parser = new StringParser(parser.advance(false, "</tbody>"));
        } catch (ParseException e) {
            return;
        }
        while (true) {
            try {
                parser.advance(true, id + "/problems/");
                String taskID;
                if (id.length() == 0) {
                    taskID = parser.advance(false, "\"");
                } else {
                    taskID = id + " " + parser.advance(false, "\"");
                }
                parser.advance(true, "<b>");
                String name = StringEscapeUtils.unescapeHtml(parser.advance(false, "</b>"));
                tasks.add(new Description(taskID, name));
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
        if (tokens.length > 2 || tokens.length == 0) {
            return null;
        }
        String url;
        if (tokens.length == 2) {
            url = "http://www.codechef.com/" + tokens[0] + "/problems/" + tokens[1];
        } else {
            url = "http://www.codechef.com/problems/" + tokens[0];
        }
        String text = FileUtilities.getWebPageContent(url);
        if (text == null) {
            return null;
        }
        Collection<Task> tasks = parseTaskFromHTML(text);
        if (tasks.isEmpty()) {
            return null;
        }
        return tasks.iterator().next();
    }

    public TestType defaultTestType() {
        return TestType.MULTI_NUMBER;
    }

    public Collection<Task> parseTaskFromHTML(String text) {
        StringParser parser = new StringParser(text);
        try {
            parser.advance(true, "<aside class=\"breadcrumbs\">");
            parser.advance(true, "<a href=\"");
            parser.advance(true, "<a href=\"");
            parser.advance(true, "<a id");
            parser.advance(true, "\">");
            String contestName = parser.advance(false, "</a>");
            parser.advance(true, "&nbsp;");
            parser.advance(true, "&nbsp;");
            String taskName = parser.advance(false, "</aside>");
            String taskID = getTaskID(taskName);
            List<Test> tests = new ArrayList<Test>();
            int index = 0;
            if (parser.advanceIfPossible(true, "<h3>Example</h3>") != null) {
                parser = new StringParser(parser.advance(true, "<h3>", "<!--.problem-statement-->"));
                while (parser.advanceIfPossible(true, "</b>") != null) {
                    String input = StringEscapeUtils.unescapeHtml(parser.advance(false, "<b>")).trim();
                    parser.advance(true, "</b>");
                    String output = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre>")).trim();
                    tests.add(new Test(input + "\n", output + "\n", index));
                }
            } else {
                while (parser.advanceIfPossible(true, "<h3>Sample Input", "<h3>Sample input") != null) {
                    parser.advance(true, "<pre>");
                    String input = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre>"));
                    parser.advance(true, "<pre>");
                    String output = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre>"));
                    tests.add(new Test(input + "\n", output + "\n", index++));
                }
            }
            return Collections.singleton(new Task(taskName, defaultTestType(), StreamConfiguration.STANDARD,
                    StreamConfiguration.STANDARD, tests.toArray(new Test[tests.size()]), null, "-Xmx1536M", "Main", taskID,
                    TokenChecker.class.getCanonicalName(), "", new String[0], null, contestName, true, null, null, false,
                    false));
        } catch (ParseException e) {
            return Collections.emptySet();
        }
    }

    public static String getTaskID(String title) {
        boolean shouldBeCapital = true;
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < title.length(); i++) {
            if (Character.isLetter(title.charAt(i)) || id.length() > 0 && Character.isDigit(title.charAt(i))) {
                if (shouldBeCapital) {
                    shouldBeCapital = false;
                    id.append(Character.toUpperCase(title.charAt(i)));
                } else {
                    id.append(title.charAt(i));
                }
            } else if (title.charAt(i) == ' ') {
                shouldBeCapital = true;
            }
        }
        return TaskUtilities.createClassName(id.toString());
    }
}
