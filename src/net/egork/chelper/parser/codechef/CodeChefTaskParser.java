package net.egork.chelper.parser.codechef;

import net.egork.chelper.parser.TaskParser;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.util.FileUtilities;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;

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
		int position = text.indexOf("<div class=\"prob\">");
		if (position == -1)
			return null;
		text = text.substring(position);
		position = text.indexOf("<h1>");
		if (position == -1)
			return null;
		text = text.substring(position + 4);
		position = text.indexOf("</h1>");
		if (position == -1)
			return null;
		String taskID = getTaskID(StringEscapeUtils.unescapeHtml(text.substring(0, position)));
		text = text.substring(position);
		position = text.indexOf("<div id=\"comments\">");
		if (position != -1)
			text = text.substring(0, position);
		Test test;
		position = text.lastIndexOf("Example");
		if (position != -1)
			text = text.substring(position);
		position = text.lastIndexOf("Sample input");
		if (position != -1)
			text = text.substring(position);
		position = text.lastIndexOf("Sample Input");
		if (position != -1)
			text = text.substring(position);
		position = text.indexOf("nput");
		if (position == -1)
			return null;
		text = text.substring(position + 4);
		if (text.startsWith(":"))
			text = text.substring(1);
		int bracketLevel = 0;
		while (true) {
			char head = text.charAt(0);
			if (head == '<')
				bracketLevel++;
			if (bracketLevel == 0 && head != ' ' && head != '\n')
				break;
			if (head == '>')
				bracketLevel--;
			text = text.substring(1);
		}
		position = text.indexOf("utput");
		if (position == -1)
			return null;
		String input = text.substring(0, position - 1);
		if (input.endsWith("Sample "))
			input = input.substring(0, input.length() - 7);
		text = text.substring(position + 5);
		if (text.startsWith(":"))
			text = text.substring(1);
		while (true) {
			char head = input.charAt(input.length() - 1);
			if (head == '>')
				bracketLevel++;
			if (bracketLevel == 0 && head != ' ' && head != '\n')
				break;
			if (head == '<')
				bracketLevel--;
			input = input.substring(0, input.length() - 1);
		}
		input = input.replace("<br />\n", "\n").replace("<br />", "\n");
		input = StringEscapeUtils.unescapeHtml(input);
		if (!input.endsWith("\n"))
			input += "\n";
		while (true) {
			char head = text.charAt(0);
			if (head == '<')
				bracketLevel++;
			if (bracketLevel == 0 && head != ' ' && head != '\n')
				break;
			if (head == '>')
				bracketLevel--;
			text = text.substring(1);
		}
		position = text.indexOf("<b>");
		int altPosition = text.indexOf("</pre>");
		if (position == -1 || altPosition != -1 && altPosition < position)
			position = altPosition;
		altPosition = text.indexOf("</div>");
		if (position == -1 || altPosition != -1 && altPosition < position)
			position = altPosition;
		if (position == -1)
			return null;
		String output = text.substring(0, position);
		output = output.replace("<br />\n", "\n").replace("<br />", "\n");
		output = StringEscapeUtils.unescapeHtml(output);
		test = new Test(input, output, 0);
		return new Task(taskID, predefined.location, predefined.testType, StreamConfiguration.STANDARD,
			StreamConfiguration.STANDARD, "256M", "64M", predefined.project, new Test[]{test});
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
