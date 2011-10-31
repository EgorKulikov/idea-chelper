package net.egork.chelper.ui;

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.task.TopCoderTest;
import net.egork.chelper.util.FileUtilities;
import sun.awt.VariableGridLayout;

import javax.swing.*;
import java.awt.*;
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
	private static int HEIGHT = new JLabel("Test").getPreferredSize().height;

	private List<TopCoderTest> tests;
	private int currentTest;
	private JBList testList;
	private JTextField[] arguments;
	private JTextField result;
	private JPanel testPanel;
	private List<JCheckBox> checkBoxes = new ArrayList<JCheckBox>();
	private JPanel checkBoxesPanel;

	public TopCoderEditTestsDialog(TopCoderTask task) {
		super(null, "Tests", ModalityType.APPLICATION_MODAL);
		setAlwaysOnTop(true);
		setResizable(false);
		this.tests = new ArrayList<TopCoderTest>(Arrays.asList(task.tests));
		VariableGridLayout mainLayout = new VariableGridLayout(1, 2, 5, 5);
		mainLayout.setColFraction(0, 0.35);
		mainLayout.setColFraction(1, 0.65);
		JPanel mainPanel = new JPanel(mainLayout);
		JPanel selectorAndButtonsPanel = new JPanel(new BorderLayout());
		selectorAndButtonsPanel.add(new JLabel("Tests:"), BorderLayout.NORTH);
		JPanel checkBoxesAndSelectorPanel = new JPanel(new BorderLayout());
		checkBoxesPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, false, false));
		for (TopCoderTest test : tests) {
			JCheckBox checkBox = createCheckBox(test);
			checkBoxesPanel.add(checkBox);
		}
		checkBoxesAndSelectorPanel.add(checkBoxesPanel, BorderLayout.WEST);
		testList = new JBList(tests);
		testList.setFixedCellHeight(HEIGHT);
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
		checkBoxesAndSelectorPanel.add(testList, BorderLayout.CENTER);
		selectorAndButtonsPanel.add(new JBScrollPane(checkBoxesAndSelectorPanel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
		JPanel buttonsPanel = new JPanel(new GridLayout(3, 1));
		JPanel upperButtonsPanel = new JPanel(new GridLayout(1, 2));
		JButton all = new JButton("All");
		all.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = 0;
				for (JCheckBox checkBox : checkBoxes) {
					checkBox.setSelected(true);
					TopCoderEditTestsDialog.this.tests.set(index, TopCoderEditTestsDialog.this.tests.get(index).
						setActive(true));
					index++;
				}
				setSelectedTest(currentTest);
			}
		});
		upperButtonsPanel.add(all);
		JButton none = new JButton("None");
		none.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = 0;
				for (JCheckBox checkBox : checkBoxes) {
					checkBox.setSelected(false);
					TopCoderEditTestsDialog.this.tests.set(index, TopCoderEditTestsDialog.this.tests.get(index).
						setActive(false));
					index++;
				}
				setSelectedTest(currentTest);
			}
		});
		upperButtonsPanel.add(none);
		buttonsPanel.add(upperButtonsPanel);
		JPanel middleButtonsPanel = new JPanel(new GridLayout(1, 2));
		JButton newTest = new JButton("New");
		newTest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveCurrentTest();
				int index = TopCoderEditTestsDialog.this.tests.size();
				String[] arguments = new String[TopCoderEditTestsDialog.this.arguments.length];
				Arrays.fill(arguments, "");
				TopCoderTest test = new TopCoderTest(arguments, "", index);
				TopCoderEditTestsDialog.this.tests.add(test);
				checkBoxesPanel.add(createCheckBox(test));
				setSelectedTest(index);
			}
		});
		middleButtonsPanel.add(newTest);
		JButton remove = new JButton("Remove");
		remove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (currentTest == -1)
					return;
				while (checkBoxes.size() > currentTest) {
					checkBoxesPanel.remove(checkBoxes.get(currentTest));
					checkBoxes.remove(currentTest);
				}
				TopCoderEditTestsDialog.this.tests.remove(currentTest);
				int size = TopCoderEditTestsDialog.this.tests.size();
				for (int i = currentTest; i < size; i++) {
					TopCoderTest test = TopCoderEditTestsDialog.this.tests.get(i);
					test = new TopCoderTest(test.arguments, test.result, i, test.active);
					TopCoderEditTestsDialog.this.tests.set(i, test);
					checkBoxesPanel.add(createCheckBox(test));
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
		middleButtonsPanel.add(remove);
		buttonsPanel.add(middleButtonsPanel);
		JButton save = new JButton("Save");
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveCurrentTest();
				setVisible(false);
			}
		});
		buttonsPanel.add(save);
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

	private JCheckBox createCheckBox(final TopCoderTest test) {
		final JCheckBox checkBox = new JCheckBox("", test.active);
		Dimension preferredSize = new Dimension(checkBox.getPreferredSize().width, HEIGHT);
		checkBox.setPreferredSize(preferredSize);
		checkBox.setMaximumSize(preferredSize);
		checkBox.setMinimumSize(preferredSize);
		checkBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tests.set(test.index, tests.get(test.index).setActive(checkBox.isSelected()));
				setSelectedTest(currentTest);
			}
		});
		checkBoxes.add(checkBox);
		return checkBox;
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
		checkBoxesPanel.repaint();
	}

	private void saveCurrentTest() {
		if (currentTest == -1)
			return;
		String[] arguments = new String[this.arguments.length];
		for (int i = 0; i < arguments.length; i++)
			arguments[i] = this.arguments[i].getText();
		tests.set(currentTest, new TopCoderTest(arguments, result.getText(), currentTest,
			checkBoxes.get(currentTest).isSelected()));
	}

	public static TopCoderTest[] editTests(TopCoderTask task) {
		TopCoderEditTestsDialog dialog = new TopCoderEditTestsDialog(task);
		dialog.setVisible(true);
		return dialog.tests.toArray(new TopCoderTest[dialog.tests.size()]);
	}
}
