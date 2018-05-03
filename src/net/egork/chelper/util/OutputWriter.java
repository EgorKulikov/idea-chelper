package net.egork.chelper.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class OutputWriter {
    public final OutputStream out;

    public OutputWriter(OutputStream outputStream) {
        out = outputStream;
    }

    public void print(Object... objects) {
        try {
            for (int i = 0; i < objects.length; i++) {
                if (i != 0) {
                    out.write(' ');
                }
                out.write(objects[i].toString().getBytes("UTF-8"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void printLine(Object... objects) {
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
        if (s == null) {
            printLine(-1);
        } else {
            try {
                printLine(s.getBytes("UTF-8").length, s);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void printBoolean(boolean b) {
        printLine(b ? 1 : 0);
    }

    public void printEnum(Enum e) {
        printString(e == null ? null : e.name());
    }

    public void printTopCoder(Object o) {
        if (o == null) {
            printString(null);
        } else if (o instanceof Integer) {
            printLine("int", o);
        } else if (o instanceof Long) {
            printLine("long", o);
        } else if (o instanceof Double) {
            printLine("double", o);
        } else if (o instanceof String) {
            printLine("String");
            printString((String) o);
        } else if (o instanceof int[]) {
            printLine("int[]", ((int[]) o).length);
            for (int i : (int[]) o)
                printLine(i);
        } else if (o instanceof long[]) {
            printLine("long[]", ((long[]) o).length);
            for (long i : (long[]) o)
                printLine(i);
        } else if (o instanceof double[]) {
            printLine("double[]", ((double[]) o).length);
            for (double i : (double[]) o)
                printLine(i);
        } else if (o instanceof String[]) {
            printLine("String[]", ((String[]) o).length);
            for (String i : (String[]) o)
                printString(i);
        }
    }
}
