package net.egork.chelper.checkers;

import net.egork.chelper.tester.Verdict;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class StrictChecker implements Checker {
    public StrictChecker(String parameters) {
    }

    public Verdict check(String input, String expectedOutput, String actualOutput) {
        if (expectedOutput == null) {
            return Verdict.UNDECIDED;
        }
        if (actualOutput.equals(expectedOutput)) {
            return Verdict.OK;
        }
        return Verdict.WA;
    }
}
