package net.egork.chelper.task;

import java.util.Arrays;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderTest {
    public final String[] arguments;
    public final String result;
    public final int index;
    public final boolean active;

    public TopCoderTest(String[] arguments, String result, int index) {
        this(arguments, result, index, true);
    }

    public TopCoderTest(String[] arguments, String result, int index, boolean active) {
        this.arguments = arguments;
        this.result = result;
        this.index = index;
        this.active = active;
    }

    public String toString() {
        String representation = Arrays.toString(arguments);
        if (representation.length() > 15) {
            representation = representation.substring(0, 12) + "...";
        }
        return "Test #" + index + ": " + representation;
    }

    public TopCoderTest setIndex(int index) {
        return new TopCoderTest(arguments, result, index, active);
    }

    public TopCoderTest setActive(boolean active) {
        return new TopCoderTest(arguments, result, index, active);
    }
}
