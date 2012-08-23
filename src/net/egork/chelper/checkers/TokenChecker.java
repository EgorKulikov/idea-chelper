package net.egork.chelper.checkers;

import net.egork.chelper.tester.StringInputStream;
import net.egork.chelper.tester.Verdict;
import net.egork.chelper.util.InputReader;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class TokenChecker implements Checker {
    public TokenChecker(String parameters) {
    }

    public Verdict check(String input, String expectedOutput, String actualOutput) {
        if (expectedOutput == null)
            return Verdict.UNDECIDED;
        InputReader expected = new InputReader(new StringInputStream(expectedOutput));
        InputReader actual = new InputReader(new StringInputStream(actualOutput));
        int count = 0;
        while (true) {
            if (expected.isExhausted()) {
                if (actual.isExhausted())
                    return Verdict.OK;
                return new Verdict(Verdict.VerdictType.PE, "Only " + count + " tokens expected");
            }
            if (actual.isExhausted())
                return new Verdict(Verdict.VerdictType.PE, "More than " + count + " tokens expected");
            if (!expected.readToken().equals(actual.readToken()))
                return new Verdict(Verdict.VerdictType.WA, "Difference in token #" + count);
            count++;
        }
    }
}
