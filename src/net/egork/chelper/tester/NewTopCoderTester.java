package net.egork.chelper.tester;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.egork.chelper.task.MethodSignature;
import net.egork.chelper.task.NewTopCoderTest;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.InputReader;
import net.egork.chelper.util.OutputWriter;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class NewTopCoderTester {
    private static ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args)
            throws InterruptedException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, IOException {
        test(args);
    }

    public static boolean test(String... args)
            throws InterruptedException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, IOException {
        Locale.setDefault(Locale.US);
        List<Verdict> verdicts = new ArrayList<Verdict>();
        long maximalTime = 0;
        boolean ok = true;
        String taskFileName = args[0];
        int singleTest = -1;
        if (args.length > 1) {
            singleTest = Integer.parseInt(args[1]);
        }
        TopCoderTask task;
        if (taskFileName.endsWith(".tctask")) {
            InputReader input;
            try {
                input = new InputReader(new FileInputStream(taskFileName));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            task = TopCoderTask.load(input);
        } else {
            task = mapper.readValue(new File(taskFileName), TopCoderTask.class);
        }
        List<NewTopCoderTest> tests = new ArrayList<NewTopCoderTest>(Arrays.asList(task.tests));
        for (String testClass : task.testClasses) {
            Class test = Class.forName(testClass);
            Object provider = test.getConstructor().newInstance();
            for (Method method : test.getMethods()) {
                if (method.getAnnotation(TestCase.class) != null) {
                    Collection<NewTopCoderTest> providedTests = (Collection<NewTopCoderTest>) method.invoke(provider);
                    for (NewTopCoderTest testCase : providedTests)
                        tests.add(new NewTopCoderTest(testCase.arguments, testCase.result, tests.size(), testCase.active));
                }
            }
            if (provider instanceof TopCoderTestProvider) {
                for (NewTopCoderTest testCase : ((TopCoderTestProvider) provider).createTests())
                    tests.add(new NewTopCoderTest(testCase.arguments, testCase.result, tests.size(), testCase.active));
            }
        }
        Class taskClass = Class.forName(task.fqn);
        System.out.println(task.contestName + " - " + task.name);
        System.out.println("------------------------------------------------------------------");
        int testNumber = 0;
        for (NewTopCoderTest test : tests) {
            if (singleTest != -1 && testNumber++ != singleTest || !test.active) {
                verdicts.add(Verdict.SKIPPED);
                System.out.println("Test #" + test.index + ": SKIPPED");
                System.out.println("------------------------------------------------------------------");
                continue;
            }
            System.out.println("Test #" + test.index + ":");
            System.out.println("Input:");
            Object[] arguments = test.getArguments(task.signature);
            for (int i = 0, argumentsLength = arguments.length; i < argumentsLength; i++) {
                Object argument = arguments[i];
                System.out.println(NewTopCoderTest.toString(argument, MethodSignature.getClass(task.signature.arguments[i])));
            }
            System.out.println("Expected output:");
            System.out.println(NewTopCoderTest.toString(test.result, MethodSignature.getClass(task.signature.result)));
            System.out.println("Execution result:");
            long time = System.currentTimeMillis();
            try {
                Object actual = run(taskClass, task.signature, test);
                time = System.currentTimeMillis() - time;
                maximalTime = Math.max(time, maximalTime);
                System.out.println(NewTopCoderTest.toString(actual, MethodSignature.getClass(task.signature.result)));
                System.out.print("Verdict: ");
                Verdict checkResult = check(actual, test.getResult(task.signature), MethodSignature.getClass(task.signature.result));
                verdicts.add(checkResult);
                System.out.print(checkResult);
                System.out.printf(" in %.3f s.\n", time / 1000.);
                if (checkResult.type != Verdict.VerdictType.OK && checkResult.type != Verdict.VerdictType.UNDECIDED) {
                    ok = false;
                }
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException) {
                    e = e.getCause();
                }
                System.out.println("Exception thrown:");
                e.printStackTrace(System.out);
                verdicts.add(new Verdict(Verdict.VerdictType.RTE, e.getClass().getSimpleName()));
                ok = false;
            }
            System.out.println("------------------------------------------------------------------");
        }
        System.out.println("==================================================================");
        System.out.println("Test results:");
        if (ok) {
            System.out.printf("All test passed in %.3f s.\n", maximalTime / 1000.);
            if (singleTest != -1) {
                return test(args[0]);
            }
        } else {
            if (singleTest == -1) {
                for (int i = 0; i < verdicts.size(); i++)
                    System.out.println("Test #" + i + ": " + verdicts.get(i));
            }
        }
        try {
            OutputWriter report = new OutputWriter(new FileOutputStream("CHelperReport.txt"));
            report.printString(args[0]);
            List<Integer> failed = new ArrayList<Integer>();
            for (int i = 0; i < verdicts.size(); i++) {
                if (verdicts.get(i).type != Verdict.VerdictType.OK && verdicts.get(i).type != Verdict.VerdictType.UNDECIDED && verdicts.get(i).type != Verdict.VerdictType.SKIPPED) {
                    failed.add(i);
                }
            }
            report.printLine(failed.size());
            for (int i : failed)
                report.printLine(i);
        } catch (FileNotFoundException e) {
            System.err.println("Can't write report");
        }
        Thread.currentThread().join(100L);
        return ok;
    }

    private static Verdict check(Object actual, Object expected, Class outputClass)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        if (expected == null) {
            return Verdict.UNDECIDED;
        }
        if (outputClass == int[].class) {
            return verdict(Arrays.equals((int[]) expected, (int[]) actual));
        }
        if (outputClass == long[].class) {
            return verdict(Arrays.equals((long[]) expected, (long[]) actual));
        }
        if (outputClass == double[].class) {
            double[] expectedArray = (double[]) expected;
            double[] actualArray = (double[]) actual;
            double maxDelta = 0;
            for (int i = 0; i < expectedArray.length; i++) {
                double expectedValue = expectedArray[i];
                double actualValue = actualArray[i];
                double delta = Math.abs(expectedValue - actualValue);
                if (checkDouble(expectedValue, delta)) {
                    maxDelta = Math.max(maxDelta, delta);
                } else {
                    return Verdict.WA;
                }
            }
            return new Verdict(Verdict.VerdictType.OK, "Maximal absolute difference " + maxDelta);
        }
        if (outputClass == String[].class) {
            return verdict(Arrays.deepEquals((String[]) expected, (String[]) actual));
        }
        if (outputClass == double.class) {
            double expectedValue = (Double) expected;
            double actualValue = (Double) actual;
            double delta = Math.abs(expectedValue - actualValue);
            if (checkDouble(expectedValue, delta)) {
                return new Verdict(Verdict.VerdictType.OK, "Absolute difference " + delta);
            }
            return Verdict.WA;
        }
        return verdict(expected.equals(actual));
    }

    private static boolean checkDouble(double expected, double delta) {
        return delta <= 1e-9 * Math.max(Math.abs(expected), 1);
    }

    private static Verdict verdict(boolean equals) {
        return equals ? Verdict.OK : Verdict.WA;
    }

    private static Object run(Class taskClass, MethodSignature signature, NewTopCoderTest test)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        Object solver = taskClass.getConstructor().newInstance();
        Method solve = taskClass.getMethod(signature.name, MethodSignature.getClasses(signature.arguments));
        return solve.invoke(solver, test.getArguments(signature));
    }

}
