package net.egork.chelper.util;

import net.egork.chelper.task.Test;
import net.egork.chelper.task.TopCoderTest;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class EncodingUtilities {
    public static final String TOKEN_SEPARATOR = "::";
    public static final String TEST_SEPARATOR = ";;";

    public static String encode(String s) {
        return s.replace(":", "/:").replace(";", "/;").replace("_", "/_").replace("\n", "/__").replace("\r", "");
    }

    public static String decode(String s) {
        return s.replace("/__", "\n").replace("/_", "_").replace("/:", ":").replace("/;", ";");
    }

    public static String encodeTests(Test[] tests) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Test test : tests) {
            if (first) {
                first = false;
            } else {
                builder.append(TOKEN_SEPARATOR);
            }
            builder.append(encode(test));
        }
        if (builder.length() == 0) {
            return "empty";
        }
        return builder.toString();
    }

    public static String encode(Test test) {
        return encode(test.input) + TEST_SEPARATOR + encode(test.output) + TEST_SEPARATOR + Boolean.toString(
                test.active);
    }

    public static Test decodeTest(int index, String test) {
        String[] tokens = test.split(TEST_SEPARATOR, -1);
        return new Test(decode(tokens[0]), decode(tokens[1]), index, tokens.length == 2 || Boolean.valueOf(tokens[2]));
    }

    public static TopCoderTest decodeTopCoderTest(int index, String s, int argumentCount) {
        String[] tokens = s.split(TEST_SEPARATOR, -1);
        String[] arguments = new String[argumentCount];
        for (int i = 0; i < arguments.length; i++)
            arguments[i] = decode(tokens[i]);
        return new TopCoderTest(arguments, decode(tokens[argumentCount]), index, tokens.length == argumentCount + 1
                || Boolean.parseBoolean(tokens[argumentCount + 1]));
    }

    public static String encode(TopCoderTest test) {
        StringBuilder builder = new StringBuilder();
        for (String argument : test.arguments)
            builder.append(encode(argument)).append(TEST_SEPARATOR);
        builder.append(encode(test.result)).append(TEST_SEPARATOR).append(test.active);
        return builder.toString();
    }

    public static String encodeTests(TopCoderTest[] tests) {
        if (tests.length == 0) {
            return "empty";
        }
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (TopCoderTest test : tests) {
            if (first) {
                first = false;
            } else {
                builder.append(TOKEN_SEPARATOR);
            }
            builder.append(encode(test));
        }
        return builder.toString();
    }
}
