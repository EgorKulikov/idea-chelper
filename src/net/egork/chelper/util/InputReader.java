package net.egork.chelper.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.InputMismatchException;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class InputReader extends InputStream {
    private InputStream stream;
    private byte[] buf = new byte[1024];
    private int curChar;
    private int numChars;

    public InputReader(InputStream stream) {
        this.stream = stream;
    }

    public int read() {
        if (numChars == -1) {
            throw new InputMismatchException();
        }
        if (curChar >= numChars) {
            curChar = 0;
            try {
                numChars = stream.read(buf);
            } catch (IOException e) {
                throw new InputMismatchException();
            }
            if (numChars <= 0) {
                return -1;
            }
        }
        return buf[curChar++];
    }

    public int peek() {
        if (numChars == -1) {
            return -1;
        }
        if (curChar >= numChars) {
            curChar = 0;
            try {
                numChars = stream.read(buf);
            } catch (IOException e) {
                return -1;
            }
            if (numChars <= 0) {
                return -1;
            }
        }
        return buf[curChar];
    }

    public int readInt() {
        int c = read();
        while (isSpaceChar(c))
            c = read();
        int sgn = 1;
        if (c == '-') {
            sgn = -1;
            c = read();
        }
        int res = 0;
        do {
            if (c < '0' || c > '9') {
                throw new InputMismatchException();
            }
            res *= 10;
            res += c - '0';
            c = read();
        } while (!isSpaceChar(c));
        return res * sgn;
    }

    public long readLong() {
        int c = read();
        while (isSpaceChar(c))
            c = read();
        int sgn = 1;
        if (c == '-') {
            sgn = -1;
            c = read();
        }
        long res = 0;
        do {
            if (c < '0' || c > '9') {
                throw new InputMismatchException();
            }
            res *= 10;
            res += c - '0';
            c = read();
        } while (!isSpaceChar(c));
        return res * sgn;
    }

    public String readString() {
        int length = readInt();
        if (length < 0) {
            return null;
        }
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++)
            bytes[i] = (byte) read();
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return new String(bytes);
        }
    }

    public String readToken() {
        int c;
        while (isSpaceChar(c = read())) ;
        StringBuilder result = new StringBuilder();
        result.appendCodePoint(c);
        while (!isSpaceChar(c = read()))
            result.appendCodePoint(c);
        return result.toString();
    }

    public static boolean isSpaceChar(int c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == -1;
    }

    public char readCharacter() {
        int c = read();
        while (isSpaceChar(c))
            c = read();
        return (char) c;
    }

    public double readDouble() {
        int c = read();
        while (isSpaceChar(c))
            c = read();
        int sgn = 1;
        if (c == '-') {
            sgn = -1;
            c = read();
        }
        double res = 0;
        while (!isSpaceChar(c) && c != '.') {
            if (c == 'e' || c == 'E') {
                return res * Math.pow(10, readInt());
            }
            if (c < '0' || c > '9') {
                throw new InputMismatchException();
            }
            res *= 10;
            res += c - '0';
            c = read();
        }
        if (c == '.') {
            c = read();
            double m = 1;
            while (!isSpaceChar(c)) {
                if (c == 'e' || c == 'E') {
                    return res * Math.pow(10, readInt());
                }
                if (c < '0' || c > '9') {
                    throw new InputMismatchException();
                }
                m /= 10;
                res += (c - '0') * m;
                c = read();
            }
        }
        return res * sgn;
    }

    public boolean isExhausted() {
        int value;
        while (isSpaceChar(value = peek()) && value != -1)
            read();
        return value == -1;
    }

    public boolean readBoolean() {
        return readInt() == 1;
    }

    public <E extends Enum<E>> E readEnum(Class<E> c) {
        String name = readString();
        if (name == null) {
            return null;
        }
        for (E e : c.getEnumConstants()) {
            if (e.name().equals(name)) {
                return e;
            }
        }
        throw new EnumConstantNotPresentException(c, name);
    }

    public Object readTopCoder() {
        String type = readToken();
        if (type.equals("-1")) {
            return null;
        }
        if ("int".equals(type)) {
            return readInt();
        } else if ("long".equals(type)) {
            return readLong();
        } else if ("double".equals(type)) {
            return readDouble();
        } else if ("String".equals(type)) {
            return readString();
        } else if ("int[]".equals(type)) {
            int length = readInt();
            int[] result = new int[length];
            for (int i = 0; i < length; i++)
                result[i] = readInt();
            return result;
        } else if ("long[]".equals(type)) {
            int length = readInt();
            long[] result = new long[length];
            for (int i = 0; i < length; i++)
                result[i] = readLong();
            return result;
        } else if ("double[]".equals(type)) {
            int length = readInt();
            double[] result = new double[length];
            for (int i = 0; i < length; i++)
                result[i] = readDouble();
            return result;
        } else if ("String[]".equals(type)) {
            int length = readInt();
            String[] result = new String[length];
            for (int i = 0; i < length; i++)
                result[i] = readString();
            return result;
        }
        throw new InputMismatchException();
    }

}