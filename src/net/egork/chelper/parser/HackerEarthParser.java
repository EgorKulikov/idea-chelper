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
public class HackerEarthParser implements Parser {
	public Icon getIcon() {
		throw new UnsupportedOperationException();
	}

	public String getName() {
		return "HackerEarth";
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
			String contestName = "";
			if (parser.advanceIfPossible(true, "<div class=\"cover\">") != null) {
				parser.advance(true, "<div class=\"title-container\">");
				parser.advance(true, "<a href");
				parser.advance(true, ">");
				contestName = StringEscapeUtils.unescapeHtml(parser.advance(false, "</a>").trim().replace('/', '-'));
			}
			parser.advance(true, "<div class=\"programming\"");
			parser.advance(true, "<span class=\"dark\">");
			String taskName = StringEscapeUtils.unescapeHtml(parser.advance(false, "</span>")).trim();
			String taskClass = CodeChefParser.getTaskID(taskName);
			StreamConfiguration	input = StreamConfiguration.STANDARD;
			StreamConfiguration output = StreamConfiguration.STANDARD;
			List<Test> tests = new ArrayList<Test>();
			parser.advance(true, "Sample Input</div>");
			parser.advance(true, "<pre>");
			String testInput = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre>"));
			parser.advance(true, "Sample Output</div>");
			parser.advance(true, "<pre>");
			String testOutput = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre>"));
			tests.add(new Test(testInput, testOutput, tests.size()));
			parser.advance(true, "<span>Memory Limit:");
			parser.advance(true, "<span>");
			String ml = parser.advance(false, " ");
			return Collections.singleton(new Task(taskName, defaultTestType(), input, output, tests.toArray(new Test[tests.size()]), null,
				"-Xmx" + ml + "M", "Main", taskClass, TokenChecker.class.getCanonicalName(), "",
				new String[0], null, contestName, true, null, null, false, false));
		} catch (ParseException e) {
			return Collections.emptyList();
		}
	}
}
