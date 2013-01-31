package net.egork.chelper.task;

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

    public Task(String name, TestType testType, StreamConfiguration input, StreamConfiguration output, Test[] tests, String location, String vmArgs, String mainClass, String taskClass, String checkerClass, String checkerParameters, String[] testClasses, String date, String contestName, boolean truncate, String inputClass, String outputClass, boolean includeLocale, boolean failOnOverflow) {
        this.name = name.trim();
        this.testType = testType;
        this.input = input;
        this.output = output;
        this.tests = tests;
        this.location = location.trim();
        this.vmArgs = vmArgs.trim();
        this.mainClass = mainClass.trim();
        this.taskClass = taskClass.trim();
        this.checkerClass = checkerClass.trim();
        this.checkerParameters = checkerParameters.trim();
        this.testClasses = testClasses;
        this.date = date.trim();
        this.contestName = contestName.trim();
        this.truncate = truncate;
        this.inputClass = inputClass.trim();
        this.outputClass = outputClass.trim();
		this.includeLocale = includeLocale;
        this.failOnOverflow = failOnOverflow;
    }

    public static String getDateString() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        StringBuilder result = new StringBuilder();
        result.append(year).append('.');
        if (month < 10)
            result.append('0');
        result.append(month).append('.');
        if (day < 10)
            result.append('0');
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
		} catch (InputMismatchException ignored) {}
        return new Task(name, testType, new StreamConfiguration(inputStreamType, inputFileName),
                new StreamConfiguration(outputStreamType, outputFileName), tests, location, vmArgs, mainClass,
                taskClass, checkerClass, checkerParameters, testClasses, date, contestName, truncate, inputClass,
                outputClass, includeLocale, failOnOverflow);
    }

    public Task setTests(Test[] tests) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale, failOnOverflow);
    }

    public Task setTestClasses(String[] testClasses) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale, failOnOverflow);
    }

    public Task setTaskClass(String taskClass) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale, failOnOverflow);
    }

    public Task setCheckerClass(String checkerClass) {
        return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
                checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale, failOnOverflow);
    }

	public Task setLocation(String location) {
		return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
			checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale, failOnOverflow);
	}

	public Task setInputOutputClasses(String inputClass, String outputClass) {
		return new Task(name, testType, input, output, tests, location, vmArgs, mainClass, taskClass, checkerClass,
			checkerParameters, testClasses, date, contestName, truncate, inputClass, outputClass, includeLocale, failOnOverflow);
	}
}
