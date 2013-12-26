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

	public Task parseTaskFromHTML(String html) {
		StringParser parser = new StringParser(html);
		try {
			parser.advance(true, "<div class=\"status__contest inline-block\">");
			String contestName = parser.advance(false, "</div>");
			if (contestName.startsWith("<a"))
				contestName = contestName.substring(contestName.indexOf(">") + 1, contestName.indexOf("</a>"));
			parser.advance(true, "<div class=\"problem-statement\">");
			parser.advance(true, "<h1 class=\"title\">");
			String taskName = parser.advance(false, "</h1>");
			parser.advance(true, "<tr class=\"memory-limit\">");
			parser.advance(true, "<td>");
			String memoryLimit = parser.advance(false, "</td>");
			memoryLimit = memoryLimit.substring(0, memoryLimit.length() - 1);
			parser.advance(true, "<tr class=\"input-file\">");
			if (parser.advanceIfPossible(true, "<td>") == null) {
				parser.advance(true, "<td colspan=\"");
				parser.advance(true, "\">");
			}
			String rawInput = removeTags(parser);
			StreamConfiguration input;
			if (rawInput.contains(" "))
				input = StreamConfiguration.STANDARD;
			else
				input = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, rawInput);
			parser.advance(true, "<tr class=\"output-file\">");
			if (parser.advanceIfPossible(true, "<td>") == null) {
				parser.advance(true, "<td colspan=\"");
				parser.advance(true, "\">");
			}
			String rawOutput = removeTags(parser);
			StreamConfiguration output;
			if (rawOutput.contains(" "))
				output = StreamConfiguration.STANDARD;
			else
				output = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, rawOutput);
			List<Test> tests = new ArrayList<Test>();
			while (parser.advanceIfPossible(true, "<table class=\"sample-tests\">") != null) {
				parser.advance(true, "<tbody>");
				parser.advance(true, "<td><pre>");
				String testInput = parser.advance(false, "</pre></td>");
				parser.advance(true, "<td><pre>");
				String testOutput = parser.advance(false, "</pre></td>");
				tests.add(new Test(testInput, testOutput, tests.size()));
			}
			char problemLetter;
			for (char c = 'A'; ; c++) {
				if (html.indexOf("problems/" + c) != html.lastIndexOf("problems/" + c) ||
					!html.contains("problems/" + c))
				{
					problemLetter = c;
					break;
				}
			}
			return new Task(problemLetter + " - " + taskName, defaultTestType(), input, output, tests.toArray(new Test[tests.size()]), null,
				"-Xmx" + memoryLimit, "Main", "Task" + problemLetter, TokenChecker.class.getCanonicalName(), "",
				new String[0], null, contestName, true, null, null, false, false);
		} catch (ParseException e) {
			return null;
		}
	}

	private String removeTags(StringParser parser) throws ParseException {
		String rawInput = parser.advance(false, "</td>");
		if (rawInput.contains("<center>"))
			rawInput = rawInput.substring(rawInput.indexOf("<center>") + 8, rawInput.indexOf("</center>"));
		return rawInput;
	}
}
