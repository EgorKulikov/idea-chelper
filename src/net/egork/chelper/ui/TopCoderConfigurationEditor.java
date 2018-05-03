package net.egork.chelper.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import net.egork.chelper.configurations.TopCoderConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderConfigurationEditor extends SettingsEditor<TopCoderConfiguration> {
    private TopCoderConfiguration taskConfiguration;
    private JPanel wrapper;
    private TopCoderTaskPanel taskConfigurationPanel;

    public TopCoderConfigurationEditor(TopCoderConfiguration taskConfiguration) {
        this.taskConfiguration = taskConfiguration;
        applyTask();
    }

    private void applyTask() {
        taskConfigurationPanel = new TopCoderTaskPanel(taskConfiguration.getProject(), taskConfiguration.getConfiguration());
        if (wrapper == null) {
            wrapper = new JPanel(new BorderLayout());
        }
        wrapper.add(taskConfigurationPanel, BorderLayout.CENTER);
    }

    @Override
    protected void resetEditorFrom(TopCoderConfiguration s) {
        taskConfiguration = s;
        applyTask();
    }

    @Override
    protected void applyEditorTo(TopCoderConfiguration s) throws ConfigurationException {
        s.setConfiguration(taskConfigurationPanel.getTask());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        applyTask();
        return wrapper;
    }

    @Override
    protected void disposeEditor() {
    }
}
