package net.egork.chelper.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.egork.chelper.util.InputReader;
import net.egork.chelper.util.OutputWriter;

import java.util.Calendar;
import java.util.InputMismatchException;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Task {
    //Basic
    public final String name;
    public final TestType testType;
    public final StreamConfiguration input;
    public final StreamConfiguration output;
    public final Test[] tests;

    //Creation only
    @JsonIgnore
    public final String template;

    //Advanced
    public final String location;
    public final String vmArgs;
    public final String mainClass;
    public final String taskClass;
    public final String checkerClass;
    public final String checkerParameters;
    public final String[] testClasses;
    public final String date;
    public final String contestName;
    public final boolean truncate;
    public final String inputClass;
    public final String outputClass;
    public final boolean includeLocale;
    public final boolean failOnOverflow;
    public final boolean interactive;
    public final String interactor;

    @JsonCreator
    public Task(@JsonProperty("name") String name,
                @JsonProperty("testType") TestType testType,
                @JsonProperty("input") StreamConfiguration input,
                @JsonProperty("output") StreamConfiguration output,
                @JsonProperty("tests") Test[] tests,
                @JsonProperty("location") String location,
                @JsonProperty("vmArgs") String vmArgs,
                @JsonProperty("mainClass") String mainClass,
                @JsonProperty("taskClass") String taskClass,
                @JsonProperty("checkerClass") String checkerClass,
                @JsonProperty("checkerParameters") String checkerParameters,
                @JsonProperty("testClasses") String[] testClasses,
                @JsonProperty("date") String date,
                @JsonProperty("contestName") String contestName,
                @JsonProperty("truncate") boolean truncate,
                @JsonProperty("inputClass") String inputClass,
                @JsonProperty("outputClass") String outputClass,
                @JsonProperty("includeLocale") boolean includeLocale,
                @JsonProperty("failOnOverflow") boolean failOnOverflow,
                @JsonProperty("interactive") boolean interactive,
                @JsonProperty("interactor") String interactor) {
        this(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, null, interactive, interactor);
    }

    public Task(String name, TestType testType, StreamConfiguration input, StreamConfiguration output, Test[] tests,
                String location, String vmArgs, String mainClass, String taskClass, String checkerClass,
                String checkerParameters, String[] testClasses, String date, String contestName, boolean truncate,
                String inputClass, String outputClass, boolean includeLocale, boolean failOnOverflow) {
        this(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, null);
    }

    public Task(String name, TestType testType, StreamConfiguration input, StreamConfiguration output, Test[] tests,
                String location, String vmArgs, String mainClass, String taskClass, String checkerClass,
                String checkerParameters, String[] testClasses, String date, String contestName, boolean truncate,
                String inputClass, String outputClass, boolean includeLocale, boolean failOnOverflow, String template) {
        this(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, template, false, null);
    }

    public Task(String name, TestType testType, StreamConfiguration input, StreamConfiguration output, Test[] tests,
                String location, String vmArgs, String mainClass, String taskClass, String checkerClass,
                String checkerParameters, String[] testClasses, String date, String contestName, boolean truncate,
                String inputClass, String outputClass, boolean includeLocale, boolean failOnOverflow, String template,
                boolean interactive, String interactor) {
        this.name = trim(name);
        this.testType = testType;
        this.input = input;
        this.output = output;
        this.tests = tests;
        this.location = trim(location);
        this.vmArgs = trim(vmArgs);
        this.mainClass = trim(mainClass);
        this.taskClass = trim(taskClass);
        this.checkerClass = trim(checkerClass);
        this.checkerParameters = trim(checkerParameters);
        this.testClasses = testClasses;
        this.date = trim(date);
        this.contestName = trim(contestName);
        this.truncate = truncate;
        this.inputClass = trim(inputClass);
        this.outputClass = trim(outputClass);
        this.includeLocale = includeLocale;
        this.failOnOverflow = failOnOverflow;
        this.template = template;
        if (tests != null) {
            for (int i = 0; i < tests.length; i++) {
                if (tests[i].index != i) {
                    tests[i] = new Test(tests[i].input, tests[i].output, i, tests[i].active);
                }
            }
        }
        this.interactive = interactive;
        this.interactor = interactive && interactor == null ? "net.egork.chelper.tester.Interactor" : interactor;
    }

    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        return s.trim();
    }

    public static String getDateString() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        StringBuilder result = new StringBuilder();
        result.append(year).append('.');
        if (month < 10) {
            result.append('0');
        }
        result.append(month).append('.');
        if (day < 10) {
            result.append('0');
        }
        result.append(day);
        return result.toString();
    }

    public void saveTask(OutputWriter out) {
        out.printString(name);
        out.printEnum(testType);
        out.printEnum(input.type);
        out.printString(input.fileName);
        out.printEnum(output.type);
        out.printString(output.fileName);
        out.printLine(tests.length);
        for (Test test : tests)
            test.saveTest(out);

        out.printString(location);
        out.printString(vmArgs);
        out.printString(mainClass);
        out.printString(taskClass);
        out.printString(checkerClass);
        out.printString(checkerParameters);
        out.printLine(testClasses.length);
        for (String testClass : testClasses)
            out.printString(testClass);
        out.printString(date);
        out.printString(contestName);
        out.printBoolean(truncate);
        out.printString(inputClass);
        out.printString(outputClass);
        out.printBoolean(includeLocale);
        out.printBoolean(failOnOverflow);
    }

    public static Task loadTask(InputReader in) {
        String name = in.readString();
        TestType testType = in.readEnum(TestType.class);
        StreamConfiguration.StreamType inputStreamType = in.readEnum(StreamConfiguration.StreamType.class);
        String inputFileName = in.readString();
        StreamConfiguration.StreamType outputStreamType = in.readEnum(StreamConfiguration.StreamType.class);
        String outputFileName = in.readString();
        int testCount = in.readInt();
        Test[] tests = new Test[testCount];
        for (int i = 0; i < testCount; i++)
            tests[i] = Test.loadTest(in);

        String location = in.readString();
        String vmArgs = in.readString();
        String mainClass = in.readString();
        String taskClass = in.readString();
        String checkerClass = in.readString();
        String checkerParameters = in.readString();
        int testClassesCount = in.readInt();
        String[] testClasses = new String[testClassesCount];
        for (int i = 0; i < testClassesCount; i++)
            testClasses[i] = in.readString();
        String date = in.readString();
        String contestName = in.readString();
        boolean truncate = in.readBoolean();
        String inputClass = in.readString();
        String outputClass = in.readString();
        boolean includeLocale = false;
        boolean failOnOverflow = false;
        try {
            includeLocale = in.readBoolean();
            failOnOverflow = in.readBoolean();
        } catch (InputMismatchException ignored) {
        }
        return new Task(name, testType, new StreamConfiguration(inputStreamType, inputFileName),
                new StreamConfiguration(outputStreamType, outputFileName), tests, location, vmArgs, mainClass,
                taskClass, checkerClass, checkerParameters, testClasses, date, contestName, truncate, inputClass,
                outputClass, includeLocale, failOnOverflow);
    }

    public Task setTests(Test[] tests) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, template, interactive, interactor);
    }

    public Task setTestClasses(String[] testClasses) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, template, interactive, interactor);
    }

    public Task setTaskClass(String taskClass) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, template, interactive, interactor);
    }

    public Task setCheckerClass(String checkerClass) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, template, interactive, interactor);
    }

    public Task setInteractor(String interactor) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, template, interactive, interactor);
    }

    public Task setLocation(String location) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, template, interactive, interactor);
    }

    public Task setTestType(TestType testType) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, template, interactive, interactor);
    }

    public Task setContestName(String contestName) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, template, interactive, interactor);
    }

    public Task setName(String name) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, template, interactive, interactor);
    }

    public Task setFailOnIntegerOverflow(boolean failOnOverflow) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, template, interactive, interactor);
    }

    public Task setDate(String date) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, template, interactive, interactor);
    }

    public Task setInputOutputClasses(String inputClass, String outputClass) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, template, interactive, interactor);
    }

    public Task setTemplate(String template) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale,
                failOnOverflow, template, interactive, interactor);
    }
}
