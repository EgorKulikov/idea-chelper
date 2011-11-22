package net.egork.chelper.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import net.egork.chelper.parser.TaskParser;
import net.egork.chelper.parser.codechef.CodeChefTaskParser;
import net.egork.chelper.parser.codeforces.CodeforcesTaskParser;
import net.egork.chelper.parser.eolimp.EOlimpTaskParser;
import net.egork.chelper.parser.timus.TimusTaskParser;
import net.egork.chelper.task.Task;
import net.egork.chelper.ui.ParseDialog;
import net.egork.chelper.util.Utilities;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class ParseTaskAction extends AnAction {
	public static final TaskParser[] PARSERS = {CodeforcesTaskParser.INSTANCE, CodeChefTaskParser.INSTANCE,
		EOlimpTaskParser.INSTANCE, TimusTaskParser.INSTANCE};

	public void actionPerformed(AnActionEvent e) {
		if (!Utilities.isEligible(e.getDataContext()))
			return;
		Project project = Utilities.getProject(e.getDataContext());
		Task task = ParseDialog.parseTask(project);
		if (task == null)
			return;
		PsiElement element = task.initialize();
		Utilities.createConfiguration(task, true);
		Utilities.openElement(project, element);
	}
}
