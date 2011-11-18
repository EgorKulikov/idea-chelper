package net.egork.chelper.parser.codechef;

import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.parser.Parser;
import net.egork.chelper.parser.TaskOptions;

import javax.swing.*;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class CodeChefParser implements Parser {
	public TaskOptions getOptions() {
		return CodeChefTaskOptions.INSTANCE;
	}

	public Icon getIcon() {
		return IconLoader.getIcon("/icons/codechef.png");
	}

	public String getName() {
		return "CodeChef";
	}
}
