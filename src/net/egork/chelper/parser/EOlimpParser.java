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
import java.util.Collections;
import java.util.List;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class EOlimpParser implements Parser {
	public Icon getIcon() {
		return IconLoader.getIcon("/icons/eolimp.png");
	}

	public String getName() {
		return "E-Olimp";
	}

	public void getContests(DescriptionReceiver receiver) {
		int lastContestPage = 0;
		String contestPage = FileUtilities.getWebPageContent("http://www.e-olimp.com/en/competitions");
		if (contestPage != null) {
			StringParser parser = new StringParser(contestPage);
			String lastPage = null;
			try {
				while (parser.advanceIfPossible(true, "page:") != null) {
					String page = parser.advance(false, "' class='page'");
					if (!"1".equals(page) || lastPage == null)
						lastPage = page;
				}
			} catch (ParseException ignored) {
			}
			try {
				lastContestPage = Integer.parseInt(lastPage);
			} catch (NumberFormatException ignored) {}
		}
		if (receiver.isStopped())
			return;
		for (int i = 0; i <= lastContestPage; i++) {
			contestPage = FileUtilities.getWebPageContent("http://www.e-olimp.com/en/competitions/page:" + i);
			if (contestPage != null) {
				StringParser parser = new StringParser(contestPage);
				List<Description> descriptions = new ArrayList<Description>();
				try {
					if (i == 0)
						parser.advance(true, "<h1>CurrentCompetitions</h1>");
					else
						parser.advance(true, "<h1>Competitions</h1>");
					while (parser.advanceIfPossible(true, "href='competitions/") != null) {
						String id = parser.advance(true, "'>");
						String name = parser.advance(false, "</a>");
						descriptions.add(new Description(id, name));
					}
				} catch (ParseException ignored) {}
				if (receiver.isStopped())
					return;
				receiver.receiveDescriptions(descriptions);
			}
		}
		String archivePage = FileUtilities.getWebPageContent("http://www.e-olimp.com/en/problems");
		if (archivePage != null) {
			StringParser parser = new StringParser(archivePage);
			String lastPage = null;
			try {
				while (parser.advanceIfPossible(true, "page:") != null) {
					String page = parser.advance(false, "' class='page'");
					if (!"1".equals(page))
						lastPage = page;
				}
			} catch (ParseException ignored) {
			}
			try {
				int maxPage = Integer.parseInt(lastPage);
				List<Description> pages = new ArrayList<Description>();
				for (int i = 0; i <= maxPage; i++)
					pages.add(new Description("page:" + i, "Archive page " + (i + 1)));
				if (receiver.isStopped())
					return;
				receiver.receiveDescriptions(pages);
			} catch (NumberFormatException ignored) {}
		}
		int lastPastContestPage = -1;
		String pastContestPage = FileUtilities.getWebPageContent("http://www.e-olimp.com/en/competitions-history");
		if (pastContestPage != null) {
			StringParser parser = new StringParser(pastContestPage);
			String lastPage = null;
			try {
				while (parser.advanceIfPossible(true, "page:") != null) {
					String page = parser.advance(false, "' class='page'");
					if (!"1".equals(page))
						lastPage = page;
				}
			} catch (ParseException ignored) {
			}
			try {
				lastPastContestPage = Integer.parseInt(lastPage);
			} catch (NumberFormatException ignored) {}
		}
		if (receiver.isStopped())
			return;
		for (int i = 0; i <= lastPastContestPage; i++) {
			pastContestPage = FileUtilities.getWebPageContent("http://www.e-olimp.com/en/competitions-history/page:" + i);
			if (pastContestPage != null) {
				StringParser parser = new StringParser(pastContestPage);
				List<Description> descriptions = new ArrayList<Description>();
				try {
					parser.advance(true, "<h1>Competitions history</h1>");
					while (parser.advanceIfPossible(true, "href='competitions/") != null) {
						String id = parser.advance(true, "'>");
						String name = parser.advance(false, "</a>");
						descriptions.add(new Description(id, name));
					}
				} catch (ParseException ignored) {}
				if (receiver.isStopped())
					return;
				receiver.receiveDescriptions(descriptions);
			}
		}
	}

	public void parseContest(String id, DescriptionReceiver receiver) {
		String url;
		if (id.startsWith("page")) {
			if (!receiver.isStopped())
				receiver.receiveDescriptions(Collections.<Description>emptyList());
			url = "http://www.e-olimp.com/en/problems/" + id;
		} else
			url = "http://www.e-olimp.com/en/competitions/" + id;
		String mainPage = FileUtilities.getWebPageContent(url);
		if (mainPage == null)
			return;
		List<Description> tasks = new ArrayList<Description>();
		StringParser parser = new StringParser(mainPage);
		char letter = 'A';
		while (true) {
			try {
				if (id.startsWith("page"))
					parser.advance(true, "<a href='problems/");
				parser.advance(true, "<a href='problems/");
				String taskID = parser.advance(false, "'>");
				parser.advance(true, "'>");
				String name = StringEscapeUtils.unescapeHtml(parser.advance(false, "</a>"));
				if (id.startsWith("page"))
					tasks.add(new Description(taskID, taskID + " - " + name));
				else
					tasks.add(new Description(taskID + " " + letter, name));
				letter++;
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
		if (tokens.length > 2 || tokens.length < 1)
			return null;
		id = tokens[0];
		String name = tokens[tokens.length - 1];
		String text = FileUtilities.getWebPageContent("http://www.e-olimp.com/en/problems/" + id);
		if (text == null)
			return null;
		StringParser parser = new StringParser(text);
		try {
			parser.advance(true, "Memory Limit: ");
			String heapMemory = parser.advance(false, " ") + "M";
			List<Test> tests = new ArrayList<Test>();
			parser.advance(false, "<h3>Example input</h3>");
			parser.advance(true, "<pre>");
			if (parser.startsWith("Sample 1")) {
				List<String> inputs = new ArrayList<String>();
				List<String> outputs = new ArrayList<String>();
				for (int i = 1; ; i++) {
					try {
						parser.advance(true, "Sample " + i + "\r\n", "Sample " + i + "\n");
						inputs.add(parser.advance(false, "\nSample " + (i + 1), "</pre>"));
					} catch (ParseException e) {
						break;
					}
				}
				parser.advance(false, "<h3>Example output</h3>");
				parser.advance(true, "<pre>");
				for (int i = 1; ; i++) {
					try {
						parser.advance(true, "Sample " + i + "\r\n", "Sample " + i + "\n");
						outputs.add(parser.advance(false, "\nSample " + (i + 1), "</pre>"));
					} catch (ParseException e) {
						break;
					}
				}
				if (inputs.size() != outputs.size())
					return null;
				for (int i = 0; i < inputs.size(); i++)
					tests.add(new Test(StringEscapeUtils.unescapeHtml(inputs.get(i)),
					StringEscapeUtils.unescapeHtml(outputs.get(i)), tests.size()));
			} else {
				String input = parser.advance(false, "</pre>");
				parser.advance(false, "<h3>Example output</h3>");
				parser.advance(true, "<pre>");
				String output = parser.advance(false, "</pre>");
				tests.add(new Test(input, output, 0));
			}
			String taskClass = "Task" + name;
			return new Task(description.description, null, StreamConfiguration.STANDARD, StreamConfiguration.STANDARD,
				tests.toArray(new Test[tests.size()]), null, "-Xmx" + heapMemory, "Main", taskClass,
				TokenChecker.class.getCanonicalName(), "", new String[0], null, null, true, null, null, true);
		} catch (ParseException e) {
			return null;
		}
	}
}
