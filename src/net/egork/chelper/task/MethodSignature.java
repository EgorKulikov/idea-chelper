package net.egork.chelper.task;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class MethodSignature {
	public final String name;
	public final Class result;
	public final Class[] arguments;
	public final String[] argumentNames;

	private MethodSignature(String name, Class result, Class[] arguments, String[] argumentNames) {
		this.name = name;
		this.result = result;
		this.arguments = arguments;
		this.argumentNames = argumentNames;
	}

	public static MethodSignature parse(String signature) {
		if (signature.startsWith("public "))
			signature = signature.substring(7);
		signature = signature.replace('(', ' ').replace(')', ' ').replace(',', ' ');
		String[] tokens = signature.split(" +");
		if (tokens.length % 2 == 1)
			return null;
		try {
			Class result = getClass(tokens[0]);
			String name = tokens[1];
			int argumentCount = (tokens.length - 2) / 2;
			Class[] arguments = new Class[argumentCount];
			String[] argumentNames = new String[argumentCount];
			for (int i = 0; i < argumentCount; i++) {
				arguments[i] = getClass(tokens[2 * i + 2]);
				argumentNames[i] = tokens[2 * i + 3];
			}
			return new MethodSignature(name, result, arguments, argumentNames);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	private static Class getClass(String name) throws ClassNotFoundException {
		if ("int".equals(name))
			return int.class;
		if ("long".equals(name))
			return long.class;
		if ("double".equals(name))
			return double.class;
		if ("String".equals(name))
			return String.class;
		if ("int[]".equals(name))
			return int[].class;
		if ("long[]".equals(name))
			return long[].class;
		if ("double[]".equals(name))
			return double[].class;
		if ("String[]".equals(name))
			return String[].class;
		throw new ClassNotFoundException();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(result.getSimpleName()).append(" ").append(name).append("(");
		for (int i = 0; i < arguments.length; i++) {
			if (i != 0)
				builder.append(", ");
			builder.append(arguments[i].getSimpleName()).append(" ").append(argumentNames[i]);
		}
		builder.append(")");
		return builder.toString();
	}

	public Object[] generateArguments(TopCoderTest test) {
		Object[] arguments = new Object[this.arguments.length];
		for (int i = 0; i < arguments.length; i++) {
			arguments[i] = resolve(this.arguments[i], test.arguments[i]);
		}
		return arguments;
	}

	public static Object resolve(Class aClass, String value) {
		value = value.trim();
		if (aClass == int.class)
			return Integer.parseInt(value);
		else if (aClass == long.class) {
			if (value.endsWith("L"))
				value = value.substring(0, value.length() - 1);
			return Long.parseLong(value);
		} else if (aClass == double.class)
			return Double.parseDouble(value);
		else if (aClass == String.class) {
			return getString(value);
		} else {
			if (value.charAt(0) == '{')
				value = value.substring(1, value.length() - 1);
			if (aClass == String[].class) {
				String[] tokens = mySplit(value);
				String[] result = new String[tokens.length];
				for (int j = 0; j < tokens.length; j++)
					result[j] = getString(tokens[j]);
				return result;
			} else {
				String[] tokens = value.trim().split(",");
				if (aClass == int[].class) {
					int[] result = new int[tokens.length];
					for (int j = 0; j < tokens.length; j++)
						result[j] = Integer.parseInt(tokens[j].trim());
					return result;
				} else if (aClass == long[].class) {
					long[] result = new long[tokens.length];
					for (int j = 0; j < tokens.length; j++)
						result[j] = Long.parseLong(tokens[j].trim());
					return result;
				} else if (aClass == double[].class) {
					double[] result = new double[tokens.length];
					for (int j = 0; j < tokens.length; j++)
						result[j] = Double.parseDouble(tokens[j].trim());
					return result;
				}
			}
		}
		return null;
	}

	private static String[] mySplit(String s) {
		List<String> list = new ArrayList<String>();
		int quoteCount = 0;
		int start = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '"')
				quoteCount++;
			if (s.charAt(i) == ',' && quoteCount % 2 == 0) {
				list.add(s.substring(start, i));
				start = i + 1;
			}
		}
		list.add(s.substring(start));
		return list.toArray(new String[list.size()]);
	}

	private static String getString(String argument) {
		argument = argument.trim();
		if (argument.length() > 0 && argument.charAt(0) == '"' && argument.charAt(argument.length() - 1) == '"')
			argument = argument.substring(1, argument.length() - 1);
		return argument;
	}
}
