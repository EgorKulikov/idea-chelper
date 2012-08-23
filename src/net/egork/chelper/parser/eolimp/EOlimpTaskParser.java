package net.egork.chelper.parser.eolimp;

import net.egork.chelper.parser.StringParser;
import net.egork.chelper.parser.TaskParser;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.util.FileUtilities;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class EOlimpTaskParser extends EOlimpParser implements TaskParser {
	public static EOlimpTaskParser INSTANCE = new EOlimpTaskParser();


	public Task parse(String id, Task predefined) {
		String[] tokens = id.split(" ");
		if (tokens.length > 2 || tokens.length < 1)
			return null;
		id = tokens[0];
		String name = tokens[tokens.length - 1];
		String text;
		try {
			text = FileUtilities.getWebPageContent("http://www.e-olimp.com/en/problems/" + id);
		} catch (IOException e) {
			return null;
		}
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
						parser.advance(true, "Sample " + i + "\r\n");
						inputs.add(parser.advance(false, "\nSample " + (i + 1), "</pre>"));
					} catch (ParseException e) {
						break;
					}
				}
				parser.advance(false, "<h3>Example output</h3>");
				parser.advance(true, "<pre>");
				for (int i = 1; ; i++) {
					try {
						parser.advance(true, "Sample " + i + "\r\n");
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
			name = "Task" + name;
			return new Task(name, predefined.location, predefined.testType, predefined.input, predefined.output,
				heapMemory, "16M", true, tests.toArray(new Test[tests.size()]));
		} catch (ParseException e) {
			return null;
		}
	}
}
