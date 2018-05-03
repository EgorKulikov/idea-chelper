package net.egork.chelper.tester;

import net.egork.chelper.task.MethodSignature;
import net.egork.chelper.task.TopCoderTest;
import net.egork.chelper.util.EncodingUtilities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderTester {
    public static void main(String[] args)
            throws InterruptedException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        test(args);
    }

    public static boolean test(String... args)
            throws InterruptedException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        Locale.setDefault(Locale.US);
        List<Verdict> verdicts = new ArrayList<Verdict>();
        long maximalTime = 0;
        boolean ok = true;
        int argumentIndex = 0;
        String signature = args[argumentIndex++];
        MethodSignature methodSignature = MethodSignature.parse(signature);
        String fqn = args[argumentIndex++];
        List<TopCoderTest> tests = new ArrayList<TopCoderTest>();
        tests.addAll(Arrays.asList(decode(args[argumentIndex], methodSignature)));
        Class taskClass = Class.forName(fqn);
        for (TopCoderTest test : tests) {
            if (!test.active) {
                verdicts.add(Verdict.SKIPPED);
                System.out.println("Test #" + test.index + ": SKIPPED");
                System.out.println("------------------------------------------------------------------");
                continue;
            }
            System.out.println("Test #" + test.index + ":");
            System.out.println("Input:");
            for (String argument : test.arguments)
                System.out.println(argument);
            System.out.println("Expected output:");
            System.out.println(test.result);
            System.out.println("Execution result:");
            long time = System.currentTimeMillis();
            try {
                Object actual = run(taskClass, methodSignature, test);
                time = System.currentTimeMillis() - time;
                maximalTime = Math.max(time, maximalTime);
                System.out.println(print(MethodSignature.getClass(methodSignature.result), actual));
                System.out.print("Verdict: ");
                Verdict checkResult = check(actual, test.result, MethodSignature.getClass(methodSignature.result));
                verdicts.add(checkResult);
                System.out.print(checkResult);
                System.out.printf(" in %.3f s.\n", time / 1000.);
                if (checkResult.type != Verdict.VerdictType.OK) {
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
        } else {
            for (int i = 0; i < verdicts.size(); i++)
                System.out.println("Test #" + i + ": " + verdicts.get(i));
        }
        Thread.currentThread().join(100L);
        return ok;
    }

    private static String print(Class result, Object actual) {
        if (result == int[].class) {
            return Arrays.toString((int[]) actual);
        }
        if (result == long[].class) {
            return Arrays.toString((long[]) actual);
        }
        if (result == double[].class) {
            return Arrays.toString((double[]) actual);
        }
        if (result == String[].class) {
            return Arrays.toString((String[]) actual);
        }
        return actual.toString();
    }

    private static Verdict check(Object actual, String expectedOutput, Class outputClass)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        Object expected = MethodSignature.resolve(outputClass, expectedOutput);
        if (outputClass == int[].class) {
            return verdict(Arrays.equals((int[]) expected, (int[]) actual));
        }
        if (outputClass == long[].class) {
            return verdict(Arrays.equals((long[]) expected, (long[]) actual));
        }
        if (outputClass == double[].class) {
            return verdict(Arrays.equals((double[]) expected, (double[]) actual));
        }
        if (outputClass == String[].class) {
            return verdict(Arrays.deepEquals((String[]) expected, (String[]) actual));
        }
        if (outputClass == double.class) {
            double expectedValue = (Double) expected;
            double actualValue = (Double) actual;
            double delta = Math.abs(expectedValue - actualValue);
            if (delta <= 1e-9 * Math.max(Math.abs(expectedValue), 1)) {
                return new Verdict(Verdict.VerdictType.OK, "Absolute difference " + delta);
            }
            return Verdict.WA;
        }
        return verdict(expected.equals(actual));
    }

    private static Verdict verdict(boolean equals) {
        return equals ? Verdict.OK : Verdict.WA;
    }

    private static Object run(Class taskClass, MethodSignature signature, TopCoderTest test)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        Object solver = taskClass.getConstructor().newInstance();
        Method solve = taskClass.getMethod(signature.name, MethodSignature.getClasses(signature.arguments));
        return solve.invoke(solver, signature.generateArguments(test));
    }

    private static TopCoderTest[] decode(String s, MethodSignature methodSignature) {
        if ("empty".equals(s)) {
            return new TopCoderTest[0];
        }
        String[] tokens = s.split("::", -1);
        TopCoderTest[] tests = new TopCoderTest[tokens.length];
        for (int i = 0; i < tests.length; i++)
            tests[i] = EncodingUtilities.decodeTopCoderTest(i, tokens[i], methodSignature.arguments.length);
        return tests;
    }
}
