package net.egork.chelper.tester;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author egor@egork.net
 */
public class Interactor {
    public Verdict interact(InputStream input, InputStream solutionOutput, OutputStream solutionInput,
            State<Boolean> state) {
        try {
            while (state.getState()) {
                while (System.in.available() > 0) {
                    solutionInput.write(System.in.read());
                }
                while (solutionOutput.available() > 0) {
                    solutionOutput.read();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return new Verdict(Verdict.VerdictType.RTE, e.getClass().getName());
        }
        return Verdict.UNDECIDED;
    }
}
