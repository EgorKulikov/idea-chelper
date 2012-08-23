package net.egork.chelper.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import net.egork.chelper.configurations.TaskConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TaskConfigurationEditor extends SettingsEditor<TaskConfiguration> {
	private TaskConfiguration taskConfiguration;
    private TaskConfigurationPanel taskConfigurationPanel;

    public TaskConfigurationEditor(TaskConfiguration taskConfiguration) {
		this.taskConfiguration = taskConfiguration;
		applyTask();
	}

	private void applyTask() {
        taskConfigurationPanel = new TaskConfigurationPanel(taskConfiguration.getConfiguration(), false, taskConfiguration.getProject(), null, null);
	}

	@Override
	protected void resetEditorFrom(TaskConfiguration s) {
		taskConfiguration = s;
		applyTask();
	}

    @Override
	protected void applyEditorTo(TaskConfiguration s) throws ConfigurationException {
		s.setConfiguration(taskConfigurationPanel.getTask());
	}

	@NotNull
	@Override
	protected JComponent createEditor() {
        taskConfigurationPanel = new TaskConfigurationPanel(taskConfiguration.getConfiguration(), false, taskConfiguration.getProject(), null, null);
        return taskConfigurationPanel;
	}

    @Override
	protected void disposeEditor() {
	}
}
