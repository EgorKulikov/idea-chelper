package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;
import net.egork.chelper.util.FileCreator;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Provider;
import net.egork.chelper.util.Utilities;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class TaskConfigurationPanel extends JPanel {
    private JPanel basic;
    private JPanel advanced;
    private Task task;
    private Project project;

    //basic
    private JTextField name;
    private JComboBox testType;
    private JComboBox inputType;
    private JTextField inputFileName;
    private JComboBox outputType;
    private JTextField outputFileName;
    private JButton tests;

    //advanced
    private DirectorySelector location;
    private JTextField vmArgs;
    private JCheckBox failOnOverflow;
    private JTextField mainClass;
    private SelectOrCreateClass taskClass;
    private SelectOrCreateClass checkerClass;
    private JTextField checkerParameters;
    private JCheckBox hasTestCase;
    private SelectOrCreateClass testClass;
    private JTextField date;
    private JTextField contestName;
    private JCheckBox truncate;
	private int panelWidth = new JTextField(27).getPreferredSize().width;
	private JCheckBox includeLocale;

    public TaskConfigurationPanel(final Task task, boolean firstEdit, final Project project, final SizeChangeListener listener, JPanel buttonPanel) {
        super(new BorderLayout(5, 5));
        this.task = task;
        this.project = project;
        basic = new JPanel(new VerticalFlowLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = panelWidth;
                return size;
            }
        };
        basic.add(new JLabel("Name:"));
        name = new JTextField(task.name);
        name.setEnabled(firstEdit);
		name.getDocument().addDocumentListener(new DocumentListener() {
			String lastText = name.getText();

			public void insertUpdate(DocumentEvent e) {
				update();
			}

			public void removeUpdate(DocumentEvent e) {
				update();
			}

			public void changedUpdate(DocumentEvent e) {
				update();
			}

			private void update() {
				if (name.isEnabled() && taskClass.getText().equals(lastText))
					taskClass.setText(name.getText());
				lastText = name.getText();
			}
		});
        basic.add(name);
        basic.add(new JLabel("Contest name:"));
        contestName = new JTextField(task.contestName);
        basic.add(contestName);
        basic.add(new JLabel("Test type:"));
        testType = new JComboBox(TestType.values());
        testType.setSelectedItem(task.testType);
        basic.add(testType);
        basic.add(new JLabel("Input:"));
        inputType = new JComboBox(StreamConfiguration.StreamType.values());
        inputType.setSelectedItem(task.input.type);
        inputType.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				inputFileName.setVisible(((StreamConfiguration.StreamType)inputType.getSelectedItem()).hasStringParameter);
                if (listener != null)
                    listener.onSizeChanged();
            }
        });
        basic.add(inputType);
        inputFileName = new JTextField(task.input.type.hasStringParameter ? task.input.fileName :
                "input.txt");
        inputFileName.setVisible(task.input.type.hasStringParameter);
        basic.add(inputFileName);
        basic.add(new JLabel("Output:"));
        outputType = new JComboBox(StreamConfiguration.OUTPUT_TYPES);
        outputType.setSelectedItem(task.output.type);
        outputType.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                outputFileName.setVisible(((StreamConfiguration.StreamType)outputType.getSelectedItem()).hasStringParameter);
                if (listener != null)
                    listener.onSizeChanged();
            }
        });
        basic.add(outputType);
        outputFileName = new JTextField(task.output.type.hasStringParameter ?
                task.output.fileName : "output.txt");
        outputFileName.setVisible(task.output.type.hasStringParameter);
        basic.add(outputFileName);
        tests = new JButton("Edit tests");
        tests.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TaskConfigurationPanel.this.task = TaskConfigurationPanel.this.task.setTests(
                        EditTestsDialog.editTests(TaskConfigurationPanel.this.task.tests, TaskConfigurationPanel.this.project));
                name.setText(name.getText());
            }
        });
        basic.add(tests);
		if (buttonPanel != null)
        	basic.add(buttonPanel);
        JPanel leftAdvanced = new JPanel(new VerticalFlowLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = panelWidth;
                return size;
            }
        };
        leftAdvanced.add(new JLabel("Location:"));
        location = new DirectorySelector(project, task.location);
        location.setEnabled(firstEdit);
        leftAdvanced.add(location);
        leftAdvanced.add(new JLabel("Main class name:"));
        mainClass = new JTextField(task.mainClass);
        leftAdvanced.add(mainClass);
        leftAdvanced.add(new JLabel("Task class:"));
        Provider<String> locationProvider = new Provider<String>() {
            public String provide() {
                return location.getText();
            }
        };
        taskClass = new SelectOrCreateClass(task.taskClass, project, locationProvider, new FileCreator() {
            public String createFile(Project project, String path, String name) {
                return FileUtilities.createTaskClass(task, project, path, name);
            }

            public boolean isValid(String name) {
                return FileUtilities.isValidClassName(name);
            }
        });
        leftAdvanced.add(taskClass);
        leftAdvanced.add(new JLabel("Checker class:"));
        checkerClass = new SelectOrCreateClass(task.checkerClass, project, locationProvider, new FileCreator() {
            public String createFile(Project project, String path, String name) {
                return FileUtilities.createCheckerClass(project, path, name, task);
            }

            public boolean isValid(String name) {
                return FileUtilities.isValidClassName(name);
            }
        });
        leftAdvanced.add(checkerClass);
        leftAdvanced.add(new JLabel("Checker parameters:"));
        checkerParameters = new JTextField(task.checkerParameters);
        leftAdvanced.add(checkerParameters);
        JPanel rightAdvanced = new JPanel(new VerticalFlowLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = panelWidth;
                return size;
            }
        };
        rightAdvanced.add(new JLabel("VM arguments:"));
        vmArgs = new JTextField(task.vmArgs);
        rightAdvanced.add(vmArgs);
        failOnOverflow = new JCheckBox("Fail on integer overflow");
        failOnOverflow.setSelected(task.failOnOverflow);
        rightAdvanced.add(failOnOverflow);
        rightAdvanced.add(new JLabel("Test class:"));
        JPanel testClassPanel = new JPanel(new BorderLayout());
        hasTestCase = new JCheckBox();
        hasTestCase.setSelected(task.testClasses.length != 0);
        testClassPanel.add(hasTestCase, BorderLayout.WEST);
        testClass = new SelectOrCreateClass(task.testClasses.length != 0 ? task.testClasses[0] : (Utilities.getSimpleName(task.taskClass) + "TestCase"), project, new Provider<String>() {
            public String provide() {
                return task.location;
            }
        }, new FileCreator() {
            public String createFile(Project project, String path, String name) {
                return FileUtilities.createTestClass(project, path, name, task);
            }

            public boolean isValid(String name) {
                return FileUtilities.isValidClassName(name);
            }
        });
        testClass.setEnabled(hasTestCase.isSelected());
        testClassPanel.add(testClass, BorderLayout.CENTER);
        rightAdvanced.add(testClassPanel);
        hasTestCase.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                testClass.setEnabled(hasTestCase.isSelected());
            }
        });
        rightAdvanced.add(new JLabel("Date:"));
        date = new JTextField(task.date);
        rightAdvanced.add(date);
        truncate = new JCheckBox("Truncate long tests", task.truncate);
        rightAdvanced.add(truncate);
		includeLocale = new JCheckBox("Force locale", task.includeLocale);
		rightAdvanced.add(includeLocale);
        advanced = new JPanel(new GridLayout(1, 2, 5, 5));
        advanced.add(leftAdvanced);
        advanced.add(rightAdvanced);
        JPanel advancedWrapper = new JPanel(new BorderLayout());
        advancedWrapper.add(advanced, BorderLayout.WEST);
        add(basic, BorderLayout.WEST);
        add(advancedWrapper, BorderLayout.CENTER);
    }

    public void setAdvancedVisibility(boolean visibility) {
        advanced.setVisible(visibility);
    }

    public boolean isAdvancedVisible() {
        return advanced.isVisible();
    }

    public Task getTask() {
        return task = new Task(name.getText(), (TestType)testType.getSelectedItem(),
            new StreamConfiguration((StreamConfiguration.StreamType) inputType.getSelectedItem(), inputFileName.getText()),
            new StreamConfiguration((StreamConfiguration.StreamType) outputType.getSelectedItem(), outputFileName.getText()),
            task.tests, location.getText(), vmArgs.getText(), mainClass.getText(),
            taskClass.getText(), checkerClass.getText(), checkerParameters.getText(), getTestClass(),
            date.getText(), contestName.getText(), truncate.isSelected(), task.inputClass, task.outputClass,
			includeLocale.isSelected(), failOnOverflow.isSelected());
    }

    private String[] getTestClass() {
        if (hasTestCase.isSelected())
            return new String[]{testClass.getText()};
        else
            return new String[0];
    }

    public interface SizeChangeListener {
        public void onSizeChanged();
    }

    public JTextField getNameField() {
        return name;
    }
}
