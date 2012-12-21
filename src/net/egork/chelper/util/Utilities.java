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
import net.egork.chelper.actions.TopCoderAction;
import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TaskConfigurationType;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.configurations.TopCoderConfigurationType;
import net.egork.chelper.parser.Parser;
import net.egork.chelper.task.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Utilities {
	private static Map<Project, ProjectData> eligibleProjects = new HashMap<Project, ProjectData>();
	private static Task defaultConfiguration = new Task(null, TestType.SINGLE, StreamConfiguration.STANDARD,
		StreamConfiguration.STANDARD, new Test[0], null, "-Xmx256m -Xss64m", "Main", null, TokenChecker.class.getCanonicalName(), "", new String[0], null, "", true, null, null);
	private static Parser defaultParser = Parser.PARSERS[0];

	public static void addListeners() {
		ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerAdapter() {
			@Override
			public void projectOpened(Project project) {
				ProjectData configuration = ProjectData.load(project);
				if (configuration != null) {
					eligibleProjects.put(project, configuration);
                    TopCoderAction.start(project);
                }
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
                    task.contestName, task.truncate, null, null);
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

	public static Parser getDefaultParser() {
		return defaultParser;
	}

	public static void setDefaultParser(Parser defaultParser) {
		Utilities.defaultParser = defaultParser;
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

    public static RunnerAndConfigurationSettings createConfiguration(TopCoderTask task, boolean setActive, Project project) {
        RunManagerImpl manager = RunManagerImpl.getInstanceImpl(project);
        RunnerAndConfigurationSettings old = manager.findConfigurationByName(task.name);
        if (old != null)
            manager.removeConfiguration(old);
        RunnerAndConfigurationSettingsImpl configuration = new RunnerAndConfigurationSettingsImpl(manager,
                new TopCoderConfiguration(task.name, project, task,
                        TopCoderConfigurationType.INSTANCE.getConfigurationFactories()[0]), false);
        manager.addConfiguration(configuration, false);
        if (setActive)
            manager.setActiveConfiguration(configuration);
        return configuration;
    }

	public static String getSimpleName(String className) {
		int position = className.lastIndexOf('.');
		if (position != -1)
			className = className.substring(position + 1);
		return className;
	}
}
