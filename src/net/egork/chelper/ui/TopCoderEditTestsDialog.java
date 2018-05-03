package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import net.egork.chelper.task.MethodSignature;
import net.egork.chelper.task.NewTopCoderTest;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.Utilities;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderEditTestsDialog extends JDialog {
    private static int HEIGHT = new JLabel("Test").getPreferredSize().height;

    private List<NewTopCoderTest> tests;
    private int currentTest;
    private JBList testList;
    private JTextField[] arguments;
    private JTextField result;
    private JPanel testPanel;
    private List<JCheckBox> checkBoxes = new ArrayList<JCheckBox>();
    private JPanel checkBoxesPanel;
    private TopCoderTask task;
    private JCheckBox knowAnswer;
    private boolean updating;

    public TopCoderEditTestsDialog(TopCoderTask task, Project project) {
        super(null, "Tests", ModalityType.APPLICATION_MODAL);
        this.task = task;
        setAlwaysOnTop(true);
        setResizable(false);
        this.tests = new ArrayList<NewTopCoderTest>(Arrays.asList(task.tests));
        VariableGridLayout mainLayout = new VariableGridLayout(1, 2, 5, 5);
        mainLayout.setColFraction(0, 0.35);
        mainLayout.setColFraction(1, 0.65);
        JPanel mainPanel = new JPanel(mainLayout);
        JPanel selectorAndButtonsPanel = new JPanel(new BorderLayout());
        selectorAndButtonsPanel.add(new JLabel("Tests:"), BorderLayout.NORTH);
        JPanel checkBoxesAndSelectorPanel = new JPanel(new BorderLayout());
        checkBoxesPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, false, false));
        for (NewTopCoderTest test : tests) {
            JCheckBox checkBox = createCheckBox(test);
            checkBoxesPanel.add(checkBox);
        }
        checkBoxesAndSelectorPanel.add(checkBoxesPanel, BorderLayout.WEST);
        testList = new JBList(tests);
        testList.setFixedCellHeight(HEIGHT);
        testList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        testList.setLayoutOrientation(JList.VERTICAL);
        testList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (updating) {
                    return;
                }
                int index = testList.getSelectedIndex();
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
                NewTopCoderTest test = new NewTopCoderTest(new Object[TopCoderEditTestsDialog.this.arguments.length], null, index);
                TopCoderEditTestsDialog.this.tests.add(test);
                checkBoxesPanel.add(createCheckBox(test));
                setSelectedTest(index);
                knowAnswer.setSelected(true);
                result.setEnabled(true);
            }
        });
        middleButtonsPanel.add(newTest);
        JButton remove = new JButton("Remove");
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (currentTest == -1) {
                    return;
                }
                while (checkBoxes.size() > currentTest) {
                    checkBoxesPanel.remove(checkBoxes.get(currentTest));
                    checkBoxes.remove(currentTest);
                }
                TopCoderEditTestsDialog.this.tests.remove(currentTest);
                int size = TopCoderEditTestsDialog.this.tests.size();
                for (int i = currentTest; i < size; i++) {
                    NewTopCoderTest test = TopCoderEditTestsDialog.this.tests.get(i);
                    test = new NewTopCoderTest(test.arguments, test.result, i, test.active);
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
            testPanel.add(createPanel(task.signature.arguments[i] + " " +
                    task.signature.argumentNames[i], arguments[i]));
            final int finalI = i;
            arguments[i].getDocument().addDocumentListener(new DocumentListener() {
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
                    if (updating) {
                        return;
                    }
                    saveCurrentTest();
                    if (NewTopCoderTest.parse(arguments[finalI].getText(), MethodSignature.getClass(TopCoderEditTestsDialog.this.task.signature.arguments[finalI])) == null) {
                        arguments[finalI].setForeground(Color.RED);
                    } else {
                        arguments[finalI].setForeground(Color.BLACK);
                    }
                }
            });
        }
        testPanel.add(new JLabel("Result:"));
        knowAnswer = new JCheckBox("Know answer?");
        knowAnswer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveCurrentTest();
                result.setEnabled(knowAnswer.isSelected());
            }
        });
        testPanel.add(knowAnswer);
        result = new JTextField();
        result.getDocument().addDocumentListener(new DocumentListener() {
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
                if (updating) {
                    return;
                }
                saveCurrentTest();
                if (NewTopCoderTest.parse(result.getText(), MethodSignature.getClass(TopCoderEditTestsDialog.this.task.signature.result)) == null) {
                    result.setForeground(Color.RED);
                } else {
                    result.setForeground(Color.BLACK);
                }
            }
        });
        testPanel.add(createPanel(task.signature.result, result));
        mainPanel.add(new JBScrollPane(testPanel));
        setContentPane(mainPanel);
        setSelectedTest(Math.min(0, task.tests.length - 1));
        pack();
        setSize(600, 400);
        setLocation(Utilities.getLocation(project, this.getSize()));
    }

    private JPanel createPanel(String label, JTextField editor) {
        VariableGridLayout layout = new VariableGridLayout(1, 2);
        layout.setColFraction(0, 0.3);
        layout.setColFraction(1, 0.7);
        JPanel panel = new JPanel(layout) {
            @Override
            public Dimension getPreferredSize() {
                Dimension preferredSize = super.getPreferredSize();
                preferredSize.width = (int) Math.min(preferredSize.width, 600 * .65 - 10);
                return preferredSize;
            }
        };
        panel.add(new JLabel(label));
        panel.add(editor);
        return panel;
    }

    private JCheckBox createCheckBox(final NewTopCoderTest test) {
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
        updating = true;
        currentTest = index;
        if (index == -1) {
            testPanel.setVisible(false);
        } else {
            testPanel.setVisible(true);
            for (int i = 0; i < arguments.length; i++) {
                if (tests.get(index).arguments[i] == null) {
                    arguments[i].setText("");
                } else {
                    arguments[i].setText(NewTopCoderTest.toString(tests.get(index).arguments[i], MethodSignature.getClass(task.signature.arguments[i])));
                }
            }
            knowAnswer.setSelected(tests.get(index).result != null);
            result.setEnabled(knowAnswer.isSelected());
            if (tests.get(index).result != null) {
                result.setText(NewTopCoderTest.toString(tests.get(index).result, MethodSignature.getClass(task.signature.result)));
            } else {
                result.setText("");
            }
        }
        for (int i = 0; i < arguments.length; i++)
            arguments[i].setForeground(Color.BLACK);
        result.setForeground(Color.BLACK);
        testList.setListData(tests.toArray());
        testList.setSelectedIndex(currentTest);
        testList.repaint();
        checkBoxesPanel.repaint();
        updating = false;
    }

    private void saveCurrentTest() {
        if (currentTest == -1) {
            return;
        }
        Object[] arguments = new Object[this.arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            if ((arguments[i] = NewTopCoderTest.parse(this.arguments[i].getText(), MethodSignature.getClass(task.signature.arguments[i]))) == null) {
                return;
            }
        }
        Object result;
        if (knowAnswer.isSelected()) {
            result = NewTopCoderTest.parse(this.result.getText(), MethodSignature.getClass(task.signature.result));
            if (result == null) {
                return;
            }
        } else {
            result = null;
        }
        tests.set(currentTest, new NewTopCoderTest(arguments, result, currentTest,
                checkBoxes.get(currentTest).isSelected()));
    }

    public static NewTopCoderTest[] editTests(TopCoderTask task, Project project) {
        TopCoderEditTestsDialog dialog = new TopCoderEditTestsDialog(task, project);
        dialog.setVisible(true);
        return dialog.tests.toArray(new NewTopCoderTest[dialog.tests.size()]);
    }
}
