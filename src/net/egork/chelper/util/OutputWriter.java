package net.egork.chelper.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class OutputWriter {
    private final OutputStream out;

    public OutputWriter(OutputStream outputStream) {
        out = outputStream;
    }

    public void print(Object...objects) {
        try {
            for (int i = 0; i < objects.length; i++) {
                if (i != 0)
                    out.write(' ');
                out.write(objects[i].toString().getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void printLine(Object...objects) {
        print(objects);
        try {
            out.write('\n');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void flush() {
        try {
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void printString(String s) {
        if (s == null)
            printLine(-1);
        else
            printLine(s.length(), s);
    }

    public void printBoolean(boolean b) {
        printLine(b ? 1 : 0);
    }

    public void printEnum(Enum e) {
        printString(e == null ? null : e.name());
    }
}
