package net.egork.chelper.topcoder;

import net.egork.chelper.util.InputReader;
import net.egork.chelper.util.OutputWriter;

import java.io.IOException;
import java.net.Socket;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class Message {
    public static final String PORT_PROPERTY = "net.egork.chelper.port";
    public static final String NEW_TASK = "New task";
    public static final String GET_SOURCE = "Get source";
    public static final String OK = "OK";
    public static final String ALREADY_DEFINED = "Already defined";
    public static final String OTHER_ERROR = "Error";
    private final Socket socket;
    public final InputReader in;
    public final OutputWriter out;

    public Message(Socket socket) throws IOException {
        this.socket = socket;
        in = new InputReader(socket.getInputStream());
        out = new OutputWriter(socket.getOutputStream());
    }

    public Message(int port) throws IOException {
        this(new Socket("localhost", port));
    }
}
