package net.egork.chelper;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Utilities;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class ProjectData {
    public static final ProjectData DEFAULT = new ProjectData(
        "java.util.Scanner", "java.io.PrintWriter", "java.,javax.,com.sun.".split(","), "output", "",
        "archive/unsorted", "main", "lib/test", false, false, false);

	public final String inputClass;
	public final String outputClass;
	public final String[] excludedPackages;
	public final String outputDirectory;
	public final String author;
	public final String archiveDirectory;
	public final String defaultDirectory;
	public final String testDirectory;
	public final boolean enableUnitTests;
    public final boolean failOnIntegerOverflowForNewTasks;
    public final boolean libraryMigrated;

    public ProjectData(String inputClass, String outputClass, String[] excludedPackages, String outputDirectory, String author, String archiveDirectory, String defaultDirectory, String testDirectory, boolean enableUnitTests, boolean failOnIntegerOverflowForNewTasks, boolean libraryMigrated) {
        this.inputClass = inputClass;
        this.outputClass = outputClass;
        this.excludedPackages = excludedPackages;
        this.outputDirectory = outputDirectory;
        this.author = author;
        this.archiveDirectory = archiveDirectory;
        this.defaultDirectory = defaultDirectory;
        this.testDirectory = testDirectory;
        this.enableUnitTests = enableUnitTests;
        this.failOnIntegerOverflowForNewTasks = failOnIntegerOverflowForNewTasks;
        this.libraryMigrated = libraryMigrated;
    }

    public ProjectData(Properties properties) {
		inputClass = properties.getProperty("inputClass", DEFAULT.inputClass);
		outputClass = properties.getProperty("outputClass", DEFAULT.outputClass);
		excludedPackages = properties.getProperty("excludePackages", join(DEFAULT.excludedPackages)).split(",");
		outputDirectory = properties.getProperty("outputDirectory", DEFAULT.outputDirectory);
		author = properties.getProperty("author", DEFAULT.author);
		archiveDirectory = properties.getProperty("archiveDirectory", DEFAULT.archiveDirectory);
		defaultDirectory = properties.getProperty("defaultDirectory", DEFAULT.defaultDirectory);
		testDirectory = properties.getProperty("testDirectory", DEFAULT.testDirectory);
		enableUnitTests = Boolean.valueOf(properties.getProperty("enableUnitTests", Boolean.toString(DEFAULT.enableUnitTests)));
        failOnIntegerOverflowForNewTasks = Boolean.valueOf(properties.getProperty("failOnIntegerOverflowForNewTasks", Boolean.toString(DEFAULT.enableUnitTests)));
        libraryMigrated = Boolean.valueOf(properties.getProperty("libraryMigrated", Boolean.toString(DEFAULT.libraryMigrated)));
	}

	public static ProjectData load(Project project) {
		if (project == null)
			return null;
		VirtualFile root = project.getBaseDir();
		if (root == null)
			return null;
		VirtualFile config = root.findChild("chelper.properties");
		if (config == null)
			return null;
		Properties properties = FileUtilities.loadProperties(config);
		if (properties == null)
			return null;
		return new ProjectData(properties);
	}

    public void save(final Project project) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                if (project == null)
                    return;
                VirtualFile root = project.getBaseDir();
                if (root == null)
                    return;
                try {
                    VirtualFile config = root.findChild("chelper.properties");
                    if (config == null)
                        config = root.createChildData(null, "chelper.properties");
                    Properties properties = new Properties();
                    properties.setProperty("inputClass", inputClass);
                    properties.setProperty("outputClass", outputClass);
                    properties.setProperty("excludePackages", join(excludedPackages));
                    properties.setProperty("outputDirectory", outputDirectory);
                    properties.setProperty("author", author);
                    properties.setProperty("archiveDirectory", archiveDirectory);
                    properties.setProperty("defaultDirectory", defaultDirectory);
                    properties.setProperty("testDirectory", testDirectory);
                    properties.setProperty("enableUnitTests", Boolean.toString(enableUnitTests));
                    properties.setProperty("failOnIntegerOverflowForNewTasks", Boolean.toString(failOnIntegerOverflowForNewTasks));
                    properties.setProperty("libraryMigrated", Boolean.toString(libraryMigrated));
                    OutputStream outputStream = config.getOutputStream(null);
                    properties.store(outputStream, "");
                    outputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static String join(String[] excludedPackages) {
        StringBuilder result = new StringBuilder();
        for (String aPackage : excludedPackages) {
            if (result.length() > 0)
                result.append(',');
            result.append(aPackage);
        }
        return result.toString();
    }

    public void completeMigration(Project project) {
        ProjectData newData = new ProjectData(inputClass, outputClass, excludedPackages, outputDirectory, author,
            archiveDirectory, defaultDirectory, testDirectory, enableUnitTests, failOnIntegerOverflowForNewTasks, true);
        newData.save(project);
        Utilities.addProjectData(project, newData);
    }
}
