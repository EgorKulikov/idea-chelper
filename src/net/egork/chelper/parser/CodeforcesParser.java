package net.egork.chelper.parser;

import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.util.FileUtilities;
import org.apache.commons.lang.StringEscapeUtils;

import javax.swing.*;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CodeforcesParser implements Parser {
	public Icon getIcon() {
		return IconLoader.getIcon("/icons/codeforces.png");
	}

	public String getName() {
		return "Codeforces";
	}

    public void getContests(DescriptionReceiver receiver) {
        String contestsPage;
        try{
            contestsPage = FileUtilities.getWebPageContent("http://codeforces.com/contests");
        } catch (IOException e) {
            return;
        }
        List<Description> contests = new ArrayList<Description>();
        StringParser parser = new StringParser(contestsPage);
        try {
            parser.advance(true, "<div class=\"contestList\">");
            parser.advance(true, "</tr>");
            while (parser.advanceIfPossible(true, "data-contestId=\"") != null) {
                String id = parser.advance(false, "\"");
                parser.advance(true, "<td>");
                String name = parser.advance(false, "</td>", "<br/>").trim();
                contests.add(new Description(id, name));
            }
        } catch (ParseException ignored) {}
		if (!receiver.isStopped())
			receiver.receiveDescriptions(contests);
		else
			return;
		//noinspection StatementWithEmptyBody
		while (parser.advanceIfPossible(true, "<span class=\"page-index\" pageIndex=\"") != null);
		String lastPage = parser.advanceIfPossible(false, "\"");
		if (lastPage == null)
			return;
		int additionalPagesCount;
		try {
			additionalPagesCount = Integer.parseInt(lastPage);
		} catch (NumberFormatException e) {
			return;
		}
		for (int i = 2; i <= additionalPagesCount; i++) {
			String page;
			try {
				page = FileUtilities.getWebPageContent("http://codeforces.com/contests/page/" + i);
			} catch (IOException e) {
				continue;
			}
			parser = new StringParser(page);
			List<Description> descriptions = new ArrayList<Description>();
			try {
				parser.advance(true, "Contest history");
				parser.advance(true, "</tr>");
				while (parser.advanceIfPossible(true, "data-contestId=\"") != null) {
					String id = parser.advance(false, "\"");
					parser.advance(true, "<td>");
					String name = parser.advance(false, "</td>", "<br/>").trim();
					descriptions.add(new Description(id, name));
				}
			} catch (ParseException e) {
				continue;
			}
			if (receiver.isStopped()) {
				return;
			}
			receiver.receiveDescriptions(descriptions);
		}
    }

    public void parseContest(String id, DescriptionReceiver receiver) {
        String mainPage;
        try {
            mainPage = FileUtilities.getWebPageContent("http://codeforces.com/contest/" + id);
        } catch (IOException e) {
            return;
        }
        List<Description> ids = new ArrayList<Description>();
        StringParser parser = new StringParser(mainPage);
        try {
            parser.advance(true, "<table class=\"problems\">");
            while (parser.advanceIfPossible(true, "<a href=\"/contest/" + id + "/problem/") != null) {
                String taskID = parser.advance(false, "\">");
                parser.advance(true, "<a href=\"/contest/" + id + "/problem/" + taskID + "\">");
                String name = taskID + " - " + parser.advance(false, "</a>");
                ids.add(new Description(id + " " + taskID, name));
            }
        } catch (ParseException ignored) {
        }
        if (!receiver.isStopped())
			receiver.receiveDescriptions(ids);
    }

    public Task parseTask(String id) {
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
            return new Task(name, null, inputType, outputType, tests.toArray(new Test[tests.size()]), null,
                    "-Xmx" + heapMemory, null, name, TokenChecker.class.getCanonicalName(), "", new String[0], null,
                    null, true, null, null);
        } catch (ParseException e) {
            return null;
        }
    }
}
