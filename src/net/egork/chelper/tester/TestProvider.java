package net.egork.chelper.tester;

import net.egork.chelper.task.Test;

import java.util.Collection;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public interface TestProvider {
    public Collection<Test> createTests();
}
