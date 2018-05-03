package net.egork.chelper.parser;

import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;
import net.egork.chelper.tester.StringInputStream;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.InputReader;
import org.apache.commons.lang.StringEscapeUtils;

import javax.swing.*;
import java.text.ParseException;
import java.util.*;

/**
 * @author egorku@yandex-team.ru
 */
public class GCJParser implements Parser {
    public Icon getIcon() {
        return IconLoader.getIcon("/icons/gcj.png");
    }

    public String getName() {
        return "Google Code Jam";
    }

    public void getContests(DescriptionReceiver receiver) {
        String info = FileUtilities.getWebPageContent("https://code.google.com/codejam/contest/microsite-info");
        if (info != null) {
            Map<String, Value> parsedInfo = jsonParse(new InputReader(new StringInputStream(info)));
            if ("true".equals(parsedInfo.get("contestExists").nonJSON) && parsedInfo.containsKey("secsToEnd") &&
                    parsedInfo.get("secsToEnd").type == Value.Type.STRING &&
                    !parsedInfo.get("secsToEnd").nonJSON.startsWith("-")) {
                String name = parsedInfo.containsKey("contestName") ? parsedInfo.get("contestName").nonJSON : null;
                String id = parsedInfo.containsKey("contestId") ? parsedInfo.get("contestId").nonJSON : null;
                if (receiver.isStopped()) {
                    return;
                }
                if (name != null && id != null) {
                    receiver.receiveDescriptions(Collections.singleton(new Description(id, getName() + " " + name)));
                }
            }
        }
        if (receiver.isStopped()) {
            return;
        }
        String historyPage = FileUtilities.getWebPageContent("https://code.google.com/codejam/past-contests/past-contests-page.html");
        if (historyPage != null) {
            System.err.println(historyPage);
            if (true) {
                return;
            }
            StringParser parser = new StringParser(historyPage);
            try {
                while (parser.advanceIfPossible(true, "<div class=\"year_row narrow_year_row\">") != null) {
                    parser.advance(true, "<h3>");
                    String tournamentName = parser.advance(false, "</h3>");
                    StringParser tournament = new StringParser(parser.advance(true, "</table>"));
                    List<Description> rounds = new ArrayList<Description>();
                    while (tournament.advanceIfPossible(true, "<tr>") != null) {
                        tournament.advance(true, "<a href=\"/codejam/contest/");
                        String id = tournament.advance(false, "/dashboard");
                        tournament.advance(true, ">");
                        String roundName = tournament.advance(false, "</a>");
                        rounds.add(new Description(id, tournamentName + " " + roundName));
                    }
                    if (receiver.isStopped()) {
                        return;
                    }
                    receiver.receiveDescriptions(rounds);
                }
            } catch (ParseException ignored) {
            }
        }
    }

    static class Value {
        enum Type {
            STRING,
            JSON
        }

        final Type type;
        final String nonJSON;
        final List<Map<String, Value>> json;

        Value(String nonJSON) {
            type = Type.STRING;
            this.nonJSON = nonJSON;
            json = null;
        }

        Value(List<Map<String, Value>> json) {
            type = Type.JSON;
            this.json = json;
            nonJSON = null;
        }
    }

    private Map<String, Value> jsonParse(InputReader json) {
        Map<String, Value> result = new HashMap<String, Value>();
        if (json.readCharacter() != '{') {
            throw new InputMismatchException();
        }
        while (true) {
            String key = readKey(json);
            if (json.readCharacter() != ':') {
                throw new InputMismatchException();
            }
            Value value = readValue(json);
            char delimiter = json.readCharacter();
            result.put(key, value);
            if (delimiter == '}') {
                return result;
            }
            if (delimiter != ',') {
                throw new InputMismatchException();
            }
        }
    }

    private Value readValue(InputReader json) {
        char first = json.readCharacter();
        if (first == '"') {
            StringBuilder builder = new StringBuilder();
            int last = -1;
            while (true) {
                int c = json.read();
                if (c == '"' && last != '\\') {
                    if (last != -1) {
                        builder.appendCodePoint(last);
                    }
                    break;
                } else if (c == '"') {
                    last = '"';
                } else {
                    if (last != -1) {
                        builder.appendCodePoint(last);
                    }
                    last = c;
                }
            }
            return new Value(builder.toString());
        }
        if (first != '[') {
            StringBuilder builder = new StringBuilder();
            builder.append(first);
            while (json.peek() != ',' && json.peek() != '}')
                builder.appendCodePoint(json.read());
            return new Value(builder.toString());
        }
        List<Map<String, Value>> list = new ArrayList<Map<String, Value>>();
        while (true) {
            list.add(jsonParse(json));
            char delimiter = json.readCharacter();
            if (delimiter == ']') {
                return new Value(list);
            }
            if (delimiter != ',') {
                throw new InputMismatchException();
            }
        }
    }

    private String readKey(InputReader json) {
        if (json.readCharacter() != '"') {
            throw new InputMismatchException();
        }
        StringBuilder key = new StringBuilder();
        while (true) {
            int c = json.read();
            if (c == '"') {
                return key.toString();
            }
            key.appendCodePoint(c);
        }
    }

    public void parseContest(String id, DescriptionReceiver receiver) {
        String info = FileUtilities.getWebPageContent("https://code.google.com/codejam/contest/" + id + "/dashboard/ContestInfo");
        if (info == null) {
            return;
        }
        Value problems = jsonParse(new InputReader(new StringInputStream(info))).get("problems");
        if (problems == null || problems.type != Value.Type.JSON) {
            return;
        }
        if (receiver.isStopped()) {
            return;
        }
        List<Description> descriptions = new ArrayList<Description>();
        char letter = 'A';
        for (Map<String, Value> map : problems.json) {
            String name = map.containsKey("name") ? map.get("name").nonJSON : null;
            String taskID = map.containsKey("id") ? map.get("id").nonJSON : null;
            if (name != null && taskID != null) {
                descriptions.add(new Description(id + " " + taskID, letter + " - " + name));
            }
            letter++;
        }
        if (receiver.isStopped()) {
            return;
        }
        receiver.receiveDescriptions(descriptions);
    }

    public Task parseTask(Description description) {
        String[] tokens = description.id.split(" ");
        String contestID = tokens[0];
        String taskID = tokens[1];
        String info = FileUtilities.getWebPageContent("https://code.google.com/codejam/contest/" + contestID + "/dashboard/ContestInfo");
        Value problems = jsonParse(new InputReader(new StringInputStream(info))).get("problems");
        if (problems == null || problems.type != Value.Type.JSON) {
            return null;
        }
        for (Map<String, Value> map : problems.json) {
            String id = map.containsKey("id") ? map.get("id").nonJSON : null;
            if (taskID.equals(id)) {
                String body = map.containsKey("body") ? map.get("body").nonJSON : null;
                if (body == null) {
                    return null;
                }
                return parseTask(new StringParser(body), description.description);
            }
        }
        return null;
    }

    public TestType defaultTestType() {
        return TestType.MULTI_NUMBER;
    }

    public Task parseTask(StringParser parser, String description) {
        try {
            parser.advance(true, "<div class=\"problem-io-wrapper\">");
            parser.advance(true, "<pre class=\"io-content\">", "<code>");
            String input = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre>", "</code>").replaceAll("(\\\\r)?\\\\n", "\n").trim().replaceAll("<br/?>", "")) + "\n";
            parser.advance(true, "<pre class=\"io-content\">", "<code>");
            String output = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre>", "</code>").replaceAll("(\\\\r)?\\\\n", "\n").trim().replaceAll("<br/?>", ""));
            String letter = description.split(" ")[0];
            return new Task(description, null,
                    new StreamConfiguration(StreamConfiguration.StreamType.LOCAL_REGEXP,
                            letter + "-(small|large).*[.]in"),
                    new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, letter.toLowerCase() + ".out"),
                    new Test[]{new Test(input, output, 0)}, null, "-Xmx512M", "Main",
                    "Task" + letter,
                    TokenChecker.class.getCanonicalName(), "", new String[0], null, null, true, null, null,
                    true, false);
        } catch (ParseException e) {
            return null;
        }
    }

    public Collection<Task> parseTaskFromHTML(String html) {
        StringParser parser = new StringParser(html);
        List<Task> result = new ArrayList<Task>();
        try {
            parser.advance(true, "<div id=\"dsb-contest-title\">");
            String contest = parser.advance(false, "</div>").trim();
            List<String> titles = new ArrayList<String>();
            for (int i = 0; ; i++) {
                if (parser.advanceIfPossible(true, "<div id=\"dsb-problem-title" + i + "\" class=\"dynamic-link\">") != null) {
                    String description = parser.advance(false, "</div>").trim();
                    description = description.substring(0, 1) + " - " + description.substring(3);
                    titles.add(description);
                } else {
                    break;
                }
            }
            for (String description : titles) {
                Task task = parseTask(parser, description);
                if (task == null) {
                    break;
                }
                task = task.setTestType(defaultTestType()).setContestName(getName() + " " + contest);
                result.add(task);
            }
        } catch (ParseException ignored) {
        }
        return result;
    }
}
