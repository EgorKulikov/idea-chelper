package net.egork.chelper.task;

import net.egork.chelper.util.InputReader;
import net.egork.chelper.util.OutputWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class NewTopCoderTest {
    public final Object[] arguments;
    public final Object result;
    public final int index;
    public final boolean active;

    public NewTopCoderTest(Object[] arguments) {
        this(arguments, null);
    }

    public NewTopCoderTest(Object[] arguments, Object result) {
        this(arguments, result, -1);
    }

    public NewTopCoderTest(Object[] arguments, Object result, int index) {
        this(arguments, result, index, true);
    }

    public NewTopCoderTest(Object[] arguments, Object result, int index, boolean active) {
        this.arguments = arguments;
        this.result = result;
        this.index = index;
        this.active = active;
    }

    public static Object parse(String value, Class aClass) {
        value = value.trim();
        if (aClass == int.class) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (aClass == long.class) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (aClass == double.class) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (aClass == String.class) {
            return getString(value);
        } else {
            if (value.length() >= 2 && value.charAt(0) == '{' && value.charAt(value.length() - 1) == '}')
                value = value.substring(1, value.length() - 1);
            if (aClass == String[].class) {
                String[] tokens = mySplit(value);
                String[] result = new String[tokens.length];
                for (int j = 0; j < tokens.length; j++) {
                    if ((result[j] = getString(tokens[j])) == null)
                        return null;
                }
                return result;
            } else {
                try {
                    String[] tokens = value.trim().length() == 0 ? new String[0] : value.trim().split(",");
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
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String[] mySplit(String s) {
        s = s.trim();
        if (s.length() == 0)
            return new String[0];
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
        String trimmed = argument.trim();
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"')
            return trimmed.substring(1, trimmed.length() - 1);
        return trimmed;
    }

    public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Object argument : arguments) {
			if (argument instanceof String)
				builder.append(toString(argument, String.class));
			else if (argument instanceof Integer)
				builder.append(toString(argument, int.class));
			else if (argument instanceof Long)
				builder.append(toString(argument, long.class));
			else if (argument instanceof Double)
				builder.append(toString(argument, double.class));
			else if (argument instanceof String[])
				builder.append(toString(argument, String[].class));
			else if (argument instanceof int[])
				builder.append(toString(argument, int[].class));
			else if (argument instanceof long[])
				builder.append(toString(argument, long[].class));
			else
				builder.append(toString(argument, double[].class));
		}
        String representation = builder.toString();
        if (representation.length() > 15)
            representation = representation.substring(0, 12) + "...";
        return "Test #" + index + ": " + representation;
    }

    public NewTopCoderTest setIndex(int index) {
        return new NewTopCoderTest(arguments, result, index, active);
    }

    public NewTopCoderTest setActive(boolean active) {
        return new NewTopCoderTest(arguments, result, index, active);
    }

    public void saveTest(OutputWriter out) {
        out.printLine(index);
        out.printLine(arguments.length);
        for (Object argument : arguments)
            out.printTopCoder(argument);
        out.printTopCoder(result);
        out.printBoolean(active);
    }

    public static NewTopCoderTest loadTest(InputReader in) {
        int index = in.readInt();
        int argumentCount = in.readInt();
        Object[] arguments = new Object[argumentCount];
        for (int i = 0; i < argumentCount; i++)
            arguments[i] = in.readTopCoder();
        Object result = in.readTopCoder();
        boolean active = in.readBoolean();
        return new NewTopCoderTest(arguments, result, index, active);
    }

    public static String toString(Object value, Class aClass) {
		if (value == null)
			return "null";
        if (String.class.equals(aClass))
            return '"' + value.toString() + '"';
        if (!aClass.isArray())
            return value.toString();
        if (int[].class.equals(aClass)) {
            int[] array = (int[])value;
            StringBuilder result = new StringBuilder();
            result.append('{');
            for (int i : array) {
                if (result.length() != 1)
                    result.append(',');
                result.append(i);
            }
            result.append('}');
            return result.toString();
        }
        if (long[].class.equals(aClass)) {
            long[] array = (long[])value;
            StringBuilder result = new StringBuilder();
            result.append('{');
            for (long i : array) {
                if (result.length() != 1)
                    result.append(',');
                result.append(i);
            }
            result.append('}');
            return result.toString();
        }
        if (double[].class.equals(aClass)) {
            double[] array = (double[])value;
            StringBuilder result = new StringBuilder();
            result.append('{');
            for (double i : array) {
                if (result.length() != 1)
                    result.append(',');
                result.append(i);
            }
            result.append('}');
            return result.toString();
        }
        if (String[].class.equals(aClass)) {
            String[] array = (String[])value;
            StringBuilder result = new StringBuilder();
            result.append('{');
            for (String i : array) {
                if (result.length() != 1)
                    result.append(',');
                result.append('"').append(i).append('"');
            }
            result.append('}');
            return result.toString();
        }
        return null;
    }
}
