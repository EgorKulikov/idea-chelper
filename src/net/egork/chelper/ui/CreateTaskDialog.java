package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiDirectory;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CreateTaskDialog extends JDialog {
	private Task task;
	private boolean isOk = false;
	private final JTextField taskName;
	private final JComboBox testType;
	private final JComboBox inputType;
	private final JTextField inputName;
	private final JComboBox outputType;
	private final JTextField outputName;
	private final JTextField heapMemory;
	private final JTextField stackMemory;

	public CreateTaskDialog(Task task, boolean canEditName, Project project) {
		super(null, "Task", ModalityType.APPLICATION_MODAL);
        setIconImage(Utilities.iconToImage(IconLoader.getIcon("/icons/newTask.png")));
		setAlwaysOnTop(true);
		setResizable(false);
		this.task = task;
		taskName = new JTextField(task.name);
		taskName.setEditable(canEditName);
		testType = new JComboBox(TestType.values());
		testType.setSelectedItem(task.testType);
		inputType = new JComboBox(StreamConfiguration.StreamType.values());
		inputType.setSelectedItem(task.input.type);
		inputName = new JTextField(task.input.fileName == null ? "input.txt" : task.input.fileName);
		outputType = new JComboBox(StreamConfiguration.StreamType.values());
		outputType.setSelectedItem(task.output.type);
		outputName = new JTextField(task.output.fileName == null ? "output.txt" : task.output.fileName);
		heapMemory = new JTextField(task.heapMemory);
		stackMemory = new JTextField(task.stackMemory);
		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onChange();
			}
		};
		taskName.addActionListener(listener);
		testType.addActionListener(listener);
		inputType.addActionListener(listener);
		inputName.addActionListener(listener);
		outputType.addActionListener(listener);
		outputName.addActionListener(listener);
		heapMemory.addActionListener(listener);
		stackMemory.addActionListener(listener);
        OkCancelPanel main = new OkCancelPanel(new VerticalFlowLayout()) {
            @Override
            public void onOk() {
                onChange();
                isOk = true;
                CreateTaskDialog.this.setVisible(false);
            }

            @Override
            public void onCancel() {
                CreateTaskDialog.this.task = null;
                CreateTaskDialog.this.setVisible(false);
            }
        };
        JPanel okCancelPanel = new JPanel(new BorderLayout());
        okCancelPanel.add(main.getOkButton(), BorderLayout.CENTER);
        okCancelPanel.add(main.getCancelButton(), BorderLayout.EAST);
        main.add(new JLabel("Name:"));
		main.add(taskName);
		main.add(new JLabel("Test type:"));
		main.add(testType);
		main.add(new JLabel("Input type:"));
		main.add(inputType);
		main.add(inputName);
		main.add(new JLabel("Output type:"));
		main.add(outputType);
		main.add(outputName);
		main.add(new JLabel("Heap memory:"));
		main.add(heapMemory);
		main.add(new JLabel("Stack memory:"));
		main.add(stackMemory);
		main.add(okCancelPanel);
		setContentPane(main);
		onChange();
		pack();
		Point center = Utilities.getLocation(project, main.getSize());
		setLocation(center);
	}

    private void onChange() {
		inputName.setVisible(inputType.getSelectedItem() == StreamConfiguration.StreamType.CUSTOM);
		outputName.setVisible(outputType.getSelectedItem() == StreamConfiguration.StreamType.CUSTOM);
		task = new Task(taskName.getText().trim(), task.location, (TestType) testType.getSelectedItem(),
			new StreamConfiguration((StreamConfiguration.StreamType) inputType.getSelectedItem(),
			inputName.isVisible() ? inputName.getText().trim() : null), new StreamConfiguration(
			(StreamConfiguration.StreamType) outputType.getSelectedItem(),
			outputType.isVisible() ? outputName.getText().trim() : null), heapMemory.getText().trim(),
			stackMemory.getText().trim(), true);
		pack();
	}

	public static Task showDialog(PsiDirectory directory, String defaultName) {
		Task task = Utilities.getDefaultTask().setName(defaultName == null ? "Task" : defaultName).
			setDirectory(FileUtilities.getRelativePath(directory.getProject().getBaseDir(), directory.getVirtualFile()));
		CreateTaskDialog dialog = new CreateTaskDialog(task, defaultName == null, directory.getProject());
		dialog.setVisible(true);
		Utilities.updateDefaultTask(dialog.task);
		return dialog.task;
	}

	@Override
	public void setVisible(boolean b) {
		if (b) {
			taskName.requestFocusInWindow();
			taskName.setSelectionStart(0);
			taskName.setSelectionEnd(taskName.getText().length());
		} else if (!isOk)
			task = null;
		super.setVisible(b);
	}
}
