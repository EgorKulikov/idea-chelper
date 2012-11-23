package net.egork.chelper.util;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public interface Provider<T> {
    public T provide();
}
