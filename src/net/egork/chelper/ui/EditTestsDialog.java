package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import net.egork.chelper.Utilities;
import net.egork.chelper.task.Test;
import sun.awt.VariableGridLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class EditTestsDialog extends JDialog {
	private List<Test> tests;
	private int currentTest;
	private JBList testList;
	private JTextArea input;
	private JTextArea output;

	public EditTestsDialog(Test[] tests, Project project) {
		super(null, "Tests", ModalityType.APPLICATION_MODAL);
		setAlwaysOnTop(true);
		setResizable(false);
		this.tests = new ArrayList<Test>(Arrays.asList(tests));
		VariableGridLayout mainLayout = new VariableGridLayout(1, 2, 5, 5);
		mainLayout.setColFraction(0, 0.4);
		mainLayout.setColFraction(1, 0.6);
		JPanel mainPanel = new JPanel(mainLayout);
		JPanel selectorAndButtonsPanel = new JPanel(new BorderLayout());
		selectorAndButtonsPanel.add(new JLabel("Tests:"), BorderLayout.NORTH);
		testList = new JBList(tests);
		testList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		testList.setLayoutOrientation(JList.VERTICAL);
		testList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int index = testList.locationToIndex(e.getPoint());
				if (index >= 0 && index < testList.getItemsCount()) {
					saveCurrentTest();
					setSelectedTest(index);
				}
			}
		});
		selectorAndButtonsPanel.add(new JBScrollPane(testList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
		JPanel buttonsPanel = new JPanel(new GridLayout(1, 3));
		JButton save = new JButton("Save");
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveCurrentTest();
				setVisible(false);
			}
		});
		buttonsPanel.add(save);
		JButton newTest = new JButton("New");
		newTest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = EditTestsDialog.this.tests.size();
				EditTestsDialog.this.tests.add(new Test("", "", index));
				setSelectedTest(index);
			}
		});
		buttonsPanel.add(newTest);
		JButton remove = new JButton("Remove");
		remove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (currentTest == -1)
					return;
				EditTestsDialog.this.tests.remove(currentTest);
				int size = EditTestsDialog.this.tests.size();
				for (int i = currentTest; i < size; i++) {
					Test test = EditTestsDialog.this.tests.get(i);
					EditTestsDialog.this.tests.set(i, new Test(test.input, test.output, i));
				}
				if (currentTest < size) {
					setSelectedTest(currentTest);
					return;
				}
				if (size > 0) {
					setSelectedTest(0);
					return;
				}
				setSelectedTest(-1);
			}
		});
		buttonsPanel.add(remove);
		selectorAndButtonsPanel.add(buttonsPanel, BorderLayout.SOUTH);
		mainPanel.add(selectorAndButtonsPanel);
		JPanel testPanel = new JPanel(new GridLayout(2, 1, 5, 5));
		JPanel inputPanel = new JPanel(new BorderLayout());
		inputPanel.add(new JLabel("Input:"), BorderLayout.NORTH);
		input = new JTextArea();
		inputPanel.add(new JBScrollPane(input), BorderLayout.CENTER);
		JPanel outputPanel = new JPanel(new BorderLayout());
		outputPanel.add(new JLabel("Output:"), BorderLayout.NORTH);
		output = new JTextArea();
		outputPanel.add(new JBScrollPane(output), BorderLayout.CENTER);
		testPanel.add(inputPanel);
		testPanel.add(outputPanel);
		mainPanel.add(testPanel);
		setContentPane(mainPanel);
		setSelectedTest(Math.min(0, tests.length - 1));
		pack();
		setSize(600, 400);
		setLocation(Utilities.getLocation(project, this.getSize()));
	}

	private void setSelectedTest(int index) {
		currentTest = index;
		if (index == -1) {
			input.setVisible(false);
			output.setVisible(false);
		} else {
			input.setVisible(true);
			output.setVisible(true);
			input.setText(tests.get(index).input);
			output.setText(tests.get(index).output);
		}
		testList.setListData(tests.toArray());
		testList.setSelectedIndex(currentTest);
		testList.repaint();
	}

	private void saveCurrentTest() {
		if (currentTest == -1)
			return;
		tests.set(currentTest, new Test(input.getText(), output.getText(), currentTest));
	}

	public static Test[] editTests(Test[] tests, Project project) {
		EditTestsDialog dialog = new EditTestsDialog(tests, project);
		dialog.setVisible(true);
		return dialog.tests.toArray(new Test[dialog.tests.size()]);
	}
}
