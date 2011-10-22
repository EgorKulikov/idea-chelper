package net.egork.chelper.tester;

import net.egork.chelper.task.MethodSignature;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TopCoderTest;

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
public class TopCoderTester {
	private static enum Verdict {
			OK, WA, RTE
		}

	public static void main(String[] args)
		throws InterruptedException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
		InstantiationException, IllegalAccessException
	{
		Locale.setDefault(Locale.US);
		List<Verdict> verdicts = new ArrayList<Verdict>();
		long maximalTime = 0;
		boolean ok = true;
		Set<Integer> testCases = new HashSet<Integer>();
		int argumentIndex = 0;
		String signature = args[argumentIndex++];
		MethodSignature methodSignature = MethodSignature.parse(signature);
		String fqn = args[argumentIndex++];
		List<TopCoderTest> tests = new ArrayList<TopCoderTest>();
		tests.addAll(Arrays.asList(decode(args[argumentIndex++])));
		for (int i = argumentIndex; i < args.length; i++)
			testCases.add(Integer.parseInt(args[i]));
		Class taskClass = Class.forName(fqn);
		for (TopCoderTest test : tests) {
			if (!testCases.isEmpty() && !testCases.contains(test.index))
				continue;
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
				System.out.println(actual);
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

	private static TopCoderTest[] decode(String s) {
		if ("empty".equals(s))
			return new TopCoderTest[0];
		String[] tokens = s.split("::", -1);
		TopCoderTest[] tests = new TopCoderTest[tokens.length];
		for (int i = 0; i < tests.length; i++)
			tests[i] = TopCoderTest.decode(i, tokens[i]);
		return tests;
	}
}
