package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.util.FileCreator;
import net.egork.chelper.util.Provider;
import net.egork.chelper.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class TestClassesDialog extends JDialog {
    private String[] testClasses;
    private boolean isOk = false;
    private List<TestClassPanel> panels = new ArrayList<TestClassPanel>();
    private JPanel classesPanel;
    private FileCreator fileCreator;
    private final int width = new JTextField(20).getPreferredSize().width;

    public TestClassesDialog(String[] testClasses, final Project project, final String location, FileCreator fileCreator, final String baseName) {
        super(null, "Test classes", ModalityType.APPLICATION_MODAL);
        this.fileCreator = fileCreator;
        setIconImage(Utilities.iconToImage(IconLoader.getIcon("/icons/check.png")));
        setAlwaysOnTop(true);
        setResizable(false);
        this.testClasses = testClasses;
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        OkCancelPanel main = new OkCancelPanel(new BorderLayout()) {
            @Override
            public void onOk() {
                isOk = true;
                TestClassesDialog.this.testClasses = getTestClasses();
                TestClassesDialog.this.setVisible(false);
            }

            @Override
            public void onCancel() {
                TestClassesDialog.this.setVisible(false);
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension dimension = super.getPreferredSize();
                dimension.width = width;
                return dimension;
            }
        };
        buttonPanel.add(main.getOkButton());
        buttonPanel.add(main.getCancelButton());
        classesPanel = new JPanel(new VerticalFlowLayout());
        for (String testClass : testClasses)
            panels.add(new TestClassPanel(testClass, project, location));
        JButton add = new JButton("Add");
        buttonPanel.add(add, BorderLayout.WEST);
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                panels.add(new TestClassPanel(baseName + "TestCase" + panels.size(), project, location));
                rebuild();
            }
        });
        main.add(classesPanel, BorderLayout.CENTER);
        main.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(main);
        rebuild();
        Point center = Utilities.getLocation(project, main.getSize());
        setLocation(center);
    }

    private String[] getTestClasses() {
        String[] testClasses = new String[panels.size()];
        for (int i = 0; i < testClasses.length; i++)
            testClasses[i] = panels.get(i).getText();
        return testClasses;
    }

    public static String[] showDialog(String[] testClasses, Project project, String location, FileCreator fileCreator, String baseName) {
        TestClassesDialog dialog = new TestClassesDialog(testClasses, project, location, fileCreator, baseName);
        dialog.setVisible(true);
        return dialog.testClasses;
    }

    private class TestClassPanel extends JPanel {
        private SelectOrCreateClass selector;

        private TestClassPanel(String testClass, Project project, final String location) {
            super(new BorderLayout());
            selector = new SelectOrCreateClass(testClass, project, new Provider<String>() {
                public String provide() {
                    return location;
                }
            }, fileCreator);
            JButton remove = new JButton("Remove");
            remove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TestClassesDialog.this.removeTest(TestClassPanel.this);
                }
            });
            add(selector, BorderLayout.CENTER);
            add(remove, BorderLayout.EAST);
        }

        public String getText() {
            return selector.getText();
        }
    }

    private void removeTest(TestClassPanel testClassPanel) {
        panels.remove(testClassPanel);
        rebuild();
    }

    private void rebuild() {
        classesPanel.removeAll();
        for (TestClassPanel panel : panels)
            classesPanel.add(panel);
        pack();
    }
}
