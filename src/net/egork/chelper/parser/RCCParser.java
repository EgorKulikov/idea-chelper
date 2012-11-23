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
 * @author Egor Kulikov (egor@egork.net)
 */
public class RCCParser implements Parser {
	public Icon getIcon() {
		return IconLoader.getIcon("/icons/rcc.png");
	}

	public String getName() {
		return "Russian CodeCup";
	}

	public void getContests(DescriptionReceiver receiver) {
		String mainPage = FileUtilities.getWebPageContent("http://russiancodecup.ru/", "windows-1251");
		if (mainPage == null)
			return;
		StringParser parser = new StringParser(mainPage);
		try {
			parser.advance(true, "<ul class=\"lmenu\">");
			parser = new StringParser(parser.advance(false, "</div>"));
			List<Description> descriptions = new ArrayList<Description>();
			while (parser.advanceIfPossible(true, "<a href=\"/round/") != null) {
				String id = parser.advance(true, "/\">");
				String name = getName() + " " + StringEscapeUtils.unescapeHtml(parser.advance(false, "</a>"));
				descriptions.add(new Description(id, name));
			}
			Collections.reverse(descriptions);
			if (!receiver.isStopped())
				receiver.receiveDescriptions(descriptions);
		} catch (ParseException ignored) {
		}
	}

	public void parseContest(String id, DescriptionReceiver receiver) {
		String contestPage = FileUtilities.getWebPageContent("http://russiancodecup.ru/round/" + id + "/tasks", "windows-1251");
		if (contestPage == null)
			return;
		StringParser parser = new StringParser(contestPage);
		try {
			List<Description> descriptions = new ArrayList<Description>();
			while (parser.advanceIfPossible(true, "<a name=\"p") != null) {
				String taskID = parser.advance(false, "\"></a>");
				parser.advance(true, "<th colspan=\"2\">\"");
				String letter = parser.advance(true, "\" ");
				String name = StringEscapeUtils.unescapeHtml(parser.advance(false, "</th></tr>"));
				descriptions.add(new Description(id + " " + taskID, letter + " - " + name));
			}
			if (!receiver.isStopped())
				receiver.receiveDescriptions(descriptions);
		} catch (ParseException ignored) {
		}
	}

	public Task parseTask(Description description) {
		String contestID = description.id.split(" ")[0];
		String taskID = description.id.split(" ")[1];
		String page = FileUtilities.getWebPageContent("http://russiancodecup.ru/round/" + contestID + "/tasks", "windows-1251");
		if (page == null)
			return null;
		StringParser parser = new StringParser(page);
		try {
			parser.advance(true, "<a name=\"p" + taskID + "\"></a>\n");
			parser = new StringParser(parser.advance(false, "</table>"));
			parser.advance(true, "<tr class=\"th\"><th colspan=\"2\">\"");
			String letter = parser.advance(false, "\"");
			parser.advance(true, "<td>Ограничение по памяти</td>");
			parser.advance(true, "<td>");
			String memoryLimit = parser.advance(false, " ");
			List<Test> tests = new ArrayList<Test>();
			while (parser.advanceIfPossible(true, "<pre class=\"m0\">") != null) {
				String input = StringEscapeUtils.unescapeHtml(parser.advanceIfPossible(false, "</pre>"));
				input = input.substring(1);
				parser.advance(true, "<pre class=\"m0\">");
				String output = StringEscapeUtils.unescapeHtml(parser.advanceIfPossible(false, "</pre>"));
				output = output.substring(1);
				tests.add(new Test(input, output, tests.size()));
			}
			return new Task(description.description, null, StreamConfiguration.STANDARD, StreamConfiguration.STANDARD,
				tests.toArray(new Test[tests.size()]), null, "-Xmx" + memoryLimit + "M -Xss64M", false, "Main", "Task" + letter,
				TokenChecker.class.getCanonicalName(), "", new String[0], null, null, true, null, null);
		} catch (ParseException e) {
			return null;
		}
	}
}
