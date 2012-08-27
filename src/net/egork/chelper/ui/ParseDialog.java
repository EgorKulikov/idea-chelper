package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBList;
import net.egork.chelper.ProjectData;
import net.egork.chelper.parser.Description;
import net.egork.chelper.parser.DescriptionReceiver;
import net.egork.chelper.parser.Parser;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Utilities;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class ParseDialog extends JDialog {
	private Collection<Task> result = Collections.emptyList();
    private JBList contestList;
    private JBList taskList;
    private JComboBox parserCombo;
    private JComboBox testType;
    private DirectorySelector location;
    private JTextField mainClass;
    private JTextField date;
    private JTextField contestName;
    private JCheckBox truncate;
    private ParseListModel contestModel;
    private ParseListModel taskModel;

    private ParseDialog(final Project project) {
		super(null, "Parse Contest", ModalityType.APPLICATION_MODAL);
        setIconImage(Utilities.iconToImage(IconLoader.getIcon("/icons/parseContest.png")));
        ProjectData data = Utilities.getData(project);
        OkCancelPanel contentPanel = new OkCancelPanel(new BorderLayout(5, 5)) {
            @Override
            public void onOk() {
                List<Task> list = new ArrayList<Task>();
                Object[] tasks = taskList.getSelectedValues();
                Parser parser = (Parser) parserCombo.getSelectedItem();
                ProjectData data = Utilities.getData(project);
                for (Object taskDescription : tasks) {
                    Description description = (Description) taskDescription;
                    Task raw = parser.parseTask(description.id);
                    Task task = new Task(raw.name, (TestType)testType.getSelectedItem(), raw.input, raw.output,
                            raw.tests, location.getText(), raw.vmArgs, mainClass.getText(),
                            FileUtilities.createIfNeeded(raw.taskClass, project, location.getText()), raw.checkerClass,
                            raw.checkerParameters, raw.testClasses, date.getText(), contestName.getText(),
                            truncate.isSelected(), data.inputClass, data.outputClass);
                    list.add(task);
                }
                result = list;
                ParseDialog.this.setVisible(false);
                Utilities.setDefaultParser(parser);
            }

            @Override
            public void onCancel() {
                ParseDialog.this.setVisible(false);
            }
        };
        JPanel upperPanel = new JPanel(new BorderLayout(5, 5));
		parserCombo = new JComboBox(Parser.PARSERS);
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
        parserCombo.setSelectedItem(Utilities.getDefaultParser());
        parserCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
        upperPanel.add(parserCombo, BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
        upperPanel.add(refresh, BorderLayout.EAST);
        contentPanel.add(upperPanel, BorderLayout.NORTH);
        JPanel middlePanel = new JPanel(new GridLayout(1, 2, 5, 5)) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.height = 300;
                return size;
            }
        };
        contestModel = new ParseListModel();
        contestList = new JBList(contestModel);
        contestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contestList.setLayoutOrientation(JList.VERTICAL);
        contestList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                Parser parser = (Parser) parserCombo.getSelectedItem();
                parser.stopAdditionalTaskSending();
                Description contest = (Description) contestList.getSelectedValue();
                if (contest == null) {
                    contestName.setText("");
                    return;
                }
                Collection<Description> tasks = parser.parseContest(contest.id, new DescriptionReceiver() {
                    public void receiveAdditionalDescriptions(final Collection<Description> descriptions) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                taskModel.add(descriptions);
                            }
                        });
                    }
                });
                taskModel.removeAll();
                taskModel.add(tasks);
                int[] indices = new int[tasks.size()];
                for (int i = 0; i < indices.length; i++)
                    indices[i] = i;
                taskList.setSelectedIndices(indices);
                contestName.setText(contest.description);
            }
        });
        JScrollPane contestScroll = new JScrollPane(contestList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        middlePanel.add(contestScroll);
        taskModel = new ParseListModel();
        taskList = new JBList(taskModel);
        taskList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        taskList.setLayoutOrientation(JList.VERTICAL);
        JScrollPane taskScroll = new JScrollPane(taskList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        middlePanel.add(taskScroll);
        contentPanel.add(middlePanel, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        JPanel leftPanel = new JPanel(new VerticalFlowLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = 250;
                return size;
            }
        };
        Task defaultTask = Utilities.getDefaultTask();
        leftPanel.add(new JLabel("Test type:"));
        testType = new JComboBox(TestType.values());
        testType.setSelectedItem(defaultTask.testType);
        leftPanel.add(testType);
        leftPanel.add(new JLabel("Location:"));
        location = new DirectorySelector(project, data.defaultDirectory);
        leftPanel.add(location);
        leftPanel.add(new JLabel("Main class name:"));
        mainClass = new JTextField(defaultTask.mainClass);
        leftPanel.add(mainClass);
        truncate = new JCheckBox("Truncate long tests", defaultTask.truncate);
        leftPanel.add(truncate);
        bottomPanel.add(leftPanel);
        JPanel rightPanel = new JPanel(new BorderLayout());
        JPanel dateAndContestName = new JPanel(new VerticalFlowLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = 250;
                return size;
            }
        };
        dateAndContestName.add(new JLabel("Date:"));
        date = new JTextField(Task.getDateString());
        dateAndContestName.add(date);
        dateAndContestName.add(new JLabel("Contest name:"));
        contestName = new JTextField();
        dateAndContestName.add(contestName);
        rightPanel.add(dateAndContestName, BorderLayout.NORTH);
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(contentPanel.getOkButton(), BorderLayout.CENTER);
        buttonPanel.add(contentPanel.getCancelButton(), BorderLayout.EAST);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);
        bottomPanel.add(rightPanel);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(contentPanel);
        refresh();
		pack();
		Point center = Utilities.getLocation(project, contentPanel.getSize());
		setLocation(center);
		setVisible(true);
	}

    private void refresh() {
        Parser parser = (Parser) parserCombo.getSelectedItem();
        parser.stopAdditionalContestSending();
        Description description = (Description) contestList.getSelectedValue();
        contestModel.removeAll();
        contestName.setText("");
        Collection<Description> contests = parser.getContests(new DescriptionReceiver() {
            public void receiveAdditionalDescriptions(final Collection<Description> descriptions) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        contestModel.add(descriptions);
                    }
                });
            }
        });
        contestModel.add(contests);
        if (!contests.isEmpty()) {
            int toSelect = 0;
            if (description != null) {
                int index = 0;
                for (Description contest : contests) {
                    if (contest.id.equals(description.id)) {
                        toSelect = index;
                        break;
                    }
                    index++;
                }
            }
            contestList.setSelectedIndex(toSelect);
        }
        pack();
    }

    public static Collection<Task> parseContest(Project project) {
		ParseDialog dialog = new ParseDialog(project);
		return dialog.result;
	}

    private class ParseListModel extends AbstractListModel {
        private List<Description> list = new ArrayList<Description>();

        public int getSize() {
            return list.size();
        }

        public Object getElementAt(int index) {
            return list.get(index);
        }

        public void removeAll() {
            int size = getSize();
            if (size == 0)
                return;
            list.clear();
            fireIntervalRemoved(this, 0, size - 1);
        }

        public void add(Collection<Description> collection) {
            if (collection.isEmpty())
                return;
            int size = getSize();
            list.addAll(collection);
            fireIntervalAdded(this, size, getSize() - 1);
        }
    }
}
