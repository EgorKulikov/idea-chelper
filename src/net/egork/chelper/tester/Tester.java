package net.egork.chelper.tester;

import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;
import net.egork.chelper.util.EncodingUtilities;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Tester {
    public static void main(String[] args) throws InterruptedException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        test(args);
    }

    public static boolean test(String... args) throws InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InterruptedException {
        Locale.setDefault(Locale.US);
        List<Verdict> verdicts = new ArrayList<Verdict>();
        long maximalTime = 0;
        boolean ok = true;
        int argumentIndex = 0;
        String readerFQN = args[argumentIndex++];
        String fqn = args[argumentIndex++];
        TestType testType = TestType.valueOf(args[argumentIndex++]);
        List<Test> tests = new ArrayList<Test>();
        tests.addAll(Arrays.asList(decode(args[argumentIndex++])));
        tests.addAll(addGeneratedTests(fqn, tests.size()));
        String writerFQN;
        if (argumentIndex != args.length) {
            writerFQN = args[argumentIndex++];
        } else {
            writerFQN = "java.io.PrintWriter";
        }
        boolean truncate = true;
        if (argumentIndex < args.length) {
            truncate = Boolean.parseBoolean(args[argumentIndex]);
        }
        Class readerClass = Class.forName(readerFQN);
        Class writerClass = Class.forName(writerFQN);
        Class taskClass = Class.forName(fqn);
        Class checkerClass = Class.forName(fqn + "Checker");
        for (Test test : tests) {
            if (!test.active) {
                verdicts.add(Verdict.SKIPPED);
                System.out.println("Test #" + test.index + ": SKIPPED");
                System.out.println("------------------------------------------------------------------");
                continue;
            }
            System.out.println("Test #" + test.index + ":");
            Object in = readerClass.getConstructor(InputStream.class).newInstance(new StringInputStream(test.input));
            StringWriter writer = new StringWriter(test.output.length());
            Object out = writerClass.getConstructor(Writer.class).newInstance(writer);
            System.out.println("Input:");
            print(test.input, truncate);
            System.out.println("Expected output:");
            print(test.output, truncate);
            System.out.println("Execution result:");
            long time = System.currentTimeMillis();
            try {
                run(in, out, taskClass, readerClass, writerClass, testType);
                time = System.currentTimeMillis() - time;
                maximalTime = Math.max(time, maximalTime);
                String result = writer.getBuffer().toString();
                print(result, truncate);
                System.out.print("Verdict: ");
                Verdict checkResult = check(checkerClass, readerClass,
                        readerClass.getConstructor(InputStream.class).newInstance(new StringInputStream(test.input)),
                        readerClass.getConstructor(InputStream.class).newInstance(new StringInputStream(test.output)),
                        readerClass.getConstructor(InputStream.class).newInstance(new StringInputStream(result)));
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

    private static void print(String s, boolean truncate) {
        if (truncate && s.length() > 2000) {
            s = s.substring(0, 1500) + "..." + s.substring(s.length() - 100);
        }
        System.out.println(s);
    }

    private static Verdict check(Class checkerClass, Class readerClass, Object input, Object expectedOutput,
                                 Object actualOutput)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        Method check = checkerClass.getMethod("check", readerClass, readerClass, readerClass);
        Object checker = checkerClass.getConstructor().newInstance();
        Object checkResult = check.invoke(checker, input, expectedOutput, actualOutput);
        Verdict verdict;
        if (checkResult == null || checkResult instanceof String) {
            if (checkResult == null) {
                verdict = Verdict.OK;
            } else if (checkResult.equals("")) {
                verdict = Verdict.UNDECIDED;
            } else {
                verdict = new Verdict(Verdict.VerdictType.WA, (String) checkResult);
            }
        } else {
            verdict = (Verdict) checkResult;
        }
        if (checkResult == null) {
            try {
                readerClass.getMethod("next").invoke(actualOutput);
                return new Verdict(Verdict.VerdictType.PE, "Excessive output");
            } catch (Throwable e) {
                return verdict;
            }
        }
        if (verdict != Verdict.UNDECIDED) {
            return verdict;
        }
        Method next = readerClass.getMethod("next");
        double certainty = (Double) checkerClass.getMethod("getCertainty").invoke(checker);
        int index = 0;
        double maxDelta = 0;
        while (true) {
            String expectedToken;
            try {
                expectedToken = (String) next.invoke(expectedOutput);
            } catch (Throwable e) {
                try {
                    next.invoke(actualOutput);
                } catch (Throwable t) {
                    if (maxDelta != 0) {
                        return new Verdict(Verdict.VerdictType.OK, "Maximal absolute difference is " + maxDelta);
                    }
                    return Verdict.OK;
                }
                return new Verdict(Verdict.VerdictType.PE, "Only " + index + " tokens were expected");
            }
            String actualToken;
            try {
                actualToken = (String) next.invoke(actualOutput);
            } catch (Throwable e) {
                return new Verdict(Verdict.VerdictType.PE, "More than " + index + " tokens were expected");
            }
            if (!expectedToken.equals(actualToken)) {
                if (certainty != 0) {
                    try {
                        double expectedValue = Double.parseDouble(expectedToken);
                        double actualValue = Double.parseDouble(actualToken);
                        double delta = Math.abs(actualValue - expectedValue);
                        maxDelta = Math.max(delta, maxDelta);
                        if (delta / Math.max(Math.abs(expectedValue), 1) > certainty) {
                            return new Verdict(Verdict.VerdictType.WA, "Mismatch at index " + index);
                        }
                    } catch (NumberFormatException e) {
                        return new Verdict(Verdict.VerdictType.WA, "Mismatch at index " + index);
                    }
                } else {
                    return new Verdict(Verdict.VerdictType.WA, "Mismatch at index " + index);
                }
            }
            index++;
        }
    }

    private static void run(Object in, Object out, Class taskClass, Class readerClass, Class writerClass,
                            TestType testType)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        Object solver = taskClass.getConstructor().newInstance();
        Method solve = taskClass.getMethod("solve", int.class, readerClass, writerClass);
        if (testType == TestType.SINGLE) {
            solve.invoke(solver, 1, in, out);
            return;
        }
        if (testType == TestType.MULTI_EOF) {
            try {
                int testIndex = 1;
                //noinspection InfiniteLoopStatement
                while (true)
                    solve.invoke(solver, testIndex++, in, out);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof UnknownError) {
                    return;
                }
                throw e;
            }
        }
        if (testType == TestType.MULTI_NUMBER) {
            Method read = readerClass.getMethod("next");
            String testCount = (String) read.invoke(in);
            int count = Integer.parseInt(testCount);
            for (int i = 0; i < count; i++)
                solve.invoke(solver, i + 1, in, out);
        }
    }

    private static Collection<? extends Test> addGeneratedTests(String fqn, int initialTestCount)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
            InstantiationException {
        fqn += "Checker";
        Class aClass = Class.forName(fqn);
        Object checker = aClass.getConstructor().newInstance();
        Method method = aClass.getMethod("generateTests");
        //noinspection unchecked
        Collection<? extends Test> tests = (Collection<? extends Test>) method.invoke(checker);
        Collection<Test> result = new ArrayList<Test>(tests.size());
        for (Test test : tests)
            result.add(test.setIndex(initialTestCount++));
        return result;
    }

    private static Test[] decode(String s) {
        if ("empty".equals(s)) {
            return new Test[0];
        }
        String[] tokens = s.split("::", -1);
        Test[] tests = new Test[tokens.length];
        for (int i = 0; i < tests.length; i++)
            tests[i] = EncodingUtilities.decodeTest(i, tokens[i]);
        return tests;
    }
}
