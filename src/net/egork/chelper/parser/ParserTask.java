package net.egork.chelper.parser;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class ParserTask {
    public ParserTask(final String id, final DescriptionReceiver receiver, final Parser parser) {
        new Thread(new Runnable() {
            public void run() {
                if (id == null) {
                    parser.getContests(receiver);
                } else {
                    parser.parseContest(id, receiver);
                }
            }
        }).start();
    }
}
