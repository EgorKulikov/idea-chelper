package net.egork.chelper.checkers;

import net.egork.chelper.tester.StringInputStream;
import net.egork.chelper.tester.Verdict;
import net.egork.chelper.util.InputReader;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class TokenChecker implements Checker {
    private double certainty;
    private boolean allowAbsolute;
    private boolean allowRelative;

    public TokenChecker(String parameters) {
        if (parameters.length() != 0) {
            String[] tokens = parameters.split(" ");
            if (tokens.length == 1) {
                certainty = Double.parseDouble(tokens[0]);
                allowAbsolute = allowRelative = true;
            } else {
                if (tokens[0].toLowerCase().indexOf('a') != -1) {
                    allowAbsolute = true;
                }
                if (tokens[0].toLowerCase().indexOf('r') != -1) {
                    allowRelative = true;
                }
                certainty = Double.parseDouble(tokens[1]);
            }
        }
    }

    public Verdict check(String input, String expectedOutput, String actualOutput) {
        if (expectedOutput == null) {
            return Verdict.UNDECIDED;
        }
        InputReader expected = new InputReader(new StringInputStream(expectedOutput));
        InputReader actual = new InputReader(new StringInputStream(actualOutput));
        int count = -1;
        double maxDelta = 0;
        while (true) {
            count++;
            if (expected.isExhausted()) {
                if (actual.isExhausted()) {
                    if (allowRelative || allowAbsolute) {
                        return new Verdict(Verdict.VerdictType.OK, "Maximal delta is " + maxDelta);
                    }
                    return Verdict.OK;
                }
                return new Verdict(Verdict.VerdictType.PE, "Only " + count + " tokens expected");
            }
            if (actual.isExhausted()) {
                return new Verdict(Verdict.VerdictType.PE, "More than " + count + " tokens expected");
            }
            String expectedToken = expected.readToken();
            String actualToken = actual.readToken();
            if (!expectedToken.equals(actualToken)) {
                if (allowAbsolute || allowRelative) {
                    try {
                        double expectedValue = Double.parseDouble(expectedToken);
                        double actualValue = Double.parseDouble(actualToken);
                        double absoluteDiff = Math.abs(expectedValue - actualValue);
                        double relativeDiff = absoluteDiff / (expectedValue == 0 && absoluteDiff == 0 ? 1 : Math.abs(expectedValue));
                        double diff = Double.POSITIVE_INFINITY;
                        if (allowAbsolute) {
                            diff = Math.min(diff, absoluteDiff);
                        }
                        if (allowRelative) {
                            diff = Math.min(diff, relativeDiff);
                        }
                        maxDelta = Math.max(maxDelta, diff);
                        if (diff <= certainty) {
                            continue;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                return new Verdict(Verdict.VerdictType.WA, "Difference in token #" + count);
            }
        }
    }
}
