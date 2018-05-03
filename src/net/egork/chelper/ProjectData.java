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
            "archive/unsorted", "main", "lib/test", false, false, 0, true, false);
    public static final int CURRENT_LIBRARY_VERSION = 2;

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
    public final int libraryVersion;
    public final boolean smartTesting;
    public final boolean extensionProposed;

    public ProjectData(String inputClass, String outputClass, String[] excludedPackages, String outputDirectory, String author, String archiveDirectory, String defaultDirectory, String testDirectory, boolean enableUnitTests, boolean failOnIntegerOverflowForNewTasks, int libraryVersion, boolean smartTesting, boolean extensionProposed) {
        this.extensionProposed = extensionProposed;
        this.inputClass = inputClass.trim();
        this.outputClass = outputClass.trim();
        this.excludedPackages = excludedPackages;
        this.outputDirectory = outputDirectory.trim();
        this.author = author.trim();
        this.archiveDirectory = archiveDirectory.trim();
        this.defaultDirectory = defaultDirectory.trim();
        this.testDirectory = testDirectory.trim();
        this.enableUnitTests = enableUnitTests;
        this.failOnIntegerOverflowForNewTasks = failOnIntegerOverflowForNewTasks;
        this.libraryVersion = libraryVersion;
        this.smartTesting = smartTesting;
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
        libraryVersion = Integer.valueOf(properties.getProperty("libraryVersion",
                Boolean.valueOf(properties.getProperty("libraryMigrated", "false")) ? "1" : "0"));
        smartTesting = Boolean.valueOf(properties.getProperty("smartTesting", Boolean.toString(DEFAULT.smartTesting)));
        extensionProposed = Boolean.valueOf(properties.getProperty("extensionProposed", Boolean.toString(DEFAULT.extensionProposed)));
    }

    public static ProjectData load(Project project) {
        if (project == null) {
            return null;
        }
        VirtualFile root = project.getBaseDir();
        if (root == null) {
            return null;
        }
        VirtualFile config = root.findChild("chelper.properties");
        if (config == null) {
            return null;
        }
        Properties properties = FileUtilities.loadProperties(config);
        if (properties == null) {
            return null;
        }
        return new ProjectData(properties);
    }

    public void save(final Project project) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                if (project == null) {
                    return;
                }
                VirtualFile root = project.getBaseDir();
                if (root == null) {
                    return;
                }
                try {
                    VirtualFile config = root.findOrCreateChildData(null, "chelper.properties");
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
                    properties.setProperty("libraryVersion", Integer.toString(libraryVersion));
                    properties.setProperty("smartTesting", Boolean.toString(smartTesting));
                    properties.setProperty("extensionProposed", Boolean.toString(extensionProposed));
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
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(aPackage);
        }
        return result.toString();
    }

    public void completeMigration(Project project) {
        ProjectData newData = new ProjectData(inputClass, outputClass, excludedPackages, outputDirectory, author,
                archiveDirectory, defaultDirectory, testDirectory, enableUnitTests, failOnIntegerOverflowForNewTasks, CURRENT_LIBRARY_VERSION, smartTesting, extensionProposed);
        newData.save(project);
        Utilities.addProjectData(project, newData);
    }

    public void completeExtensionProposal(Project project) {
        ProjectData newData = new ProjectData(inputClass, outputClass, excludedPackages, outputDirectory, author,
                archiveDirectory, defaultDirectory, testDirectory, enableUnitTests, failOnIntegerOverflowForNewTasks, libraryVersion, smartTesting, true);
        newData.save(project);
        Utilities.addProjectData(project, newData);
    }
}
