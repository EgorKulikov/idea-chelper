package net.egork.chelper.task;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public enum TestType {
    SINGLE("Single test"),
    MULTI_NUMBER("Number of tests known"),
    MULTI_EOF("Number of tests unknown");

    private final String uiDescription;

    private TestType(String uiDescription) {
        this.uiDescription = uiDescription;
    }


    @Override
    public String toString() {
        return uiDescription;
    }
}
