package net.egork.chelper.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.FileCreator;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Provider;
import net.egork.chelper.util.Utilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderConfigurationEditor extends SettingsEditor<TopCoderConfiguration> {
	private TopCoderTask task;
    private JTextField date = new JTextField();
    private JTextField contestName = new JTextField();
    private JCheckBox failOnOverflow = new JCheckBox("Fail on integer overflow");
    private JCheckBox hasTestCase;
    private SelectOrCreateClass testClass;
    private Project project;

	public TopCoderConfigurationEditor(TopCoderConfiguration configuration) {
        resetEditorFrom(configuration);
        project = configuration.getProject();
	}

	@Override
	protected void resetEditorFrom(TopCoderConfiguration s) {
        date.setText(s.getConfiguration().date);
        contestName.setText(s.getConfiguration().contestName);
        failOnOverflow.setSelected(s.getConfiguration().failOnOverflow);
		task = s.getConfiguration();
    }

	@Override
	protected void applyEditorTo(TopCoderConfiguration s) throws ConfigurationException {
		if (task == null)
			return;
        TopCoderTask task = s.getConfiguration();
        task = new TopCoderTask(task.name, task.signature, this.task.tests, date.getText(), contestName.getText(), hasTestCase.isSelected() ? new String[]{testClass.getText()} : new String[0], task.fqn, failOnOverflow.isSelected());
        s.setConfiguration(task);
	}

	@NotNull
	@Override
	protected JComponent createEditor() {
        JPanel editor = new JPanel(new VerticalFlowLayout());
        editor.add(new JLabel("Date:"));
        editor.add(date);
        editor.add(new JLabel("Contest name:"));
        editor.add(contestName);
		JButton editTests = new JButton("Edit Tests");
		editTests.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				task = task.setTests(TopCoderEditTestsDialog.editTests(task, project));
				date.setText(date.getText());
			}
		});
		editor.add(editTests);
        editor.add(new JLabel("Test class:"));
        JPanel testClassPanel = new JPanel(new BorderLayout());
        hasTestCase = new JCheckBox();
        hasTestCase.setSelected(task.testClasses.length != 0);
        testClassPanel.add(hasTestCase, BorderLayout.WEST);
        testClass = new SelectOrCreateClass(task.testClasses.length != 0 ? task.testClasses[0] : (task.name + "TestCase"), project, new Provider<String>() {
            public String provide() {
                return Utilities.getData(project).defaultDirectory;
            }
        }, new FileCreator() {
            public String createFile(Project project, String path, String name) {
                return FileUtilities.createTopCoderTestClass(project, path, name);
            }

            public boolean isValid(String name) {
                return FileUtilities.isValidClassName(name);
            }
        });
        testClass.setEnabled(hasTestCase.isSelected());
		hasTestCase.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				testClass.setEnabled(hasTestCase.isSelected());
			}
		});
        testClassPanel.add(testClass, BorderLayout.CENTER);
        editor.add(testClassPanel);
        editor.add(failOnOverflow);
		return editor;
	}

	@Override
	protected void disposeEditor() {

	}
}
