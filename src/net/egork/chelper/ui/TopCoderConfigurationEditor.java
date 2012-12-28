package net.egork.chelper.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.FileCreator;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Utilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
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
        task = new TopCoderTask(task.name, task.signature, this.task.tests, date.getText(), contestName.getText(), this.task.testClasses, task.fqn, failOnOverflow.isSelected());
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
        JButton editTestClasses = new JButton("Edit test classes");
        editTestClasses.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                task = task.setTestClasses(TestClassesDialog.showDialog(task.testClasses, project, Utilities.getData(project).defaultDirectory, new FileCreator() {
                    public String createFile(Project project, String path, String name) {
                        return FileUtilities.createTopCoderTestClass(project, path, name);
                    }

                    public boolean isValid(String name) {
                        return FileUtilities.isValidClassName(name);
                    }
                }, task.name));
				date.setText(date.getText());
            }
        });
        editor.add(editTestClasses);
        editor.add(failOnOverflow);
		return editor;
	}

	@Override
	protected void disposeEditor() {

	}
}
