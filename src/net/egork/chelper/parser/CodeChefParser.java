package net.egork.chelper.parser;

import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.util.FileUtilities;
import org.apache.commons.lang.StringEscapeUtils;

import javax.swing.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class CodeChefParser implements Parser {
    private final static String EASY_ID = "problems/easy";
    private final static String MEDIUM_ID = "problems/medium";
    private final static String HARD_ID = "problems/hard";
    private final static String CHALLENGE_ID = "challenge/easy";
    private final static String PEER_ID = "problems/extcontest";
    private final static List<String> SPECIAL = Arrays.asList(EASY_ID, MEDIUM_ID, HARD_ID, CHALLENGE_ID, PEER_ID);

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
			if (mainPage != null)
				break;
			if (receiver.isStopped())
				return;
		}
        StringParser parser = new StringParser(mainPage);
        List<Description> contests = new ArrayList<Description>();
        try {
            parser.advance(true, "<h3>Future Contests</h3>");
            StringParser nonPastContestParser = new StringParser(parser.advance(false, "<h3>Past Contests</h3>"));
            while (nonPastContestParser.advanceIfPossible(true, "<tr ><td >") != null) {
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
			while (parser.advanceIfPossible(true, "<tr ><td >") != null) {
				String id = parser.advance(false, "</td>");
				parser.advance(true, "<a href=\"");
				parser.advance(true, "\">");
				String name = StringEscapeUtils.unescapeHtml(parser.advance(false, "</a>"));
				contests.add(new Description(id, name));
			}
		} catch (ParseException ignored) {
		}
		if (!receiver.isStopped())
			receiver.receiveDescriptions(contests);
	}

    private Collection<Description> buildSpecial() {
        List<Description> special = new ArrayList<Description>();
        special.add(new Description(EASY_ID, "Easy problems"));
        special.add(new Description(MEDIUM_ID, "Medium problems"));
        special.add(new Description(HARD_ID, "Hard problems"));
        special.add(new Description(CHALLENGE_ID, "Challenge problems"));
        special.add(new Description(PEER_ID, "External contests problems"));
        return special;
    }

	public void parseContest(String id, DescriptionReceiver receiver) {
		String mainPage;
		while (true) {
			mainPage = FileUtilities.getWebPageContent("http://www.codechef.com/" + id);
			if (mainPage != null)
				break;
			if (receiver.isStopped())
				return;
		}
		List<Description> tasks = new ArrayList<Description>();
		StringParser parser = new StringParser(mainPage);
		try {
			parser.advance(true, "Accuracy</a></th>");
			parser = new StringParser(parser.advance(false, "</tbody>"));
		} catch (ParseException e) {
			return;
		}
		if (SPECIAL.contains(id)) {
			id = "";
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
		if (!receiver.isStopped())
			receiver.receiveDescriptions(tasks);
	}

	public Task parseTask(Description description) {
		String id = description.id;
		String[] tokens = id.split(" ");
		if (tokens.length > 2 || tokens.length == 0)
			return null;
		String url;
		if (tokens.length == 2)
			url = "http://www.codechef.com/" + tokens[0] + "/problems/" + tokens[1];
		else
			url = "http://www.codechef.com/problems/" + tokens[0];
		String text = FileUtilities.getWebPageContent(url);
		if (text == null)
			return null;
		StringParser parser = new StringParser(text);
		Pattern pattern = Pattern.compile(".*<p>.*</p>.*", Pattern.DOTALL);
		try {
			parser.advance(false, "<div class=\"prob\">");
			parser.advance(true, "<h1>");
			String taskID = getTaskID(parser.advance(false, "</h1>"));
			parser.dropTail("<table cellspacing=\"0\" cellpadding=\"0\" align=\"left\">");
			List<Test> tests = new ArrayList<Test>();
			int index = 0;
			while (true) {
				try {
					parser.advance(true, "Input", "Sample input", "Sample Input");
					if (parser.length() != 0 && parser.charAt(0) == ':')
						parser.advance(1);
					String input = parser.advance(true, "Output", "Sample output", "Sample Output");
					if (parser.length() != 0 && parser.charAt(0) == ':')
						parser.advance(1);
					String output = parser.advance(false, "Input", "Sample input", "Sample Input", "<b>",
						"<h", "</div>", "<p>");
					if (pattern.matcher(input).matches() || input.contains("</p><p>"))
						continue;
					input = dropTags(input).replace("<br />\n", "\n").replace("<br />", "\n");
					output = dropTags(output).replace("<br />\n", "\n").replace("<br />", "\n");
					if (input.contains("<") || output.contains("<"))
						continue;
					tests.add(new Test(StringEscapeUtils.unescapeHtml(input), StringEscapeUtils.unescapeHtml(output),
						index++));
				} catch (ParseException e) {
					break;
				}
			}
            return new Task(description.description, null, StreamConfiguration.STANDARD, StreamConfiguration.STANDARD,
                    tests.toArray(new Test[tests.size()]), null, "-Xmx64M", null, taskID,
                    TokenChecker.class.getCanonicalName(), "", new String[0], null, null, true, null, null);
		} catch (ParseException e) {
			return null;
		}
	}

    private String dropTags(String s) {
		int bracket = 0;
		while (s.length() != 0) {
			char c = s.charAt(0);
			if (c == '<')
				bracket++;
			else if (bracket == 0 && c != ' ' && c != '\n')
				break;
			else if (c == '>')
				bracket--;
			s = s.substring(1);
		}
		while (s.length() != 0) {
			char c = s.charAt(s.length() - 1);
			if (c == '>')
				bracket++;
			else if (bracket == 0 && c != ' ' && c != '\n')
				break;
			else if (c == '<')
				bracket--;
			s = s.substring(0, s.length() - 1);
		}
		return s + "\n";
	}

	private String getTaskID(String title) {
		boolean shouldBeCapital = true;
		StringBuilder id = new StringBuilder();
		for (int i = 0; i < title.length(); i++) {
			if (Character.isLetter(title.charAt(i))) {
				if (shouldBeCapital) {
					shouldBeCapital = false;
					id.append(Character.toUpperCase(title.charAt(i)));
				} else
					id.append(title.charAt(i));
			} else if (title.charAt(i) == ' ')
				shouldBeCapital = true;
		}
		return id.toString();
	}
}
