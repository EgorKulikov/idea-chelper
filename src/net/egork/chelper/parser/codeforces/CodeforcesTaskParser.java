package net.egork.chelper.parser.codeforces;

import net.egork.chelper.parser.TaskParser;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.util.FileUtilities;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
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
		int position = text.indexOf("<div class=\"memory-limit\">");
		if (position == -1)
			return null;
		text = text.substring(position);
		position = text.indexOf("</div>");
		if (position == -1)
			return null;
		text = text.substring(position + 6);
		position = text.indexOf("</div>");
		if (position == -1)
			return null;
		String heapMemory = text.substring(0, position).split(" ")[0] + "M";
		text = text.substring(position);
		position = text.indexOf("<div class=\"input-file\">");
		if (position == -1)
			return null;
		text = text.substring(position);
		position = text.indexOf("</div>");
		if (position == -1)
			return null;
		text = text.substring(position + 6);
		position = text.indexOf("</div>");
		if (position == -1)
			return null;
		String inputFileName = text.substring(0, position);
		text = text.substring(position);
		StreamConfiguration inputType;
		if ("standard input".equals(inputFileName))
			inputType = StreamConfiguration.STANDARD;
		else
			inputType = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, inputFileName);
		position = text.indexOf("<div class=\"output-file\">");
		if (position == -1)
			return null;
		text = text.substring(position);
		position = text.indexOf("</div>");
		if (position == -1)
			return null;
		text = text.substring(position + 6);
		position = text.indexOf("</div>");
		if (position == -1)
			return null;
		String outputFileName = text.substring(0, position);
		text = text.substring(position);
		StreamConfiguration outputType;
		if ("standard output".equals(outputFileName))
			outputType = StreamConfiguration.STANDARD;
		else
			outputType = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, outputFileName);
		List<Test> tests = new ArrayList<Test>();
		while (true) {
			position = text.indexOf("<div class=\"input\">");
			if (position == -1)
				break;
			text = text.substring(position);
			position = text.indexOf("<pre class=\"content\">");
			if (position == -1)
				break;
			text = text.substring(position + 21);
			position = text.indexOf("</pre>");
			if (position == -1)
				break;
			String testInput = text.substring(0, position).replace("<br />", "\n");
			text = text.substring(position);
			position = text.indexOf("<div class=\"output\">");
			if (position == -1)
				break;
			text = text.substring(position);
			position = text.indexOf("<pre class=\"content\">");
			if (position == -1)
				break;
			text = text.substring(position + 21);
			position = text.indexOf("</pre>");
			if (position == -1)
				break;
			String testOutput = text.substring(0, position).replace("<br />", "\n");
			text = text.substring(position);
			tests.add(new Test(StringEscapeUtils.unescapeHtml(testInput),
				StringEscapeUtils.unescapeHtml(testOutput), tests.size()));
		}
		String name = "Task" + id;
		return new Task(name, predefined.location, predefined.testType, inputType, outputType, heapMemory, "64M",
			predefined.project, tests.toArray(new Test[tests.size()]));
	}
}
