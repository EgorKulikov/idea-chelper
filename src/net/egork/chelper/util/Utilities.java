package net.egork.chelper.util;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import net.egork.chelper.ProjectData;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Utilities {
	private static Map<Project, ProjectData> eligibleProjects = new HashMap<Project, ProjectData>();
	private static Task defaultConfiguration = new Task(null, null, TestType.SINGLE, StreamConfiguration.STANDARD,
		StreamConfiguration.STANDARD, "256M", "64M", null);

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
		if (task != null)
			defaultConfiguration = task.setDirectory(null).setProject(null).setName("Task");
	}

	public static Task getDefaultTask() {
		return defaultConfiguration;
	}

	public static ProjectData getData(Project project) {
		return eligibleProjects.get(project);
	}

}
