package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.VerticalFlowLayout;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;
import net.egork.chelper.util.*;

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
    private ComboBox<TestType> testType;
    private ComboBox<StreamConfiguration.StreamType> inputType;
    private JTextField inputFileName;
    private ComboBox<StreamConfiguration.StreamType> outputType;
    private JTextField outputFileName;
    private JButton tests;
    private FileSelector template;

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
    private JPanel testInputOutputParameters;
    private JPanel interactorSettings;
    private JCheckBox isInteractive;
    private SelectOrCreateClass interactor;

    public TaskConfigurationPanel(final Task task, boolean isNewTask, final Project project, final SizeChangeListener listener, JPanel buttonPanel) {
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
        name.setEnabled(isNewTask);
        name.getDocument().addDocumentListener(new DocumentListener() {
            String lastText = TaskUtilities.createClassName(name.getText());

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
                if (name.isEnabled() && taskClass.getText().equals(lastText)) {
                    taskClass.setText(TaskUtilities.createClassName(name.getText()));
                }
                lastText = TaskUtilities.createClassName(name.getText());
            }
        });
        basic.add(name);
        basic.add(new JLabel("Contest name:"));
        contestName = new JTextField(task.contestName);
        basic.add(contestName);
        isInteractive = new JCheckBox("Interactive task", task.interactive);
        basic.add(isInteractive);
        testInputOutputParameters = new JPanel(new VerticalFlowLayout(0, 5));
        testInputOutputParameters.add(new JLabel("Test type:"));
        testType = new ComboBox<>(TestType.values());
        testType.setSelectedItem(task.testType);
        testInputOutputParameters.add(testType);
        testInputOutputParameters.add(new JLabel("Input:"));
        inputType = new ComboBox<>(StreamConfiguration.StreamType.values());
        inputType.setSelectedItem(task.input.type);
        inputType.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                inputFileName.setVisible(((StreamConfiguration.StreamType) inputType.getSelectedItem()).hasStringParameter);
                if (listener != null) {
                    listener.onSizeChanged();
                }
            }
        });
        testInputOutputParameters.add(inputType);
        inputFileName = new JTextField(task.input.type.hasStringParameter ? task.input.fileName :
                "input.txt");
        inputFileName.setVisible(task.input.type.hasStringParameter);
        testInputOutputParameters.add(inputFileName);
        testInputOutputParameters.add(new JLabel("Output:"));
        outputType = new ComboBox<>(StreamConfiguration.OUTPUT_TYPES);
        outputType.setSelectedItem(task.output.type);
        outputType.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                outputFileName.setVisible(((StreamConfiguration.StreamType) outputType.getSelectedItem()).hasStringParameter);
                if (listener != null) {
                    listener.onSizeChanged();
                }
            }
        });
        testInputOutputParameters.add(outputType);
        outputFileName = new JTextField(task.output.type.hasStringParameter ?
                task.output.fileName : "output.txt");
        outputFileName.setVisible(task.output.type.hasStringParameter);
        testInputOutputParameters.add(outputFileName);
        testInputOutputParameters.setVisible(!task.interactive);
        basic.add(testInputOutputParameters);
        interactorSettings = new JPanel(new VerticalFlowLayout(0, 5));
        interactorSettings.add(new JLabel("Interactor"));
        Provider<String> locationProvider = new Provider<String>() {
            public String provide() {
                return location.getText();
            }
        };
        interactor = new SelectOrCreateClass(task.interactor != null ? task.interactor : "net.egork.chelper.tester.Interactor", project, locationProvider, new FileCreator() {
            @Override
            public String createFile(Project project, String path, String name) {
                return FileUtilities.createInteractorClass(project, path, name, task);
            }

            @Override
            public boolean isValid(String name) {
                return FileUtilities.isValidClassName(name);
            }
        });
        interactorSettings.add(interactor);
        interactorSettings.setVisible(task.interactive);
        basic.add(interactorSettings);
        isInteractive.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                interactorSettings.setVisible(isInteractive.isSelected());
                testInputOutputParameters.setVisible(!isInteractive.isSelected());
                if (listener != null) {
                    listener.onSizeChanged();
                }
            }
        });
        template = new FileSelector(project, task.template, "template", false);
        if (isNewTask) {
            basic.add(new JLabel("Template:"));
            basic.add(template);
        }
        tests = new JButton("Edit tests");
        tests.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TaskConfigurationPanel.this.task = TaskConfigurationPanel.this.task.setTests(
                        EditTestsDialog.editTests(TaskConfigurationPanel.this.task.tests, TaskConfigurationPanel.this.project));
                name.setText(name.getText());
            }
        });
        basic.add(tests);
        if (buttonPanel != null) {
            basic.add(buttonPanel);
        }
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
        location.setEnabled(isNewTask);
        leftAdvanced.add(location);
        leftAdvanced.add(new JLabel("Main class name:"));
        mainClass = new JTextField(task.mainClass);
        leftAdvanced.add(mainClass);
        leftAdvanced.add(new JLabel("Task class:"));
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
        return task = new Task(name.getText(), (TestType) testType.getSelectedItem(),
                new StreamConfiguration((StreamConfiguration.StreamType) inputType.getSelectedItem(), inputFileName.getText()),
                new StreamConfiguration((StreamConfiguration.StreamType) outputType.getSelectedItem(), outputFileName.getText()),
                task.tests, location.getText(), vmArgs.getText(), mainClass.getText(),
                taskClass.getText(), checkerClass.getText(), checkerParameters.getText(), getTestClass(),
                date.getText(), contestName.getText(), truncate.isSelected(), task.inputClass, task.outputClass,
                includeLocale.isSelected(), failOnOverflow.isSelected(), template.getText(), isInteractive.isSelected(), interactor.getText());
    }

    private String[] getTestClass() {
        if (hasTestCase.isSelected()) {
            return new String[]{testClass.getText()};
        } else {
            return new String[0];
        }
    }

    public interface SizeChangeListener {
        public void onSizeChanged();
    }

    public JTextField getNameField() {
        return name;
    }
}
