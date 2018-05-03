package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.FileCreator;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Provider;
import net.egork.chelper.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author egorku@yandex-team.ru
 */
public class TopCoderTaskPanel extends JPanel {
    private TopCoderTask task;

    private JTextField date = new JTextField();
    private JTextField contestName = new JTextField();
    private JCheckBox hasTestCase = new JCheckBox();
    private SelectOrCreateClass testClass;
    private JCheckBox failOnOverflow = new JCheckBox("Fail on integer overflow");

    public TopCoderTaskPanel(final Project project, TopCoderTask task) {
        super(new VerticalFlowLayout());
        this.task = task;
        add(new JLabel("Date:"));
        add(date);
        date.setText(task.date);
        add(new JLabel("Contest name:"));
        add(contestName);
        contestName.setText(task.contestName);
        JButton editTests = new JButton("Edit Tests");
        editTests.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refresh();
                TopCoderTaskPanel.this.task = TopCoderTaskPanel.this.task.setTests(TopCoderEditTestsDialog.editTests(
                        TopCoderTaskPanel.this.task, project));
            }
        });
        add(editTests);
        add(new JLabel("Test class:"));
        JPanel testClassPanel = new JPanel(new BorderLayout());
        hasTestCase.setSelected(task.testClasses.length != 0);
        testClassPanel.add(hasTestCase, BorderLayout.WEST);
        testClass = new SelectOrCreateClass(task.testClasses.length != 0 ? task.testClasses[0] : (task.name + "TestCase"), project, new Provider<String>() {
            public String provide() {
                return Utilities.getData(project).defaultDirectory;
            }
        }, new FileCreator() {
            public String createFile(Project project, String path, String name) {
                return FileUtilities.createTopCoderTestClass(project, path, name);
            }

            public boolean isValid(String name) {
                return FileUtilities.isValidClassName(name);
            }
        });
        testClass.setEnabled(hasTestCase.isSelected());
        hasTestCase.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                testClass.setEnabled(hasTestCase.isSelected());
            }
        });
        testClassPanel.add(testClass, BorderLayout.CENTER);
        add(testClassPanel);
        add(failOnOverflow);
        failOnOverflow.setSelected(task.failOnOverflow);
    }

    private void refresh() {
        task = new TopCoderTask(task.name, task.signature, this.task.tests, date.getText(), contestName.getText(),
                hasTestCase.isSelected() ? new String[]{testClass.getText()} : new String[0], task.fqn,
                failOnOverflow.isSelected(), task.memoryLimit);
    }

    public TopCoderTask getTask() {
        refresh();
        return task;
    }
}
