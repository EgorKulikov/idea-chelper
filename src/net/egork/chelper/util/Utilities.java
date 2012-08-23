package net.egork.chelper.util;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import net.egork.chelper.ProjectData;
import net.egork.chelper.actions.ParseContestAction;
import net.egork.chelper.actions.ParseTaskAction;
import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TaskConfigurationType;
import net.egork.chelper.parser.ContestParser;
import net.egork.chelper.parser.TaskParser;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Utilities {
	private static Map<Project, ProjectData> eligibleProjects = new HashMap<Project, ProjectData>();
	private static Task defaultConfiguration = new Task(null, TestType.SINGLE, StreamConfiguration.STANDARD,
		StreamConfiguration.STANDARD, new Test[0], null, "-Xmx256m -Xss64m", "Main", null, TokenChecker.class.getCanonicalName(), "", new String[0], null, "", true, null, null);
	private static ContestParser defaultContestParser = ParseContestAction.PARSERS[0];
	private static TaskParser defaultTaskParser = ParseTaskAction.PARSERS[0];

	public static void addListeners() {
		ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerAdapter() {
			@Override
			public void projectOpened(Project project) {
				ProjectData configuration = ProjectData.load(project);
				if (configuration != null)
					eligibleProjects.put(project, configuration);
			}

			@Override
			public void projectClosed(Project project) {
				eligibleProjects.remove(project);
			}
		});
	}

	public static boolean isEligible(DataContext dataContext) {
		return eligibleProjects.containsKey(getProject(dataContext));
	}

	public static Project getProject(DataContext dataContext) {
		return PlatformDataKeys.PROJECT.getData(dataContext);
	}

	public static void updateDefaultTask(Task task) {
		if (task != null) {
			defaultConfiguration = new Task(null, task.testType, task.input, task.output, new Test[0], null,
                    task.vmArgs, task.mainClass, null, TokenChecker.class.getCanonicalName(), "", new String[0], null,
                    "", task.truncate, null, null);
        }
	}

	public static Task getDefaultTask() {
		return defaultConfiguration;
	}

	public static ProjectData getData(Project project) {
		return eligibleProjects.get(project);
	}

	public static void openElement(Project project, PsiElement element) {
		if (element instanceof PsiFile) {
			VirtualFile virtualFile = ((PsiFile) element).getVirtualFile();
			if (virtualFile == null)
				return;
			FileEditorManager.getInstance(project).openFile(virtualFile, true);
		} else if (element instanceof PsiClass) {
			FileEditorManager.getInstance(project).openFile(FileUtilities.getFile(project,
				getData(project).defaultDirectory + "/" + ((PsiClass) element).getName() + ".java"), true);
		}
	}

	public static Point getLocation(Project project, Dimension size) {
		JComponent component = WindowManager.getInstance().getIdeFrame(project).getComponent();
		Point center = component.getLocationOnScreen();
		center.x += component.getWidth() / 2;
		center.y += component.getHeight() / 2;
		center.x -= size.getWidth() / 2;
		center.y -= size.getHeight() / 2;
		return center;
	}

	public static RunnerAndConfigurationSettings createConfiguration(Task task, boolean setActive, Project project) {
		RunManagerImpl manager = RunManagerImpl.getInstanceImpl(project);
		RunnerAndConfigurationSettings old = manager.findConfigurationByName(task.name);
		if (old != null)
			manager.removeConfiguration(old);
		RunnerAndConfigurationSettingsImpl configuration = new RunnerAndConfigurationSettingsImpl(manager,
			new TaskConfiguration(task.name, project, task,
			TaskConfigurationType.INSTANCE.getConfigurationFactories()[0]), false);
		manager.addConfiguration(configuration, false);
		if (setActive)
			manager.setActiveConfiguration(configuration);
		return configuration;
	}

	public static ContestParser getDefaultContestParser() {
		return defaultContestParser;
	}

	public static void setDefaultContestParser(ContestParser defaultContestParser) {
		Utilities.defaultContestParser = defaultContestParser;
	}

	public static TaskParser getDefaultTaskParser() {
		return defaultTaskParser;
	}

	public static void setDefaultTaskParser(TaskParser defaultTaskParser) {
		Utilities.defaultTaskParser = defaultTaskParser;
	}

    public static void addProjectData(Project project, ProjectData data) {
        eligibleProjects.put(project, data);
    }

    public static Image iconToImage(Icon icon) {
        if (icon instanceof ImageIcon) {
            return ((ImageIcon)icon).getImage();
        } else {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return image;
        }
    }

    public static String getDateString(Date date) {
        StringBuilder result = new StringBuilder();
        result.append(date.getYear() + 1900).append('.');
        if (date.getMonth() < 9)
            result.append('0');
        result.append(date.getMonth() + 1).append('.');
        if (date.getDay() < 10)
            result.append('0');
        result.append(date.getDay());
        return result.toString();
    }
}
