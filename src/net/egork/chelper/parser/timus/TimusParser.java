package net.egork.chelper.parser.timus;

import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.parser.Parser;
import net.egork.chelper.parser.TaskOptions;

import javax.swing.*;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class TimusParser implements Parser {
	public TaskOptions getOptions() {
		return TimusTaskOptions.INSTANCE;
	}

	public Icon getIcon() {
		return IconLoader.getIcon("/icons/timus.png");
	}

	public String getName() {
		return "Timus";
	}
}
