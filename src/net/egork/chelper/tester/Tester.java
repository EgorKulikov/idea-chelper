package net.egork.chelper.tester;

import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;
import net.egork.chelper.util.EncodingUtilities;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Tester {
	private static enum Verdict {
			OK, WA, RTE
		}

	public static void main(String[] args) throws InterruptedException, InvocationTargetException,
		ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException
	{
		test(args);
	}

	public static boolean test(String...args) throws InvocationTargetException, ClassNotFoundException,
		NoSuchMethodException, InstantiationException, IllegalAccessException
	{
		Locale.setDefault(Locale.US);
		List<Verdict> verdicts = new ArrayList<Verdict>();
		long maximalTime = 0;
		boolean ok = true;
		Set<Integer> testCases = new HashSet<Integer>();
		int argumentIndex = 0;
		String readerFQN = args[argumentIndex++];
		String fqn = args[argumentIndex++];
		TestType testType = TestType.valueOf(args[argumentIndex++]);
		List<Test> tests = new ArrayList<Test>();
		tests.addAll(Arrays.asList(decode(args[argumentIndex++])));
		tests.addAll(addGeneratedTests(fqn, tests.size()));
		for (int i = argumentIndex; i < args.length; i++)
			testCases.add(Integer.parseInt(args[i]));
		Class readerClass = Class.forName(readerFQN);
		Class taskClass = Class.forName(fqn);
		Class checkerClass = Class.forName(fqn + "Checker");
		for (Test test : tests) {
			if (!testCases.isEmpty() && !testCases.contains(test.index))
				continue;
			System.out.println("Test #" + test.index + ":");
			Object in = readerClass.getConstructor(InputStream.class).newInstance(new StringInputStream(test.input));
			StringWriter out = new StringWriter(test.output.length());
			System.out.println("Input:");
			System.out.println(test.input);
			System.out.println("Expected output:");
			System.out.println(test.output);
			System.out.println("Execution result:");
			long time = System.currentTimeMillis();
			try {
				run(in, new PrintWriter(out), taskClass, readerClass, testType);
				time = System.currentTimeMillis() - time;
				maximalTime = Math.max(time, maximalTime);
				String result = out.getBuffer().toString();
				System.out.println(result);
				System.out.print("Verdict: ");
				String checkResult = check(checkerClass, readerClass,
					readerClass.getConstructor(InputStream.class).newInstance(new StringInputStream(test.input)),
					readerClass.getConstructor(InputStream.class).newInstance(new StringInputStream(test.output)),
					readerClass.getConstructor(InputStream.class).newInstance(new StringInputStream(result)));
				if (checkResult == null) {
					System.out.print("OK");
					verdicts.add(Verdict.OK);
				} else {
					System.out.print("WA (" + checkResult + ")");
					verdicts.add(Verdict.WA);
					ok = false;
				}
				System.out.printf(" in %.3f s.\n", time / 1000.);
			} catch (Throwable e) {
				if (e instanceof InvocationTargetException)
					e = e.getCause();
				System.out.println("Exception thrown:");
				e.printStackTrace(System.out);
				verdicts.add(Verdict.RTE);
				ok = false;
			}
			System.out.println("------------------------------------------------------------------");
		}
		System.out.println("==================================================================");
		System.out.println("Test results:");
		if (ok)
			System.out.printf("All test passed in %.3f s.\n", maximalTime / 1000.);
		else {
			for (int i = 0; i < verdicts.size(); i++)
				System.out.println("Test #" + i + ": " + verdicts.get(i));
		}
		return ok;
	}

	private static String check(Class checkerClass, Class readerClass, Object input, Object expectedOutput,
		Object actualOutput)
		throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException
	{
		Method check = checkerClass.getMethod("check", readerClass, readerClass, readerClass);
		Object checker = checkerClass.getConstructor().newInstance();
		String checkResult = (String)check.invoke(checker, input, expectedOutput, actualOutput);
		if (checkResult == null || checkResult.length() != 0)
			return checkResult;
		Method next = readerClass.getMethod("next");
		double certainty = (Double) checkerClass.getMethod("getCertainty").invoke(checker);
		int index = 0;
		while (true) {
			String expectedToken;
			try {
				expectedToken = (String) next.invoke(expectedOutput);
			} catch (Throwable e) {
				try {
					next.invoke(actualOutput);
				} catch (Throwable t) {
					return null;
				}
				return "Only " + index + " tokens were expected";
			}
			String actualToken;
			try {
				actualToken = (String) next.invoke(actualOutput);
			} catch (Throwable e) {
				return "More than " + index + " tokens were expected";
			}
			if (!expectedToken.equals(actualToken)) {
				if (certainty != 0) {
					try {
						double expectedValue = Double.parseDouble(expectedToken);
						double actualValue = Double.parseDouble(actualToken);
						if (Math.abs(actualValue - expectedValue) / Math.max(Math.abs(expectedValue), 1) > certainty)
							return "Mismatch at index " + index;
					} catch (NumberFormatException e) {
						return "Mismatch at index " + index;
					}
				} else
					return "Mismatch at index " + index;
			}
			index++;
		}
	}

	private static void run(Object in, PrintWriter out, Class taskClass, Class readerClass, TestType testType)
		throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException
	{
		Object solver = taskClass.getConstructor().newInstance();
		Method solve = taskClass.getMethod("solve", int.class, readerClass, PrintWriter.class);
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
				if (e.getCause() instanceof UnknownError)
					return;
				throw e;
			}
		}
		if (testType == TestType.MULTI_NUMBER) {
			Method read = readerClass.getMethod("next");
			String testCount = (String)read.invoke(in);
			int count = Integer.parseInt(testCount);
			for (int i = 0; i < count; i++)
				solve.invoke(solver, i + 1, in, out);
		}
	}

	private static Collection<? extends Test> addGeneratedTests(String fqn, int initialTestCount)
		throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
		InstantiationException
	{
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
		if ("empty".equals(s))
			return new Test[0];
		String[] tokens = s.split("::", -1);
		Test[] tests = new Test[tokens.length];
		for (int i = 0; i < tests.length; i++)
			tests[i] = EncodingUtilities.decodeTest(i, tokens[i]);
		return tests;
	}
}
