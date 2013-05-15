package net.egork.chelper.checkers;

import net.egork.chelper.tester.Verdict;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public interface Checker {
    public Verdict check(String input, String expectedOutput, String actualOutput);
}
