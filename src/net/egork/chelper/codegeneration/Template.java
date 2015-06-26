package net.egork.chelper.codegeneration;

import java.util.Map;

/**
 * @author egor@egork.net
 */
public class Template {
	protected final String template;

	public Template(String template) {
		this.template = template;
	}

	public String apply(Map<String, String> replacement) {
		String result = template;
		for (Map.Entry<String, String> entry : replacement.entrySet()) {
			result = result.replace("%" + entry.getKey() + "%", entry.getValue());
		}
		return result;
	}
}
