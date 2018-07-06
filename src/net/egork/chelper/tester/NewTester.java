package net.egork.chelper.tester;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.egork.chelper.checkers.Checker;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;
import net.egork.chelper.util.EncodingUtilities;
import net.egork.chelper.util.InputReader;
import net.egork.chelper.util.OutputWriter;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class NewTester {
    private static ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static boolean ok;
    private static long maximalTime;
    private static volatile Throwable lastException;

    public static void main(String[] args) throws InterruptedException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        test(args);
    }

    public static boolean test(String... args) throws InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InterruptedException, IOException {
        Locale.setDefault(Locale.US);
        List<Verdict> verdicts = new ArrayList<Verdict>();
        maximalTime = 0;
        ok = true;
        String taskFileName = args[0];
        int singleTest = -1;
        if (args.length > 1) {
            singleTest = Integer.parseInt(args[1]);
        }
        Task task;
        if (taskFileName.endsWith(".task")) {
            InputReader input;
            try {
                input = new InputReader(new FileInputStream(taskFileName));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            task = Task.loadTask(input);
        } else {
            task = mapper.readValue(new File(taskFileName), Task.class);
        }
        String readerFQN = task.inputClass;
        String fqn = task.taskClass;
        List<Test> tests = new ArrayList<Test>(Arrays.asList(task.tests));
        for (String testClass : task.testClasses) {
            Class test = Class.forName(testClass);
            Object provider = test.getConstructor().newInstance();
            for (Method method : test.getMethods()) {
                if (method.getAnnotation(TestCase.class) != null) {
                    Collection<Test> providedTests = (Collection<Test>) method.invoke(provider);
                    for (Test testCase : providedTests)
                        tests.add(new Test(testCase.input, testCase.output, tests.size(), testCase.active));
                }
            }
            if (provider instanceof TestProvider) {
                for (Test testCase : ((TestProvider) provider).createTests())
                    tests.add(new Test(testCase.input, testCase.output, tests.size(), testCase.active));
            }
        }
        String writerFQN = task.outputClass;
        Class readerClass = Class.forName(readerFQN);
        Class writerClass = Class.forName(writerFQN);
        Class taskClass = Class.forName(fqn);
        Class checkerClass = Class.forName(task.checkerClass);
        Class interactorClass = task.interactor == null ? null : Class.forName(task.interactor);
        TestType testType = task.testType;
        boolean truncate = task.truncate;
        if (task.contestName.isEmpty()) {
            System.out.println(task.name);
        } else {
            System.out.println(task.contestName + " - " + task.name);
        }
        System.out.println("------------------------------------------------------------------");
        int testNumber = 0;
        for (Test test : tests) {
            if (singleTest != -1 && testNumber++ != singleTest || !test.active) {
                verdicts.add(Verdict.SKIPPED);
                System.out.println("Test #" + test.index + ": SKIPPED");
                System.out.println("------------------------------------------------------------------");
                continue;
            }
            System.out.println("Test #" + test.index + ":");
            if (task.interactive) {
                runInteractiveTask(verdicts, task, readerClass, writerClass, taskClass, interactorClass, test, truncate);
            } else {
                runClassicTask(verdicts, task, readerClass, writerClass, taskClass, checkerClass, testType, truncate, test);
            }
            System.out.println("------------------------------------------------------------------");
        }
        System.out.println("==================================================================");
        System.out.println("Test results:");
        if (ok) {
            System.out.printf("All test passed input %.3f s.\n", maximalTime / 1000.);
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

    private static void runInteractiveTask(List<Verdict> verdicts, Task task, Class readerClass, Class writerClass,
                                           Class taskClass, Class interactorClass, Test test, boolean truncate) throws IOException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        PipedInputStream interactorToSolutionInputStream = new PipedInputStream();
        PipedOutputStream interactorToSolutionOutputStream = new PipedOutputStream(interactorToSolutionInputStream) {
            @Override
            public void write(int b) throws IOException {
                if (!interactorClass.equals(Interactor.class)) {
                    System.err.write(b);
                }
                super.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (!interactorClass.equals(Interactor.class)) {
                    System.err.write(b, off, len);
                }
                super.write(b, off, len);
            }
        };
        PipedInputStream solutionToInteractorInputStream = new PipedInputStream();
        PipedOutputStream solutionToInteractorOutputStream = new PipedOutputStream(solutionToInteractorInputStream) {
            @Override
            public void write(int b) throws IOException {
                System.out.write(b);
                super.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                System.out.write(b, off, len);
                super.write(b, off, len);
            }
        };
        Object in = readerClass.getConstructor(InputStream.class).newInstance(interactorToSolutionInputStream);
        Object out = writerClass.getConstructor(OutputStream.class).newInstance(solutionToInteractorOutputStream);
        Object interactor = interactorClass.newInstance();
        Object solution = taskClass.newInstance();
        InputStream input = new StringInputStream(test.input);
        System.out.println("Input:");
        print(test.input, truncate);
        System.out.println("Expected output:");
        print(test.output, truncate);
        System.out.println("Interaction:");
        State<Boolean> state = new State<>(true);
        long time = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    taskClass.getMethod("solve", int.class, readerClass, writerClass).invoke(solution, 1, in, out);
                } catch (Throwable e) {
                    lastException = e;
                }
                state.setState(false);
            }
        }, "SolutionThread").start();
        Verdict verdict = (Verdict) interactorClass.getMethod("interact", InputStream.class, InputStream.class, OutputStream.class,
                State.class).invoke(interactor, input, solutionToInteractorInputStream,
                interactorToSolutionOutputStream, state);
        if (lastException != null) {
            System.out.println("Exception thrown:");
            lastException.printStackTrace(System.out);
            verdict = new Verdict(Verdict.VerdictType.RTE, lastException.getMessage());
            lastException = null;
        }
        verdicts.add(verdict);
        time = System.currentTimeMillis() - time;
        maximalTime = Math.max(time, maximalTime);
        System.out.print("Verdict: ");
        System.out.print(verdict);
        System.out.printf(" in %.3f s.\n", time / 1000.);
        if (verdict.type != Verdict.VerdictType.OK && verdict.type != Verdict.VerdictType.UNDECIDED) {
            ok = false;
        }
    }

    private static void runClassicTask(List<Verdict> verdicts, Task task, Class readerClass, Class writerClass, Class taskClass, Class checkerClass, TestType testType, boolean truncate, Test test) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object in = readerClass.getConstructor(InputStream.class).newInstance(new StringInputStream(test.input));
        StringWriter writer = new StringWriter(test.output == null ? 16 : test.output.length());
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
            Verdict checkResult = check(checkerClass, test.input, test.output, result, task.checkerParameters);
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
    }

    private static void print(String s, boolean truncate) {
        if (s == null) {
            s = "Not provided";
        }
        if (truncate && s.length() > 2000) {
            s = s.substring(0, 1500) + "..." + s.substring(s.length() - 100);
        }
        System.out.println(s);
    }

    private static Verdict check(Class checkerClass, String input, String expectedOutput,
                                 String actualOutput, String checkerParameters)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        Method check = checkerClass.getMethod("check", String.class, String.class, String.class);
        Checker checker = (Checker) checkerClass.getConstructor(String.class).newInstance(checkerParameters);
        return checker.check(input, expectedOutput, actualOutput);
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
