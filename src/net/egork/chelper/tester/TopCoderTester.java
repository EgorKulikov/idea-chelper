package net.egork.chelper.tester;

import net.egork.chelper.task.MethodSignature;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TopCoderTest;
import net.egork.chelper.util.EncodingUtilities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderTester {
	private static enum Verdict {
			OK, WA, RTE, SKIPPED
		}

	public static void main(String[] args)
		throws InterruptedException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
		InstantiationException, IllegalAccessException
	{
		test(args);
	}

	public static boolean test(String...args)
		throws InterruptedException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
		InstantiationException, IllegalAccessException
	{
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
				System.out.println(print(methodSignature.result, actual));
				System.out.print("Verdict: ");
				String checkResult = check(actual, test.result, methodSignature.result);
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
		Thread.currentThread().join(100L);
		return ok;
	}

	private static String print(Class result, Object actual) {
		if (result == int[].class)
			return Arrays.toString((int[])actual);
		if (result == long[].class)
			return Arrays.toString((long[])actual);
		if (result == double[].class)
			return Arrays.toString((double[])actual);
		if (result == String[].class)
			return Arrays.toString((String[])actual);
		return actual.toString();
	}

	private static String check(Object actual, String expectedOutput, Class outputClass)
		throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException
	{
		Object expected = MethodSignature.resolve(outputClass, expectedOutput);
		if (outputClass == int[].class)
			return verdict(Arrays.equals((int[])expected, (int[])actual));
		if (outputClass == long[].class)
			return verdict(Arrays.equals((long[])expected, (long[])actual));
		if (outputClass == double[].class)
			return verdict(Arrays.equals((double[])expected, (double[])actual));
		if (outputClass == String[].class)
			return verdict(Arrays.deepEquals((String[]) expected, (String[]) actual));
		if (outputClass == double.class) {
			double expectedValue = (Double) expected;
			double actualValue = (Double) actual;
			return verdict(Math.abs(expectedValue - actualValue) <= 1e-9 * Math.max(Math.abs(expectedValue), 1));
		}
		return verdict(expected.equals(actual));
	}

	private static String verdict(boolean equals) {
		if (equals)
			return null;
		return "Wrong answer";
	}

	private static Object run(Class taskClass, MethodSignature signature, TopCoderTest test)
		throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException
	{
		Object solver = taskClass.getConstructor().newInstance();
		Method solve = taskClass.getMethod(signature.name, signature.arguments);
		return solve.invoke(solver, signature.generateArguments(test));
	}

	private static TopCoderTest[] decode(String s, MethodSignature methodSignature) {
		if ("empty".equals(s))
			return new TopCoderTest[0];
		String[] tokens = s.split("::", -1);
		TopCoderTest[] tests = new TopCoderTest[tokens.length];
		for (int i = 0; i < tests.length; i++)
			tests[i] = EncodingUtilities.decodeTopCoderTest(i, tokens[i], methodSignature.arguments.length);
		return tests;
	}
}
