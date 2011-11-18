package net.egork.chelper.actions;

import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TaskConfigurationType;
import net.egork.chelper.parser.dummy.DummyTaskParser;
import net.egork.chelper.parser.codeforces.CodeforcesTaskParser;
import net.egork.chelper.parser.TaskParser;
import net.egork.chelper.task.Task;
import net.egork.chelper.ui.ParseDialog;
import net.egork.chelper.util.Utilities;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class ParseTaskAction extends AnAction {
	public static final TaskParser[] PARSERS = {CodeforcesTaskParser.INSTANCE};

	public void actionPerformed(AnActionEvent e) {
		if (!Utilities.isEligible(e.getDataContext()))
			return;
		Project project = Utilities.getProject(e.getDataContext());
		RunManagerImpl manager = RunManagerImpl.getInstanceImpl(project);
		Task task = ParseDialog.parseTask(project);
		if (task == null)
			return;
		PsiElement element = task.initialize();
		RunnerAndConfigurationSettingsImpl configuration = new RunnerAndConfigurationSettingsImpl(manager,
			new TaskConfiguration(task.name, project, task,
			TaskConfigurationType.INSTANCE.getConfigurationFactories()[0]), false);
		manager.addConfiguration(configuration, false);
		Utilities.openElement(project, element);
		manager.setActiveConfiguration(configuration);
	}
}
