package net.egork.chelper.parser;

import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;
import net.egork.chelper.util.TaskUtilities;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;

public class JSONParser implements Parser {
    @Override
    public Icon getIcon() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return "JSON";
    }

    @Override
    public void getContests(DescriptionReceiver receiver) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void parseContest(String id, DescriptionReceiver receiver) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Task parseTask(Description description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TestType defaultTestType() {
        return TestType.SINGLE;
    }

    @Override
    public Collection<Task> parseTaskFromHTML(String html) {
        try {
            JSONObject obj = new JSONObject(html);

            String taskName = obj.getString("name");
            String contestName = obj.getString("group");

            JSONObject languages = obj.getJSONObject("languages");
            JSONObject java = languages.getJSONObject("java");

            String mainClass = java.getString("mainClass");
            String taskClass = TaskUtilities.replaceCyrillics(java.getString("taskClass"));

            int memoryLimit = obj.getInt("memoryLimit");

            TestType type = stringToTestType(obj.getString("testType"));
            StreamConfiguration inputConfig = parseStreamConfiguration(obj.getJSONObject("input"));
            StreamConfiguration outputConfig = parseStreamConfiguration(obj.getJSONObject("output"));

            JSONArray testsArr = obj.getJSONArray("tests");
            Test[] tests = new Test[testsArr.length()];

            for (int i = 0, iMax = testsArr.length(); i < iMax; i++) {
                JSONObject testObj = testsArr.getJSONObject(i);

                String testInput = testObj.getString("input");
                String testOutput = testObj.getString("output");

                tests[i] = new Test(testInput, testOutput);
            }

            Task task = new Task(taskName, type, inputConfig, outputConfig, tests,
                    null, "-Xmx" + memoryLimit + "M", mainClass, taskClass,
                    TokenChecker.class.getCanonicalName(), "",
                    new String[0], null, contestName, true,
                    null, null, false, false);

            return Collections.singleton(task);
        } catch (JSONException e) {
            return Collections.emptyList();
        }
    }

    private StreamConfiguration parseStreamConfiguration(JSONObject obj) {
        String type = obj.getString("type");

        switch (type) {
            case "stdin":
            case "stdout":
                return StreamConfiguration.STANDARD;
            case "file":
                String fileName = obj.getString("fileName");
                return new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, fileName);
            case "regex":
                String regex = obj.getString("pattern");
                return new StreamConfiguration(StreamConfiguration.StreamType.LOCAL_REGEXP, regex);
        }

        return null;
    }

    private TestType stringToTestType(String str) {
        switch (str) {
            case "single":
                return TestType.SINGLE;
            case "multiNumber":
                return TestType.MULTI_NUMBER;
            case "multiEOF":
                return TestType.MULTI_EOF;
        }

        return TestType.SINGLE;
    }
}
