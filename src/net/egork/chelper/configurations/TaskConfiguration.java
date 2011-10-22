package net.egork.chelper.configurations;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaCommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiDirectory;
import net.egork.chelper.Utilities;
import net.egork.chelper.task.Task;
import net.egork.chelper.ui.TaskConfigurationEditor;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TaskConfiguration extends ModuleBasedConfiguration<JavaRunConfigurationModule> {
	private Task configuration;

	public TaskConfiguration(String name, Project project, Task configuration, ConfigurationFactory factory) {
		super(name, new JavaRunConfigurationModule(project, false), factory);
		this.configuration = configuration;
	}

	@Override
	public Collection<Module> getValidModules() {
		return JavaRunConfigurationModule.getModulesForClass(getProject(), configuration.getFQN());
	}

	@Override
	protected ModuleBasedConfiguration createInstance() {
		return new TaskConfiguration(getName(), getProject(), configuration, getFactory());
	}

	public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
		return new TaskConfigurationEditor(this);
	}

	public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env)
		throws ExecutionException
	{

		Utilities.getData(configuration.project).queue.run(new com.intellij.openapi.progress.Task.Backgroundable(
			configuration.project, "Creating source file")
		{
			public void run(@NotNull ProgressIndicator indicator) {
				indicator.setText("Building source file");
				configuration.createSourceFile();
			}
		});
		JavaCommandLineState state = new JavaCommandLineState(env) {
			@Override
			protected JavaParameters createJavaParameters() throws ExecutionException {
				JavaParameters parameters = new JavaParameters();
				PsiDirectory directory = Utilities.getPsiDirectory(configuration.project, configuration.location);
				Module module = ProjectRootManager.getInstance(configuration.project).getFileIndex().getModuleForFile(
					directory.getVirtualFile());
				parameters.configureByModule(module, JavaParameters.JDK_AND_CLASSES);
				parameters.setMainClass("net.egork.chelper.tester.Tester");
				parameters.getVMParametersList().add("-Xmx" + configuration.heapMemory);
				parameters.getVMParametersList().add("-Xms" + configuration.stackMemory);
				parameters.getProgramParametersList().add(Utilities.getData(configuration.project).inputClass);
				parameters.getProgramParametersList().add(Utilities.getFQN(configuration.project,
					directory, configuration.name));
				parameters.getProgramParametersList().add(configuration.testType.name());
				parameters.getProgramParametersList().add(configuration.encodeTests());
				return parameters;
			}
		};
		state.setConsoleBuilder(new TextConsoleBuilderImpl(configuration.project));
		return state;
	}

	public Task getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Task configuration) {
		this.configuration = configuration;
	}

	@Override
	public void readExternal(Element element) throws InvalidDataException {
		super.readExternal(element);
		configuration = Task.read(element.getChildText("taskConf"), getProject());
	}

	@Override
	public void writeExternal(Element element) throws WriteExternalException {
		super.writeExternal(element);
		Element configurationElement = new Element("taskConf");
		element.addContent(configurationElement);
		configurationElement.setText(configuration.toString());
	}
}
