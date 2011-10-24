package net.egork.chelper.ui;

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.task.TopCoderTest;
import sun.awt.VariableGridLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
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
public class TopCoderEditTestsDialog extends JDialog {
	private List<TopCoderTest> tests;
	private int currentTest;
	private JBList testList;
	private JTextField[] arguments;
	private JTextField result;
	private JPanel testPanel;

	public TopCoderEditTestsDialog(TopCoderTask task) {
		super(null, "Tests", ModalityType.APPLICATION_MODAL);
		setAlwaysOnTop(true);
		setResizable(false);
		this.tests = new ArrayList<TopCoderTest>(Arrays.asList(task.tests));
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
				int index = TopCoderEditTestsDialog.this.tests.size();
				String[] arguments = new String[TopCoderEditTestsDialog.this.arguments.length];
				Arrays.fill(arguments, "");
				TopCoderEditTestsDialog.this.tests.add(new TopCoderTest(arguments, "", index));
				setSelectedTest(index);
			}
		});
		buttonsPanel.add(newTest);
		JButton remove = new JButton("Remove");
		remove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (currentTest == -1)
					return;
				TopCoderEditTestsDialog.this.tests.remove(currentTest);
				int size = TopCoderEditTestsDialog.this.tests.size();
				for (int i = currentTest; i < size; i++) {
					TopCoderTest test = TopCoderEditTestsDialog.this.tests.get(i);
					TopCoderEditTestsDialog.this.tests.set(i, new TopCoderTest(test.arguments, test.result, i));
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
		testPanel = new JPanel(new VerticalFlowLayout());
		testPanel.add(new JLabel("Arguments:"));
		arguments = new JTextField[task.signature.arguments.length];
		for (int i = 0; i < arguments.length; i++) {
			arguments[i] = new JTextField();
			testPanel.add(createPanel(task.signature.arguments[i].getSimpleName() + " " +
				task.signature.argumentNames[i], arguments[i]));
		}
		testPanel.add(new JLabel("Result:"));
		result = new JTextField();
		testPanel.add(createPanel(task.signature.result.getSimpleName(), result));
		mainPanel.add(new JBScrollPane(testPanel));
		setContentPane(mainPanel);
		setSelectedTest(Math.min(0, task.tests.length - 1));
		pack();
		setSize(600, 400);
		setLocation(FileUtilities.getLocation(task.project, this.getSize()));
	}

	private JPanel createPanel(String label, JTextField editor) {
		VariableGridLayout layout = new VariableGridLayout(1, 2);
		layout.setColFraction(0, 0.3);
		layout.setColFraction(1, 0.7);
		JPanel panel = new JPanel(layout);
		panel.add(new JLabel(label));
		panel.add(editor);
		return panel;
	}

	private void setSelectedTest(int index) {
		currentTest = index;
		if (index == -1)
			testPanel.setVisible(false);
		else {
			testPanel.setVisible(true);
			for (int i = 0; i < arguments.length; i++)
				arguments[i].setText(tests.get(index).arguments[i]);
			result.setText(tests.get(index).result);
		}
		testList.setListData(tests.toArray());
		testList.setSelectedIndex(currentTest);
		testList.repaint();
	}

	private void saveCurrentTest() {
		if (currentTest == -1)
			return;
		String[] arguments = new String[this.arguments.length];
		for (int i = 0; i < arguments.length; i++)
			arguments[i] = this.arguments[i].getText();
		tests.set(currentTest, new TopCoderTest(arguments, result.getText(), currentTest));
	}

	public static TopCoderTest[] editTests(TopCoderTask task) {
		TopCoderEditTestsDialog dialog = new TopCoderEditTestsDialog(task);
		dialog.setVisible(true);
		return dialog.tests.toArray(new TopCoderTest[dialog.tests.size()]);
	}
}
