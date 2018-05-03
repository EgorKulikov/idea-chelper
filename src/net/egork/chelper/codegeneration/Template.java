package net.egork.chelper.codegeneration;

/**
 * @author egor@egork.net
 */
public class Template {
    protected final String template;

    public Template(String template) {
        this.template = template;
    }

    public String apply(String... replacement) {
        if (replacement.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        String result = template;
        for (int i = 0; i < replacement.length; i += 2) {
            result = result.replace("%" + replacement[i] + "%", replacement[i + 1]);
        }
        return result;
    }
}
