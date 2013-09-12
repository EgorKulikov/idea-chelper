package net.egork.chelper.util;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import net.egork.chelper.ProjectData;
import net.egork.chelper.actions.TopCoderAction;
import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TaskConfigurationType;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.configurations.TopCoderConfigurationType;
import net.egork.chelper.parser.Parser;
import net.egork.chelper.task.*;
import net.egork.chelper.tester.NewTester;

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
    // TODO: The existence of non-persistent defaultConfiguration together with persistent ProjectData is a bit weird.
    // It would be natural for everything to be persistent.
	private static Task defaultConfiguration = new Task(null, TestType.SINGLE, StreamConfiguration.STANDARD,
		StreamConfiguration.STANDARD, new Test[0], null, "-Xmx256m -Xss64m", "Main", null, TokenChecker.class.getCanonicalName(), "", new String[0], null, "", true, null, null, false, false);
	private static Parser defaultParser = Parser.PARSERS[0];

	public static void addListeners() {
		ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerAdapter() {
			@Override
			public void projectOpened(Project project) {
				ProjectData configuration = ProjectData.load(project);
				if (configuration != null) {
					eligibleProjects.put(project, configuration);
                    TopCoderAction.start(project);
                    ensureLibrary(project);
                    CodeGenerationUtilities.createTaskClassTemplateIfNeeded(project);
                    CodeGenerationUtilities.createCheckerClassTemplateIfNeeded(project);
                    CodeGenerationUtilities.createTestCaseClassTemplateIfNeeded(project);
					CodeGenerationUtilities.createTopCoderTaskTemplateIfNeeded(project);
					CodeGenerationUtilities.createTopCoderTestCaseClassTemplateIfNeeded(project);
                }
			}

			@Override
			public void projectClosed(Project project) {
				eligibleProjects.remove(project);
			}
		});
	}

    public static PsiElement getPsiElement(Project project, String classFQN) {
        return JavaPsiFacade.getInstance(project).findClass(classFQN, GlobalSearchScope.allScope(project));
    }

    private static void ensureLibrary(final Project project) {
        final ProjectData data = Utilities.getData(project);
        if (data.libraryMigrated)
            return;
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                LibraryTable table = ProjectLibraryTable.getInstance(project);
                String path = TopCoderAction.getJarPathForClass(NewTester.class);
                VirtualFile jar = VirtualFileManager.getInstance().findFileByUrl(VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, path) + JarFileSystem.JAR_SEPARATOR);
                Library library = table.getLibraryByName("CHelper");
                if (library != null) {
                    Library.ModifiableModel libraryModel = library.getModifiableModel();
                    libraryModel.addRoot(jar, OrderRootType.CLASSES);
                    libraryModel.commit();
                }
                data.completeMigration(project);
            }
        });
    }

    public static boolean isEligible(DataContext dataContext) {
		return eligibleProjects.containsKey(getProject(dataContext));
	}

	public static boolean isEligible(Project project) {
		return eligibleProjects.containsKey(project);
	}

	public static Project getProject(DataContext dataContext) {
		return PlatformDataKeys.PROJECT.getData(dataContext);
	}

	public static void updateDefaultTask(Task task) {
		if (task != null) {
			defaultConfiguration = new Task(null, task.testType, task.input, task.output, new Test[0], null,
                    task.vmArgs, task.mainClass, null, TokenChecker.class.getCanonicalName(), "", new String[0], null,
                    task.contestName, task.truncate, null, null, task.includeLocale, task.failOnOverflow);
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

	public static boolean isSupported(RunConfiguration configuration) {
		return configuration instanceof TaskConfiguration || configuration instanceof TopCoderConfiguration;
	}
}
