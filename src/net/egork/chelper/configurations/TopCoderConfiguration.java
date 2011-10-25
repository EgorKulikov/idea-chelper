package net.egork.chelper.configurations;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiDirectory;
import net.egork.chelper.util.CodeGenerationUtilities;
import net.egork.chelper.util.EncodingUtilities;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Utilities;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.ui.TopCoderConfigurationEditor;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderConfiguration extends ModuleBasedConfiguration<JavaRunConfigurationModule> {
	private TopCoderTask configuration;

	public TopCoderConfiguration(String name, Project project, TopCoderTask configuration, ConfigurationFactory factory)
	{
		super(name, new JavaRunConfigurationModule(project, false), factory);
		this.configuration = configuration;
	}

	@Override
	public Collection<Module> getValidModules() {
		return JavaRunConfigurationModule.getModulesForClass(getProject(), configuration.getFQN());
	}

	@Override
	protected ModuleBasedConfiguration createInstance() {
		return new TopCoderConfiguration(getName(), getProject(), configuration, getFactory());
	}

	public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
		return new TopCoderConfigurationEditor(this);
	}

	public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env)
		throws ExecutionException
	{
		CodeGenerationUtilities.createSourceFile(configuration);
		JavaCommandLineState state = new JavaCommandLineState(env) {
			@Override
			protected JavaParameters createJavaParameters() throws ExecutionException {
				JavaParameters parameters = new JavaParameters();
				PsiDirectory directory = FileUtilities.getPsiDirectory(configuration.project,
					Utilities.getData(configuration.project).defaultDir);
				Module module = ProjectRootManager.getInstance(configuration.project).getFileIndex().getModuleForFile(
					directory.getVirtualFile());
				parameters.configureByModule(module, JavaParameters.JDK_AND_CLASSES);
				parameters.setMainClass("net.egork.chelper.tester.TopCoderTester");
				parameters.getVMParametersList().add("-Xmx64M");
				parameters.getProgramParametersList().add(configuration.getSignature());
				parameters.getProgramParametersList().add(configuration.getFQN());
				parameters.getProgramParametersList().add(EncodingUtilities.encodeTests(configuration.tests));
				return parameters;
			}
		};
		state.setConsoleBuilder(new TextConsoleBuilderImpl(configuration.project));
		return state;
	}

	public TopCoderTask getConfiguration() {
		return configuration;
	}

	public void setConfiguration(TopCoderTask configuration) {
		this.configuration = configuration;
	}

	@Override
	public void readExternal(Element element) throws InvalidDataException {
		super.readExternal(element);
		configuration = EncodingUtilities.decodeTopCoderTask(element.getChildText("taskConf"), getProject());
	}

	@Override
	public void writeExternal(Element element) throws WriteExternalException {
		super.writeExternal(element);
		Element configurationElement = new Element("taskConf");
		element.addContent(configurationElement);
		configurationElement.setText(EncodingUtilities.encodeTask(configuration));
	}

}
