package net.egork.chelper;

import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.configurations.TopCoderConfigurationType;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.CodeGenerationUtilities;
import net.egork.chelper.util.FileUtilities;

import java.util.Properties;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class ProjectData {
	public final String inputClass;
	public final String outputClass;
	public final String[] excludedPackages;
	public final String outputDirectory;
	public final String author;
	public final String archive;
	public final String defaultDir;
	public final String topcoderDir;
	public final String testDir;
	public final boolean enableUnitTests;

	public ProjectData(Properties properties, final Project project) {
		inputClass = properties.getProperty("inputClass", "java.util.Scanner");
		outputClass = properties.getProperty("outputClass", "java.io.PrintWriter");
		excludedPackages = properties.getProperty("excludePackages", "java.,javax.,com.sun.").split(",");
		outputDirectory = properties.getProperty("outputDirectory", "output");
		author = properties.getProperty("author", "");
		archive = properties.getProperty("archiveDirectory", "archive/unsorted");
		defaultDir = properties.getProperty("defaultDirectory", "main");
		topcoderDir = properties.getProperty("topcoderDirectory", "topcoder");
		testDir = properties.getProperty("testDirectory", "lib/test");
		enableUnitTests = Boolean.valueOf(properties.getProperty("enableUnitTests", "false"));
		VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
			@Override
			public void fileCreated(VirtualFileEvent event) {
				VirtualFile file = event.getFile();
				if (file.getParent() != FileUtilities.getFile(project, topcoderDir) || !"java".equals(file.getExtension()))
					return;
				TopCoderTask task = CodeGenerationUtilities.parseTopCoderStub(event.getFile(), project);
				if (task != null) {
					RunManagerImpl manager = RunManagerImpl.getInstanceImpl(project);
					RunnerAndConfigurationSettingsImpl configuration = new RunnerAndConfigurationSettingsImpl(manager,
						new TopCoderConfiguration(task.name, project, task,
						TopCoderConfigurationType.INSTANCE.getConfigurationFactories()[0]), false);
					manager.addConfiguration(configuration, false);
					manager.setActiveConfiguration(configuration);
				}
			}
		});
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
		return new ProjectData(properties, project);
	}
}
