package net.egork.chelper.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.egork.chelper.util.InputReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class NewTopCoderTest {
    public final String[] arguments;
    public final String result;
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
        this(convert(arguments), convert(result), index, active);
    }

    public static String convert(Object result) {
        if (result == null) {
            return null;
        }
        if (result instanceof Integer) {
            return Integer.toString((Integer) result);
        }
        if (result instanceof Long) {
            return Long.toString((Long) result);
        }
        if (result instanceof Double) {
            return Double.toString((Double) result);
        }
        if (result instanceof String) {
            return (String) result;
        }
        if (result instanceof int[]) {
            StringBuilder builder = new StringBuilder();
            for (int i : (int[])result) {
                builder.append(i).append(' ');
            }
            return removeLast(builder);
        }
        if (result instanceof long[]) {
            StringBuilder builder = new StringBuilder();
            for (long i : (long[])result) {
                builder.append(i).append(' ');
            }
            return removeLast(builder);
        }
        if (result instanceof double[]) {
            StringBuilder builder = new StringBuilder();
            for (double i : (double[])result) {
                builder.append(i).append(' ');
            }
            return removeLast(builder);
        }
        if (result instanceof String[]) {
            StringBuilder builder = new StringBuilder();
            for (String i : (String[])result) {
                builder.append(i).append('ф');
            }
            return removeLast(builder);
        }
        return null;
    }

    @NotNull
    private static String removeLast(StringBuilder builder) {
        if (builder.length() > 0) {
            return builder.toString().substring(0, builder.length() - 1);
        }
        return "";
    }

    public Object[] getArguments(MethodSignature signature) {
        Object[] result = new Object[arguments.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = convert(arguments[i], signature.arguments[i]);
        }
        return result;
    }

    public Object getResult(MethodSignature signature) {
        return convert(result, signature.result);
    }

    private static Object convert(String value, String aClass) {
        if (value == null) {
            return null;
        }
        switch (aClass) {
            case "int":
                return Integer.parseInt(value);
            case "long":
                return Long.parseLong(value);
            case "double":
                return Double.parseDouble(value);
            case "String":
                return value;
            case "int[]":
                if (value.isEmpty()) {
                    return new int[0];
                }
                String[] tokens = value.split(" ", -1);
                int[] result = new int[tokens.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = Integer.parseInt(tokens[i]);
                }
                return result;
            case "long[]":
                if (value.isEmpty()) {
                    return new long[0];
                }
                tokens = value.split(" ", -1);
                long[] resultLong = new long[tokens.length];
                for (int i = 0; i < resultLong.length; i++) {
                    resultLong[i] = Long.parseLong(tokens[i]);
                }
                return resultLong;
            case "double[]":
                if (value.isEmpty()) {
                    return new double[0];
                }
                tokens = value.split(" ", -1);
                double[] resultDouble = new double[tokens.length];
                for (int i = 0; i < resultDouble.length; i++) {
                    resultDouble[i] = Double.parseDouble(tokens[i]);
                }
                return resultDouble;
            case "String[]":
                return value.split("ф", -1);
            default:
                return null;
        }
    }

    public static String[] convert(Object[] arguments) {
        String[] result = new String[arguments.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = convert(arguments[i]);
        }
        return result;
    }

    @JsonCreator
    public NewTopCoderTest(@JsonProperty("arguments") String[] arguments,
                           @JsonProperty("result") String result,
                           @JsonProperty("index") int index,
                           @JsonProperty("active") boolean active) {
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
            return toString(value);
        } else {
            if (value.length() >= 2 && value.charAt(0) == '{' && value.charAt(value.length() - 1) == '}') {
                value = value.substring(1, value.length() - 1);
            }
            if (aClass == String[].class) {
                String[] tokens = mySplit(value);
                String[] result = new String[tokens.length];
                for (int j = 0; j < tokens.length; j++) {
                    if ((result[j] = toString(tokens[j])) == null) {
                        return null;
                    }
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
        if (s.length() == 0) {
            return new String[0];
        }
        List<String> list = new ArrayList<String>();
        int quoteCount = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '"') {
                quoteCount++;
            }
            if (s.charAt(i) == ',' && quoteCount % 2 == 0) {
                list.add(s.substring(start, i));
                start = i + 1;
            }
        }
        list.add(s.substring(start));
        return list.toArray(new String[list.size()]);
    }

    private static String toString(String argument) {
        String trimmed = argument.trim();
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"') {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (String argument : arguments) {
            if (builder.length() != 0) {
                builder.append(" ");
            }
            if (argument != null) {
                builder.append(argument.replace('ф', ' '));
            }
        }
        String representation = builder.toString();
        if (representation.length() > 15) {
            representation = representation.substring(0, 12) + "...";
        }
        return "Test #" + index + ": " + representation;
    }

    public NewTopCoderTest setIndex(int index) {
        return new NewTopCoderTest(arguments, result, index, active);
    }

    public NewTopCoderTest setActive(boolean active) {
        return new NewTopCoderTest(arguments, result, index, active);
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

    public static String toString(String val, Class aClass) {
        if (val == null) {
            return "null";
        }
        Object value = convert(val, MethodSignature.getName(aClass));
        return toString(value, aClass);
    }

    @Nullable
    public static String toString(Object value, Class aClass) {
        if (String.class.equals(aClass)) {
            return '"' + value.toString() + '"';
        }
        if (!aClass.isArray()) {
            return value.toString();
        }
        if (int[].class.equals(aClass)) {
            int[] array = (int[]) value;
            StringBuilder result = new StringBuilder();
            result.append('{');
            for (int i : array) {
                if (result.length() != 1) {
                    result.append(',');
                }
                result.append(i);
            }
            result.append('}');
            return result.toString();
        }
        if (long[].class.equals(aClass)) {
            long[] array = (long[]) value;
            StringBuilder result = new StringBuilder();
            result.append('{');
            for (long i : array) {
                if (result.length() != 1) {
                    result.append(',');
                }
                result.append(i);
            }
            result.append('}');
            return result.toString();
        }
        if (double[].class.equals(aClass)) {
            double[] array = (double[]) value;
            StringBuilder result = new StringBuilder();
            result.append('{');
            for (double i : array) {
                if (result.length() != 1) {
                    result.append(',');
                }
                result.append(i);
            }
            result.append('}');
            return result.toString();
        }
        if (String[].class.equals(aClass)) {
            String[] array = (String[]) value;
            StringBuilder result = new StringBuilder();
            result.append('{');
            for (String i : array) {
                if (result.length() != 1) {
                    result.append(',');
                }
                result.append('"').append(i).append('"');
            }
            result.append('}');
            return result.toString();
        }
        return null;
    }
}
