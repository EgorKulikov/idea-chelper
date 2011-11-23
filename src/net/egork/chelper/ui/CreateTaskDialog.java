package net.egork.chelper.ui;

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.psi.PsiDirectory;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Utilities;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

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

	public CreateTaskDialog(Task task, boolean canEditName) {
		super(null, "Task", ModalityType.APPLICATION_MODAL);
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
		JButton ok = new JButton("Ok");
		JButton cancel = new JButton("Cancel");
		JPanel okCancelPanel = new JPanel(new BorderLayout());
		okCancelPanel.add(ok, BorderLayout.CENTER);
		okCancelPanel.add(cancel, BorderLayout.EAST);
		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onChange();
			}
		};
		Action saveAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				onChange();
				isOk = true;
				setVisible(false);
			}
		};
		Action cancelAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				CreateTaskDialog.this.task = null;
				setVisible(false);
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
		ok.addActionListener(saveAction);
		cancel.addActionListener(cancelAction);
		initialize(taskName, saveAction, cancelAction);
		initialize(testType, saveAction, cancelAction);
		initialize(inputType, saveAction, cancelAction);
		initialize(inputName, saveAction, cancelAction);
		initialize(outputType, saveAction, cancelAction);
		initialize(outputName, saveAction, cancelAction);
		initialize(heapMemory, saveAction, cancelAction);
		initialize(stackMemory, saveAction, cancelAction);
		initialize(ok, saveAction, cancelAction);
		initialize(cancel, cancelAction, cancelAction);
		JPanel main = new JPanel(new VerticalFlowLayout());
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
		Point center = Utilities.getLocation(task.project, main.getSize());
		setLocation(center);
	}

	private static void initialize(JComponent component, Action saveAction, Action cancelAction) {
		component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "save");
		component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
		component.getActionMap().put("save", saveAction);
		component.getActionMap().put("cancel", cancelAction);
	}

	private void onChange() {
		inputName.setVisible(inputType.getSelectedItem() == StreamConfiguration.StreamType.CUSTOM);
		outputName.setVisible(outputType.getSelectedItem() == StreamConfiguration.StreamType.CUSTOM);
		task = new Task(taskName.getText().trim(), task.location, (TestType) testType.getSelectedItem(),
			new StreamConfiguration((StreamConfiguration.StreamType) inputType.getSelectedItem(),
			inputName.isVisible() ? inputName.getText().trim() : null), new StreamConfiguration(
			(StreamConfiguration.StreamType) outputType.getSelectedItem(),
			outputType.isVisible() ? outputName.getText().trim() : null), heapMemory.getText().trim(),
			stackMemory.getText().trim(), task.project, true);
		pack();
	}

	public static Task showDialog(PsiDirectory directory, String defaultName) {
		Task task = Utilities.getDefaultTask().setName(defaultName == null ? "Task" : defaultName).
			setDirectory(FileUtilities.getRelativePath(directory.getProject().getBaseDir(), directory.getVirtualFile())).
			setProject(directory.getProject());
		CreateTaskDialog dialog = new CreateTaskDialog(task, defaultName == null);
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
