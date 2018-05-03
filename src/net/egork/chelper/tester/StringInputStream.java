package net.egork.chelper.tester;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class StringInputStream extends InputStream {
    private final String s;
    private int index = 0;

    public StringInputStream(String s) {
        this.s = s;
    }

    @Override
    public int read() throws IOException {
        if (index < s.length()) {
            return s.charAt(index++);
        }
        return -1;
    }
}
