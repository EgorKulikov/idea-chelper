package net.egork.chelper.parser.codeforces;

import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.parser.Parser;
import net.egork.chelper.parser.TaskOptions;

import javax.swing.Icon;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CodeforcesParser implements Parser {
	public TaskOptions getOptions() {
		return CodeforcesTaskOptions.INSTANCE;
	}

	public Icon getIcon() {
		return IconLoader.getIcon("/icons/codeforces.png");
	}

	public String getName() {
		return "Codeforces";
	}
}
