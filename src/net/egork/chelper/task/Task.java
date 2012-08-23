package net.egork.chelper.task;

import com.intellij.openapi.project.Project;
import net.egork.chelper.util.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Task {
	public final String name;
	public final String location;
	public final TestType testType;
	public final StreamConfiguration input;
	public final StreamConfiguration output;
	public final Test[] tests;
	public final String heapMemory;
	public final String stackMemory;
	public final boolean truncate;

	public Task(String name, String location, TestType testType, StreamConfiguration input,
		StreamConfiguration output, String heapMemory, String stackMemory, boolean truncate)
	{
		this(name, location, testType, input, output, heapMemory, stackMemory, truncate, new Test[0]);
	}

	public Task(String name, String location, TestType testType, StreamConfiguration input,
		StreamConfiguration output, String heapMemory, String stackMemory, boolean truncate,
		Test[] tests)
	{
		this.name = name;
		this.location = location;
		this.testType = testType;
		this.input = input;
		this.output = output;
		this.tests = tests;
		this.heapMemory = heapMemory;
		this.stackMemory = stackMemory;
		this.truncate = truncate;
	}

    public Task setName(String name) {
		return new Task(name, location, testType, input, output, heapMemory, stackMemory, truncate, tests);
	}

	public Task setDirectory(String location) {
		return new Task(name, location, testType, input, output, heapMemory, stackMemory, truncate, tests);
	}

	public Task setTests(Test[] tests) {
		return new Task(name, location, testType, input, output, heapMemory, stackMemory, truncate, tests);
	}

    public void saveTask(OutputWriter out, Project project) {
		out.printString(name);
		out.printString(location);
		out.printEnum(testType);
		out.printEnum(input.type);
		out.printString(input.fileName);
		out.printEnum(output.type);
		out.printString(output.fileName);
		out.printString(heapMemory);
		out.printString(stackMemory);
		out.printBoolean(truncate);
		out.printLine(tests.length);
		for (Test test : tests)
			test.saveTest(out);
		out.printString(TaskUtilities.getFQN(location, name, project));
	}

    public static Task loadTask(InputReader in) {
        String name = in.readString();
        String location = in.readString();
        TestType testType = in.readEnum(TestType.class);
        StreamConfiguration.StreamType inputStreamType = in.readEnum(StreamConfiguration.StreamType.class);
        String inputFileName = in.readString();
        StreamConfiguration.StreamType outputStreamType = in.readEnum(StreamConfiguration.StreamType.class);
        String outputFileName = in.readString();
        String heapMemory = in.readString();
        String stackMemory = in.readString();
        boolean truncate = in.readBoolean();
        int testCount = in.readInt();
        Test[] tests = new Test[testCount];
        for (int i = 0; i < testCount; i++)
            tests[i] = Test.loadTest(in);
        return new Task(name, location, testType, new StreamConfiguration(inputStreamType, inputFileName),
                new StreamConfiguration(outputStreamType, outputFileName), heapMemory, stackMemory, truncate,
                tests);
    }
}
