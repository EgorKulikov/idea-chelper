package net.egork.chelper.task;

import net.egork.chelper.util.InputReader;
import net.egork.chelper.util.OutputWriter;

import java.util.InputMismatchException;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderTask {
	public final String name;
	public final MethodSignature signature;
	public final NewTopCoderTest[] tests;
    public final String date;
    public final String contestName;
    public final String[] testClasses;
    public final String fqn;
    public final boolean failOnOverflow;

    public TopCoderTask(String name, MethodSignature signature, NewTopCoderTest[] tests, String date, String contestName, String[] testClasses, String fqn, boolean failOnOverflow) {
        this.name = name;
        this.signature = signature;
        this.tests = tests;
        this.date = date;
        this.contestName = contestName;
        this.testClasses = testClasses;
        this.fqn = fqn;
        this.failOnOverflow = failOnOverflow;
    }

    public void saveTask(OutputWriter out) {
        out.printString(name);
        out.printString(signature.name);
        out.printString(signature.result.getCanonicalName());
        out.printLine(signature.arguments.length);
        for (int i = 0; i < signature.argumentNames.length; i++) {
            out.printString(signature.arguments[i].getCanonicalName());
            out.printString(signature.argumentNames[i]);
        }
        out.printLine(tests.length);
        for (NewTopCoderTest test : tests)
            test.saveTest(out);
        out.printString(date);
        out.printString(contestName);
        out.printLine(testClasses.length);
        for (String testClass : testClasses)
            out.printString(testClass);
        out.printString(fqn);
        out.printBoolean(failOnOverflow);
    }

    public static TopCoderTask load(InputReader in) {
        try {
            String name = in.readString();
            String methodName = in.readString();
            Class result = forName(in.readString());
            int argumentCount = in.readInt();
            Class[] arguments = new Class[argumentCount];
            String[] argumentNames = new String[argumentCount];
            for (int i = 0; i < argumentCount; i++) {
                arguments[i] = forName(in.readString());
                argumentNames[i] = in.readString();
            }
            MethodSignature signature = new MethodSignature(methodName, result, arguments, argumentNames);
            int testCount = in.readInt();
            NewTopCoderTest[] tests = new NewTopCoderTest[testCount];
            for (int i = 0; i < testCount; i++)
                tests[i] = NewTopCoderTest.loadTest(in);
            String date = in.readString();
            String contestName = in.readString();
            int testClassCount = in.readInt();
            String[] testClasses = new String[testClassCount];
            for (int i = 0; i < testClassCount; i++)
                testClasses[i] = in.readString();
            String fqn = in.readString();
            boolean failOnOverflow = false;
            try {
                failOnOverflow = in.readBoolean();
            } catch (InputMismatchException ignored) {}
            return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Class forName(String s) throws ClassNotFoundException {
        if ("int".equals(s))
            return int.class;
        if ("long".equals(s))
            return long.class;
        if ("double".equals(s))
            return double.class;
        if ("java.lang.String".equals(s))
            return String.class;
        if ("int[]".equals(s))
            return int[].class;
        if ("long[]".equals(s))
            return long[].class;
        if ("double[]".equals(s))
            return double[].class;
        if ("java.lang.String[]".equals(s))
            return String[].class;
        throw new ClassNotFoundException(s);
    }

    public TopCoderTask setFQN(String fqn) {
        return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow);
    }

    public TopCoderTask setTests(NewTopCoderTest[] tests) {
        return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow);
    }

    public TopCoderTask setTestClasses(String[] testClasses) {
        return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow);
    }

    public TopCoderTask setFailOnOverflow(boolean failOnOverflow) {
        return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow);
    }
}
