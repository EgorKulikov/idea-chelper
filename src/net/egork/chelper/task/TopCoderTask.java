package net.egork.chelper.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.notification.NotificationType;
import net.egork.chelper.util.InputReader;
import net.egork.chelper.util.Messenger;

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
    public final String memoryLimit;

    @JsonCreator
    public TopCoderTask(@JsonProperty("name") String name,
                        @JsonProperty("signature") MethodSignature signature,
                        @JsonProperty("tests") NewTopCoderTest[] tests,
                        @JsonProperty("date") String date,
                        @JsonProperty("contestName") String contestName,
                        @JsonProperty("testClasses") String[] testClasses,
                        @JsonProperty("fqn") String fqn,
                        @JsonProperty("failOnOverflow") boolean failOnOverflow,
                        @JsonProperty("memoryLimit") String memoryLimit) {
        this.name = name;
        this.signature = signature;
        this.tests = tests;
        this.date = date;
        this.contestName = contestName;
        this.testClasses = testClasses;
        this.fqn = fqn;
        this.failOnOverflow = failOnOverflow;
        this.memoryLimit = memoryLimit;
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
            String memoryLimit = "64M";
            try {
                failOnOverflow = in.readBoolean();
                memoryLimit = in.readString();
            } catch (InputMismatchException ignored) {
            }
            return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow, memoryLimit);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public String defaultValue() {
        Class returnType = MethodSignature.getClass(signature.result);
        if (returnType == int.class) {
            return "0";
        }
        if (returnType == long.class) {
            return "0L";
        }
        if (returnType == double.class) {
            return "0D";
        }
        if (returnType == String.class) {
            return "\"\"";
        }
        if (returnType == int[].class) {
            return "new int[0]";
        }
        if (returnType == long[].class) {
            return "new long[0]";
        }
        if (returnType == double[].class) {
            return "new double[0]";
        }
        if (returnType == String[].class) {
            return "new String[0]";
        }
        Messenger.publishMessage("Task " + name + " has unrecognized return type - " +
                signature.result, NotificationType.ERROR);
        return "";
    }

    private static Class forName(String s) throws ClassNotFoundException {
        if ("int".equals(s)) {
            return int.class;
        }
        if ("long".equals(s)) {
            return long.class;
        }
        if ("double".equals(s)) {
            return double.class;
        }
        if ("java.lang.String".equals(s)) {
            return String.class;
        }
        if ("int[]".equals(s)) {
            return int[].class;
        }
        if ("long[]".equals(s)) {
            return long[].class;
        }
        if ("double[]".equals(s)) {
            return double[].class;
        }
        if ("java.lang.String[]".equals(s)) {
            return String[].class;
        }
        throw new ClassNotFoundException(s);
    }

    public TopCoderTask setFQN(String fqn) {
        return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow, memoryLimit);
    }

    public TopCoderTask setTests(NewTopCoderTest[] tests) {
        return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow, memoryLimit);
    }

    public TopCoderTask setTestClasses(String[] testClasses) {
        return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow, memoryLimit);
    }

    public TopCoderTask setFailOnOverflow(boolean failOnOverflow) {
        return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow, memoryLimit);
    }
}
