package net.egork.chelper.parser;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class Description {
    public final String id;
    public final String description;

    public Description(String id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
