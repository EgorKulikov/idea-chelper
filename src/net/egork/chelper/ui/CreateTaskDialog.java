package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiDirectory;
import net.egork.chelper.ProjectData;
import net.egork.chelper.task.Task;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.TaskUtilities;
import net.egork.chelper.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CreateTaskDialog extends JDialog {
    private Task task;
    private boolean isOk = false;
    private JButton basicAdvanced;
    private TaskConfigurationPanel panel;

    public CreateTaskDialog(Task task, boolean isNewTask, Project project) {
        super(null, "Task", ModalityType.APPLICATION_MODAL);
        setIconImage(Utilities.iconToImage(IconLoader.getIcon("/icons/newTask.png")));
        setAlwaysOnTop(true);
        setResizable(false);
        this.task = task;
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));
        basicAdvanced = new JButton("Advanced");
        OkCancelPanel main = new OkCancelPanel(new BorderLayout()) {
            @Override
            public void onOk() {
                isOk = true;
                CreateTaskDialog.this.task = panel.getTask();
                CreateTaskDialog.this.setVisible(false);
            }

            @Override
            public void onCancel() {
                CreateTaskDialog.this.task = null;
                CreateTaskDialog.this.setVisible(false);
            }
        };
        buttonPanel.add(basicAdvanced);
        buttonPanel.add(main.getOkButton());
        buttonPanel.add(main.getCancelButton());
        basicAdvanced.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                panel.setAdvancedVisibility(!panel.isAdvancedVisible());
                basicAdvanced.setText(panel.isAdvancedVisible() ? "Basic" : "Advanced");
                pack();
            }
        });
        panel = new TaskConfigurationPanel(task, isNewTask, project, new TaskConfigurationPanel.SizeChangeListener() {
            public void onSizeChanged() {
                pack();
            }
        }, buttonPanel);
        panel.setAdvancedVisibility(false);
        main.add(panel, BorderLayout.CENTER);
        setContentPane(main);
        pack();
        Point center = Utilities.getLocation(project, main.getSize());
        setLocation(center);
    }

    public static Task showDialog(PsiDirectory directory, String defaultName, Task template, boolean isNewTask) {
        Task defaultTask = template == null ? Utilities.getDefaultTask() : template;
        String name = defaultName == null ? "Task" : defaultName;
        Project project = directory.getProject();
        String location = FileUtilities.getRelativePath(project.getBaseDir(), directory.getVirtualFile());
        ProjectData data = Utilities.getData(project);
        if (defaultTask.inputClass == null) {
            defaultTask = defaultTask.setInputOutputClasses(data.inputClass, data.outputClass).
                    setFailOnIntegerOverflow(data.failOnIntegerOverflowForNewTasks);
        }
        if (defaultTask.date == null) {
            defaultTask = defaultTask.setDate(Task.getDateString());
        }
        Task task = new Task(name, defaultTask.testType, defaultTask.input, defaultTask.output, defaultTask.tests, location,
                defaultTask.vmArgs, defaultTask.mainClass, defaultTask.taskClass == null ? TaskUtilities.createClassName(name) : defaultTask.taskClass,
                defaultTask.checkerClass, defaultTask.checkerParameters, defaultTask.testClasses,
                defaultTask.date, defaultTask.contestName, defaultTask.truncate, defaultTask.inputClass,
                defaultTask.outputClass, defaultTask.includeLocale, defaultTask.failOnOverflow, defaultTask.template,
                defaultTask.interactive, defaultTask.interactor);
        CreateTaskDialog dialog = new CreateTaskDialog(task, isNewTask, project);
        dialog.setVisible(true);
        Utilities.updateDefaultTask(dialog.task);
        if (dialog.task != null) {
            dialog.task = dialog.task.setTaskClass(FileUtilities.createIfNeeded(dialog.task, dialog.task.taskClass, project, dialog.task.location));
        }
        return dialog.task;
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            JTextField taskName = panel.getNameField();
            taskName.requestFocusInWindow();
            taskName.setSelectionStart(0);
            taskName.setSelectionEnd(taskName.getText().length());
        } else if (!isOk) {
            task = null;
        }
        super.setVisible(b);
    }
}
