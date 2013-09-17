package net.egork.chelper.parser;

import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;

import javax.swing.*;
import java.text.ParseException;
import java.util.ArrayList;
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

	public Task parseTaskFromHTML(String html) {
		StringParser parser = new StringParser(html);
		try {
			parser.advance(true, "<div class=\"challenge-header\">");
			String header = parser.advance(true, "</ul></div>");
			StringParser headerParser = new StringParser(header);
			//noinspection StatementWithEmptyBody
			while (headerParser.advanceIfPossible(true, "<li>") != null);
			headerParser.advance(true, "class=\"backbone\">");
			String contestName = headerParser.advance(false, "</a>").trim().replace('/', '-');
			parser.advance(true, "<h1");
			parser.advance(true, "\">");
			String taskName = parser.advance(false, "</h1>").trim();
			String taskClass = CodeChefParser.getTaskID(taskName);
			StreamConfiguration	input = StreamConfiguration.STANDARD;
			StreamConfiguration output = StreamConfiguration.STANDARD;
			List<Test> tests = new ArrayList<Test>();
			while (parser.advanceIfPossible(true, "<strong>Sample Input") != null) {
				parser.advance(true, "<pre><code>");
				String testInput = parser.advance(false, "</code></pre>");
				parser.advance(true, "<strong>Sample Output");
				parser.advance(true, "<pre><code>");
				String testOutput = parser.advance(false, "</code></pre>");
				tests.add(new Test(testInput, testOutput, tests.size()));
			}
			return new Task(taskName, defaultTestType(), input, output, tests.toArray(new Test[tests.size()]), null,
				"-Xmx256M", "Solution", taskClass, TokenChecker.class.getCanonicalName(), "",
				new String[0], null, contestName, true, null, null, false, false);
		} catch (ParseException e) {
			return null;
		}
	}
}
