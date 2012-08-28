package net.egork.chelper.parser;

import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.util.FileUtilities;
import org.apache.commons.lang.StringEscapeUtils;

import javax.swing.*;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
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
		String currentContestPage = null;
		try {
			currentContestPage = FileUtilities.getWebPageContent("http://acm.timus.ru/contest.aspx?locale=en");
		} catch (IOException ignored) {
		}
		if (currentContestPage != null) {
			StringParser parser = new StringParser(currentContestPage);
			try {
				if (parser.advanceIfPossible(true, "No contests scheduled at this time.") == null) {
					parser.advance(true, "<A HREF=\"monitor.aspx?id=");
					String id = parser.advance(false, "\">");
					if (!receiver.isStopped())
						receiver.receiveDescriptions(Collections.singleton(new Description(id, "Current contest")));
					else
						return;
				}
			} catch (ParseException ignored) {}
		}
		String problemsetPage = null;
		try {
			problemsetPage = FileUtilities.getWebPageContent("http://acm.timus.ru/problemset.aspx?locale=en");
		} catch (IOException ignored) {
		}
		if (problemsetPage != null) {
			StringParser parser = new StringParser(problemsetPage);
			int index = 1;
			List<Description> volumes = new ArrayList<Description>();
			while (parser.advanceIfPossible(true, "<A HREF=\"problemset.aspx?space=1&amp;page=") != null) {
				volumes.add(new Description(Integer.toString(-index), "Volume " + index));
				index++;
			}
			if (!receiver.isStopped())
				receiver.receiveDescriptions(volumes);
			else
				return;
		}
		String archivePage = null;
		try {
			archivePage = FileUtilities.getWebPageContent("http://acm.timus.ru/archive.aspx?locale=en");
		} catch (IOException ignored) {
		}
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
			} catch (ParseException ignored) {}
			if (!receiver.isStopped())
				receiver.receiveDescriptions(contests);
		}
	}

	public void parseContest(String id, DescriptionReceiver receiver) {
		String mainPage;
		int index = 1;
		try {
			String url = "http://acm.timus.ru/problemset.aspx?space=" + id + "&locale=en";
			if (Integer.parseInt(id) < 0) {
				url = "http://acm.timus.ru/problemset.aspx?space=1&page=" + id.substring(1) + "&locale=en";
				index = 1000 + 100 * (-Integer.parseInt(id) - 1);
				id = "1";
				if (!receiver.isStopped())
					receiver.receiveDescriptions(Collections.<Description>emptyList());
				else
					return;
			}
			mainPage = FileUtilities.getWebPageContent(url);
		} catch (IOException e) {
			return;
		}
		List<Description> tasks = new ArrayList<Description>();
		StringParser parser = new StringParser(mainPage);
		while (true) {
			try {
				parser.advance(true, "<A HREF=\"problem.aspx?space=" + id + "&amp;num=" + index);
				parser.advance(true, "\">");
				String description = StringEscapeUtils.unescapeHtml(parser.advance(false, "</A>"));
				tasks.add(new Description(id + " " + index++, description));
			} catch (ParseException e) {
				break;
			}
		}
		if (!receiver.isStopped())
			receiver.receiveDescriptions(tasks);
	}

	public Task parseTask(String id) {
		String[] tokens = id.split(" ");
		String url;
		String taskName;
		if (tokens.length != 2)
			return null;
		if (!tokens[0].equals("1")) {
			taskName = "Task" + (char)('A' - 1 + Integer.parseInt(tokens[1]));
		} else {
			taskName = "Task" + tokens[1];
		}
		url = "http://acm.timus.ru/problem.aspx?space=" + tokens[0] + "&num=" + tokens[1] + "&locale=en";
		String text;
		try {
			text = FileUtilities.getWebPageContent(url);
		} catch (IOException e) {
			return null;
		}
		StringParser parser = new StringParser(text);
		try {
			parser.advance(true, "Memory Limit: ");
			Integer heapMemory = Integer.parseInt(parser.advance(false, " "));
			List<Test> tests = new ArrayList<Test>();
			parser.advance(false, "<TABLE CLASS=\"sample\">");
			while (true) {
				try {
					parser.advance(true, "<PRE CLASS=\"intable\">");
					String input = parser.advance(false, "</PRE>");
					parser.advance(true, "<PRE CLASS=\"intable\">");
					String output = parser.advance(false, "</PRE>");
					tests.add(new Test(StringEscapeUtils.unescapeHtml(input),
						StringEscapeUtils.unescapeHtml(output), tests.size()));
				} catch (ParseException e) {
					break;
				}
			}
			return new Task(taskName, null, StreamConfiguration.STANDARD, StreamConfiguration.STANDARD,
				tests.toArray(new Test[tests.size()]), null,
				"-Xmx" + heapMemory + "M -Xss" + Math.min(heapMemory, 64) + "M", null, taskName,
				TokenChecker.class.getCanonicalName(), "", new String[0], null, null, true, null, null);
		} catch (ParseException e) {
			return null;
		}
	}
}
