package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import net.egork.chelper.actions.ParseContestAction;
import net.egork.chelper.actions.ParseTaskAction;
import net.egork.chelper.parser.ContestParser;
import net.egork.chelper.parser.Parser;
import net.egork.chelper.parser.TaskOptions;
import net.egork.chelper.parser.TaskParser;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;
import net.egork.chelper.util.Utilities;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class ParseDialog extends JDialog {
	private Collection<Task> result = Collections.emptyList();
	private Parser selected;

	private ParseDialog(final Project project, final boolean parseContest) {
		super(null, parseContest ? "Parse Contest" : "Parse Task", ModalityType.APPLICATION_MODAL);
		JPanel contentPanel = new JPanel(new VerticalFlowLayout());
		final JComboBox parserCombo = new JComboBox(parseContest ? ParseContestAction.PARSERS : ParseTaskAction.PARSERS);
		parserCombo.setRenderer(new ListCellRenderer() {
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus)
			{
				Parser parser = (Parser) value;
				JLabel label = new JLabel(parser.getName(), parser.getIcon(), JLabel.LEFT);
				label.setOpaque(true);
				if (isSelected)
					label.setBackground(UIManager.getColor("textHighlight"));
				return label;
			}
		});
		contentPanel.add(parserCombo);
		final JTextField id = new JTextField();
		JPanel idPanel = createPanel(parseContest ? "Contest id:" : "Task id:", id);
		contentPanel.add(idPanel);
		final JComboBox testType = new JComboBox(TestType.values());
		final JPanel testTypePanel = createPanel("Test type:", testType);
		contentPanel.add(testTypePanel);
		final JComboBox inputType = new JComboBox(StreamConfiguration.StreamType.values());
		final JPanel inputTypePanel = createPanel("Input type:", inputType);
		contentPanel.add(inputTypePanel);
		final JTextField inputFileName = new JTextField("input.txt");
		final JPanel inputFileNamePanel = createPanel("Input filename:", inputFileName);
		contentPanel.add(inputFileNamePanel);
		final JComboBox outputType = new JComboBox(StreamConfiguration.StreamType.values());
		final JPanel outputTypePanel = createPanel("Output type:", outputType);
		contentPanel.add(outputTypePanel);
		final JTextField outputFileName = new JTextField("output.txt");
		final JPanel outputFileNamePanel = createPanel("Output filename:", outputFileName);
		contentPanel.add(outputFileNamePanel);
		final JTextField heapMemory = new JTextField("256M");
		final JPanel heapMemoryPanel = createPanel("Heap memory:", heapMemory);
		contentPanel.add(heapMemoryPanel);
		final JTextField stackMemory = new JTextField("64M");
		final JPanel stackMemoryPanel = createPanel("Stack memory:", stackMemory);
		contentPanel.add(stackMemoryPanel);
		JPanel okCancelPanel = new JPanel(new GridLayout(1, 2));
		JButton ok = new JButton("Ok");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Collection<String> taskIDs;
				if (parseContest)
					taskIDs = ((ContestParser)selected).parse(id.getText());
				else
					taskIDs = Collections.singleton(id.getText());
				TaskParser taskParser;
				if (parseContest)
					taskParser = ((ContestParser)selected).getTaskParser();
				else
					taskParser = (TaskParser) selected;
				TaskOptions options = selected.getOptions();
				Task predefined = new Task(null, Utilities.getData(project).defaultDir,
					options.shouldProvideTestType() ? (TestType)testType.getSelectedItem() : null,
					options.shouldProvideInputType() ? new StreamConfiguration(
					(StreamConfiguration.StreamType) inputType.getSelectedItem(), inputType.getSelectedItem() ==
					StreamConfiguration.StreamType.CUSTOM ? inputFileName.getText() : null) : null,
					options.shouldProvideOutputType() ? new StreamConfiguration(
					(StreamConfiguration.StreamType) outputType.getSelectedItem(), outputType.getSelectedItem() ==
					StreamConfiguration.StreamType.CUSTOM ? outputFileName.getText() : null) : null,
					options.shouldProvideHeapMemory() ? heapMemory.getText() : null,
					options.shouldProvideStackMemory() ? stackMemory.getText() : null, project);
				List<Task> tasks = new ArrayList<Task>();
				for (String id : taskIDs) {
					Task task = taskParser.parse(id, predefined);
					if (task != null)
						tasks.add(task);
				}
				result = tasks;
				setVisible(false);
			}
		});
		parserCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (parserCombo.getSelectedItem() == selected)
					return;
				selected = (Parser) parserCombo.getSelectedItem();
				TaskOptions options = selected.getOptions();
				testTypePanel.setVisible(options.shouldProvideTestType());
				inputTypePanel.setVisible(options.shouldProvideInputType());
				inputFileNamePanel.setVisible(options.shouldProvideInputType() &&
					inputType.getSelectedItem() == StreamConfiguration.StreamType.CUSTOM);
				outputTypePanel.setVisible(options.shouldProvideOutputType());
				outputFileNamePanel.setVisible(options.shouldProvideOutputType() &&
					outputType.getSelectedItem() == StreamConfiguration.StreamType.CUSTOM);
				heapMemoryPanel.setVisible(options.shouldProvideHeapMemory());
				stackMemoryPanel.setVisible(options.shouldProvideStackMemory());
				pack();
			}
		});
		inputType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				inputFileNamePanel.setVisible(inputType.getSelectedItem() == StreamConfiguration.StreamType.CUSTOM);
				pack();
			}
		});
		outputType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				outputFileNamePanel.setVisible(outputType.getSelectedItem() == StreamConfiguration.StreamType.CUSTOM);
				pack();
			}
		});
		okCancelPanel.add(ok);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		okCancelPanel.add(cancel);
		contentPanel.add(okCancelPanel);
		setContentPane(contentPanel);
		parserCombo.setSelectedIndex(0);
		pack();
		Point center = Utilities.getLocation(project, contentPanel.getSize());
		setLocation(center);
		setVisible(true);
	}

	private JPanel createPanel(String label, JComponent component) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(label), BorderLayout.NORTH);
		panel.add(component, BorderLayout.CENTER);
		return panel;
	}

	public static Collection<Task> parseContest(Project project) {
		ParseDialog dialog = new ParseDialog(project, true);
		return dialog.result;
	}

	public static Task parseTask(Project project) {
		ParseDialog dialog = new ParseDialog(project, false);
		if (dialog.result.isEmpty())
			return null;
		return dialog.result.iterator().next();
	}
}
