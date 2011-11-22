package net.egork.chelper.parser.eolimp;

import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.parser.Parser;
import net.egork.chelper.parser.TaskOptions;

import javax.swing.Icon;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class EOlimpParser implements Parser {
	public TaskOptions getOptions() {
		return EOlimpOptions.INSTANCE;
	}

	public Icon getIcon() {
		return IconLoader.getIcon("/icons/eolimp.png");
	}

	public String getName() {
		return "E-Olimp";
	}
}
