package net.egork.chelper.actions;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.util.CodeGenerationUtilities;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Utilities;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TopCoderTask;

import java.io.IOException;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class ArchiveAction extends AnAction {
	public void actionPerformed(AnActionEvent e) {
		if (!Utilities.isEligible(e.getDataContext()))
			return;
		final Project project = Utilities.getProject(e.getDataContext());
		final RunManagerImpl manager = RunManagerImpl.getInstanceImpl(project);
		RunnerAndConfigurationSettings selectedConfiguration =
			manager.getSelectedConfiguration();
		if (selectedConfiguration == null)
			return;
		RunConfiguration configuration = selectedConfiguration.getConfiguration();
		if (configuration instanceof TaskConfiguration) {
			String archiveDir = Utilities.getData(project).archive;
			final VirtualFile directory = FileUtilities.getFile(project, archiveDir);
			if (directory == null)
				return;
			final Task task = ((TaskConfiguration) configuration).getConfiguration();
			CodeGenerationUtilities.createUnitTest(task);
			ApplicationManager.getApplication().runWriteAction(new Runnable() {
				public void run() {
					try {
						VirtualFile mainFile = FileUtilities.getFile(project, task.location + "/" + task.name + ".java");
						if (mainFile == null)
							return;
						VfsUtil.copyFile(this, mainFile, directory);
						mainFile.delete(this);
						VirtualFile checkerFile = FileUtilities
							.getFile(project, task.location + "/" + task.name + "Checker.java");
						if (checkerFile == null)
							return;
						VfsUtil.copyFile(this, checkerFile, directory);
						checkerFile.delete(this);
						manager.removeConfiguration(manager.getSelectedConfiguration());
						setOtherConfiguration(manager);
					} catch (IOException ignored) {
					}
				}
			});
		}
		if (configuration instanceof TopCoderConfiguration) {
			String archiveDir = Utilities.getData(project).archive;
			final VirtualFile directory = FileUtilities.getFile(project, archiveDir);
			if (directory == null)
				return;
			final TopCoderTask task = ((TopCoderConfiguration) configuration).getConfiguration();
			ApplicationManager.getApplication().runWriteAction(new Runnable() {
				public void run() {
					try {
						VirtualFile mainFile = FileUtilities.getFile(project, Utilities.getData(project).defaultDir
							+ "/" + task.name + ".java");
						if (mainFile == null)
							return;
						VfsUtil.copyFile(this, mainFile, directory);
						mainFile.delete(this);
						VirtualFile topcoderFile = FileUtilities.getFile(project, Utilities.getData(project).topcoderDir
							+ "/" + task.name + ".java");
						if (topcoderFile != null)
							topcoderFile.delete(this);
						manager.removeConfiguration(manager.getSelectedConfiguration());
						setOtherConfiguration(manager);
					} catch (IOException ignored) {
					}
				}
			});
		}
	}

	public static void setOtherConfiguration(RunManagerImpl manager) {
		RunConfiguration[] allConfigurations = manager.getAllConfigurations();
		for (RunConfiguration configuration : allConfigurations) {
			if (configuration instanceof TaskConfiguration || configuration instanceof TopCoderConfiguration)
				manager.setActiveConfiguration(new RunnerAndConfigurationSettingsImpl(manager, configuration, false));
		}
	}
}
