package net.egork.chelper.parser.codechef;

import net.egork.chelper.parser.ContestParser;
import net.egork.chelper.parser.TaskParser;
import net.egork.chelper.util.FileUtilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class CodeChefContestParser extends CodeChefParser implements ContestParser {
	public static final CodeChefContestParser INSTANCE = new CodeChefContestParser();

	private CodeChefContestParser() {
	}

	public Collection<String> parse(String id) {
		String mainPage;
		try {
			mainPage = FileUtilities.getWebPageContent("http://www.codechef.com/" + id);
		} catch (IOException e) {
			return Collections.emptyList();
		}
		List<String> tasks = new ArrayList<String>();
		while (true) {
			int position = mainPage.indexOf("<tr class=\"problemrow\">");
			if (position == -1)
				break;
			mainPage = mainPage.substring(position);
			position = mainPage.indexOf("/problems/");
			if (position == -1)
				break;
			mainPage = mainPage.substring(position + 10);
			position = mainPage.indexOf("\"");
			if (position == -1)
				break;
			tasks.add(id + " " + mainPage.substring(0, position));
		}
		return tasks;
	}

	public TaskParser getTaskParser() {
		return CodeChefTaskParser.INSTANCE;
	}
}
