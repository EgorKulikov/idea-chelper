package net.egork.chelper.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import net.egork.chelper.parser.ContestParser;
import net.egork.chelper.parser.codechef.CodeChefContestParser;
import net.egork.chelper.parser.codeforces.CodeforcesContestParser;
import net.egork.chelper.parser.eolimp.EOlimpContestParser;
import net.egork.chelper.parser.timus.TimusContestParser;
import net.egork.chelper.task.Task;
import net.egork.chelper.ui.ParseDialog;
import net.egork.chelper.util.Utilities;

import java.util.Collection;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class ParseContestAction extends AnAction {
	public static final ContestParser[] PARSERS = {CodeforcesContestParser.INSTANCE, CodeChefContestParser.INSTANCE,
		EOlimpContestParser.INSTANCE, TimusContestParser.INSTANCE};

	public void actionPerformed(AnActionEvent e) {
		if (!Utilities.isEligible(e.getDataContext()))
			return;
		Project project = Utilities.getProject(e.getDataContext());
		Collection<Task> tasks = ParseDialog.parseContest(project);
		boolean firstConfiguration = true;
		PsiElement firstElement = null;
		for (Task task : tasks) {
			PsiElement element = task.initialize();
			Utilities.createConfiguration(task, firstConfiguration);
			firstConfiguration = false;
			if (firstElement == null)
				firstElement = element;
		}
		if (firstElement != null)
			Utilities.openElement(project, firstElement);
	}

}
