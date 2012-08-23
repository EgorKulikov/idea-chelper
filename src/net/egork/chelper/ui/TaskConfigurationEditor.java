package net.egork.chelper.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.VerticalFlowLayout;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;
import org.jetbrains.annotations.NotNull;
import sun.awt.VariableGridLayout;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TaskConfigurationEditor extends SettingsEditor<TaskConfiguration> {
	private final JComboBox testType = new JComboBox(TestType.values());
	private final JComboBox inputType = new JComboBox(StreamConfiguration.StreamType.values());
	private final JTextField inputFileName = new JTextField();
	private final JComboBox outputType = new JComboBox(StreamConfiguration.StreamType.values());
	private final JTextField outputFileName = new JTextField();
	private final JTextField heapMemory = new JTextField();
	private final JTextField stackMemory = new JTextField();
	private final JCheckBox truncate = new JCheckBox("Truncate big input/output");
	private TaskConfiguration taskConfiguration;
	private JComponent inputPanel;
	private JComponent outputPanel;

	public TaskConfigurationEditor(TaskConfiguration taskConfiguration) {
		this.taskConfiguration = taskConfiguration;
		applyTask();
	}

	private void applyTask() {
		Task base = taskConfiguration.getConfiguration();
		testType.setSelectedItem(base.testType);
		inputType.setSelectedItem(base.input.type);
		inputFileName.setText(base.input.type == StreamConfiguration.StreamType.CUSTOM ? base.input.fileName :
			"input.txt");
		outputType.setSelectedItem(base.output.type);
		outputFileName.setText(base.output.type == StreamConfiguration.StreamType.CUSTOM ? base.output.fileName :
			"output.txt");
		heapMemory.setText(base.heapMemory);
		stackMemory.setText(base.stackMemory);
		truncate.setSelected(base.truncate);
	}

	@Override
	protected void resetEditorFrom(TaskConfiguration s) {
		taskConfiguration = s;
		applyTask();
		ensureVisibility();
	}

	private void ensureVisibility() {
		if (inputPanel != null)
			inputPanel.setVisible(inputType.getSelectedItem() == StreamConfiguration.StreamType.CUSTOM);
		if (outputPanel != null)
			outputPanel.setVisible(outputType.getSelectedItem() == StreamConfiguration.StreamType.CUSTOM);
	}

	@Override
	protected void applyEditorTo(TaskConfiguration s) throws ConfigurationException {
		Task old = taskConfiguration.getConfiguration();
		Task task = new Task(old.name, old.location, (TestType) testType.getSelectedItem(), new StreamConfiguration(
			(StreamConfiguration.StreamType) inputType.getSelectedItem(),
			inputType.getSelectedItem() == StreamConfiguration.StreamType.CUSTOM ? inputFileName.getText() : null),
			new StreamConfiguration((StreamConfiguration.StreamType) outputType.getSelectedItem(),
			outputType.getSelectedItem() == StreamConfiguration.StreamType.CUSTOM ? outputFileName.getText() : null),
			heapMemory.getText(), stackMemory.getText(), truncate.isSelected(), old.tests);
		s.setConfiguration(task);
		s.setName(task.name);
	}

	@NotNull
	@Override
	protected JComponent createEditor() {
		JPanel panel = new JPanel(new VerticalFlowLayout());
		panel.add(labelAndComponent("Test type: ", testType));
		panel.add(labelAndComponent("Input type: ", inputType));
		inputPanel = labelAndComponent("Input file: ", inputFileName);
		panel.add(inputPanel);
		panel.add(labelAndComponent("Output type: ", outputType));
		outputPanel = labelAndComponent("Output file: ", outputFileName);
		panel.add(outputPanel);
		panel.add(labelAndComponent("Heap memory: ", heapMemory));
		panel.add(labelAndComponent("Stack memory: ", stackMemory));
		panel.add(truncate);
		inputType.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				ensureVisibility();
			}
		});
		outputType.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				ensureVisibility();
			}
		});
		JButton editTests = new JButton("Edit tests");
		editTests.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Task task = taskConfiguration.getConfiguration();
				taskConfiguration.setConfiguration(task.setTests(EditTestsDialog.editTests(task.tests, taskConfiguration.getProject())));
			}
		});
		JPanel editTestsPanel = new JPanel(new BorderLayout());
		editTestsPanel.add(editTests, BorderLayout.WEST);
		panel.add(editTestsPanel);
		ensureVisibility();
		return panel;
	}

	private static JComponent labelAndComponent(String label, JComponent component) {
		VariableGridLayout layout = new VariableGridLayout(1, 2);
		layout.setColFraction(0, .3);
		layout.setColFraction(1, .7);
		JPanel panel = new JPanel(layout);
		panel.add(new JLabel(label), BorderLayout.WEST);
		panel.add(component, BorderLayout.CENTER);
		return panel;
	}

	@Override
	protected void disposeEditor() {
	}
}
