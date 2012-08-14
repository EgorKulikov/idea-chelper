package net.egork.chelper.parser.codechef;

import net.egork.chelper.parser.ContestParser;
import net.egork.chelper.parser.StringParser;
import net.egork.chelper.parser.TaskParser;
import net.egork.chelper.util.FileUtilities;

import java.io.IOException;
import java.text.ParseException;
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
		StringParser parser = new StringParser(mainPage);
		while (true) {
			try {
				parser.advance(true, id + "/problems/");
				tasks.add(id + " " + parser.advance(false, "\""));
			} catch (ParseException e) {
				break;
			}
		}
		return tasks;
	}

	public TaskParser getTaskParser() {
		return CodeChefTaskParser.INSTANCE;
	}
}
