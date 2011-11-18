package net.egork.chelper.parser;

import javax.swing.Icon;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public interface Parser {
	public TaskOptions getOptions();
	public Icon getIcon();
	public String getName();
}
