package net.egork.chelper.parser.codeforces;

import net.egork.chelper.parser.ContestParser;
import net.egork.chelper.parser.TaskParser;
import net.egork.chelper.util.FileUtilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CodeforcesContestParser extends CodeforcesParser implements ContestParser {
	public static final CodeforcesContestParser INSTANCE = new CodeforcesContestParser();

	private CodeforcesContestParser() {
	}

	public Collection<String> parse(String id) {
		String mainPage;
		try {
			mainPage = FileUtilities.getWebPageContent("http://codeforces.ru/contest/" + id);
		} catch (IOException e) {
			return Collections.emptyList();
		}
		List<String> ids = new ArrayList<String>();
		for (char c = 'A'; c <= 'Z'; c++) {
			if (mainPage.indexOf("<a href=\"/contest/" + id + "/problem/" + c + "\">") != -1)
				ids.add(id + " " + Character.toString(c));
		}
		return ids;
	}

	public TaskParser getTaskParser() {
		return CodeforcesTaskParser.INSTANCE;
	}
}
