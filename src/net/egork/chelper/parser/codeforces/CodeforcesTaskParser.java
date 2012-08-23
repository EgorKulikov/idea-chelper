package net.egork.chelper.parser.codeforces;

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

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CodeforcesTaskParser extends CodeforcesParser implements TaskParser {
	public static final CodeforcesTaskParser INSTANCE = new CodeforcesTaskParser();

	private CodeforcesTaskParser() {
	}

	public Task parse(String id, Task predefined) {
		String[] tokens = id.split(" ");
		if (tokens.length != 2)
			return null;
		String contestId = tokens[0];
		id = tokens[1];
		String text;
		try {
			text = FileUtilities.getWebPageContent("http://codeforces.ru/contest/" + contestId + "/problem/" + id);
		} catch (IOException e) {
			return null;
		}
		StringParser parser = new StringParser(text);
		try {
			parser.advance(false, "<div class=\"memory-limit\">");
			parser.advance(true, "</div>");
			String heapMemory = parser.advance(false, "</div>").split(" ")[0] + "M";
			parser.advance(false, "<div class=\"input-file\">");
			parser.advance(true, "</div>");
			String inputFileName = parser.advance(false, "</div>");
			StreamConfiguration inputType;
			if ("standard input".equals(inputFileName))
				inputType = StreamConfiguration.STANDARD;
			else
				inputType = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, inputFileName);
			parser.advance(false, "<div class=\"output-file\">");
			parser.advance(true, "</div>");
			String outputFileName = parser.advance(false, "</div>");
			StreamConfiguration outputType;
			if ("standard output".equals(outputFileName))
				outputType = StreamConfiguration.STANDARD;
			else
				outputType = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, outputFileName);
			List<Test> tests = new ArrayList<Test>();
			while (true) {
				try {
					parser.advance(false, "<div class=\"input\">");
					parser.advance(true, "<pre>");
					String testInput = parser.advance(false, "</pre>").replace("<br />", "\n");
					parser.advance(false, "<div class=\"output\">");
					parser.advance(true, "<pre>");
					String testOutput = parser.advance(false, "</pre>").replace("<br />", "\n");
					tests.add(new Test(StringEscapeUtils.unescapeHtml(testInput),
						StringEscapeUtils.unescapeHtml(testOutput), tests.size()));
				} catch (ParseException e) {
					break;
				}
			}
			String name = "Task" + id;
			return new Task(name, predefined.location, predefined.testType, inputType, outputType, heapMemory, "64M",
				true, tests.toArray(new Test[tests.size()]));
		} catch (ParseException e) {
			return null;
		}
	}
}
