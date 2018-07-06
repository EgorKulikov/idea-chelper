package net.egork.chelper.tester;

/**
 * @author egor@egork.net
 */
public class State<T> {
    private volatile T value;

    public State(T value) {
        this.value = value;
    }

    public void setState(T value) {
        this.value = value;
    }

    public T getState() {
        return value;
    }
}
