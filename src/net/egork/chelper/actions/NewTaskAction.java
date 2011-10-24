package net.egork.chelper.actions;

import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.ide.actions.CreateElementActionBase;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TaskConfigurationType;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Utilities;
import net.egork.chelper.task.Task;
import net.egork.chelper.ui.CreateTaskDialog;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class NewTaskAction extends CreateElementActionBase {
	@NotNull
	@Override
	protected PsiElement[] invokeDialog(Project project, PsiDirectory psiDirectory) {
		return create(null, psiDirectory);
	}

	@Override
	protected void checkBeforeCreate(String s, PsiDirectory psiDirectory) throws IncorrectOperationException {
	}

	@NotNull
	@Override
	protected PsiElement[] create(String s, PsiDirectory psiDirectory) {
		return createTask(s, psiDirectory);
	}

	public static PsiElement[] createTask(String s, PsiDirectory psiDirectory) {
		if (!FileUtilities.isJavaDirectory(psiDirectory))
			return PsiElement.EMPTY_ARRAY;
		Task task = CreateTaskDialog.showDialog(psiDirectory, s);
		if (task == null)
			return PsiElement.EMPTY_ARRAY;
		PsiElement main = task.initialize();
		if (main == null)
			return PsiElement.EMPTY_ARRAY;
		RunManagerImpl manager = RunManagerImpl.getInstanceImpl(main.getProject());
		RunnerAndConfigurationSettingsImpl configuration = new RunnerAndConfigurationSettingsImpl(manager,
			new TaskConfiguration(task.name, main.getProject(), task,
			TaskConfigurationType.INSTANCE.getConfigurationFactories()[0]), false);
		manager.addConfiguration(configuration, false);
		manager.setActiveConfiguration(configuration);
		return new PsiElement[]{main};
	}

	@Override
	protected String getErrorTitle() {
		return "Error";
	}

	@Override
	protected String getCommandName() {
		return "Task";
	}

	@Override
	protected String getActionName(PsiDirectory psiDirectory, String s) {
		return "New task " + s;
	}

	@Override
	protected boolean isAvailable(DataContext dataContext) {
		if (!Utilities.isEligible(dataContext))
			return false;
		PsiDirectory directory = FileUtilities.getDirectory(dataContext);
		return FileUtilities.isJavaDirectory(directory);
	}
}
