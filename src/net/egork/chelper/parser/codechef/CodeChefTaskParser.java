package net.egork.chelper.parser.codechef;

import net.egork.chelper.parser.StringParser;
import net.egork.chelper.parser.TaskParser;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.util.FileUtilities;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class CodeChefTaskParser extends CodeChefParser implements TaskParser {
	public static final CodeChefTaskParser INSTANCE = new CodeChefTaskParser();

	private CodeChefTaskParser() {
	}

	public Task parse(String id, Task predefined) {
		String[] tokens = id.split(" ");
		if (tokens.length > 2 || tokens.length == 0)
			return null;
		String url;
		if (tokens.length == 2)
			url = "http://www.codechef.com/" + tokens[0] + "/problems/" + tokens[1];
		else
			url = "http://www.codechef.com/problems/" + tokens[0];
		String text;
		try {
			text = FileUtilities.getWebPageContent(url);
		} catch (IOException e) {
			return null;
		}
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
			if (tests.isEmpty())
				return null;
			return new Task(taskID, predefined.location, predefined.testType, StreamConfiguration.STANDARD,
				StreamConfiguration.STANDARD, "256M", "64M", true,
				tests.toArray(new Test[tests.size()]));
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
