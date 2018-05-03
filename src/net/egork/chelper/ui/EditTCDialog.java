package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.Utilities;

import javax.swing.*;
import java.awt.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class EditTCDialog extends JDialog {
    private TopCoderTask task;
    private boolean isOk = false;
    private TopCoderTaskPanel panel;

    public EditTCDialog(TopCoderTask task, Project project) {
        super(null, task.name, ModalityType.APPLICATION_MODAL);
        setIconImage(Utilities.iconToImage(IconLoader.getIcon("/icons/topcoder.png")));
        setAlwaysOnTop(true);
        setResizable(false);
        this.task = task;
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        OkCancelPanel main = new OkCancelPanel(new BorderLayout()) {
            @Override
            public void onOk() {
                isOk = true;
                EditTCDialog.this.task = panel.getTask();
                EditTCDialog.this.setVisible(false);
            }

            @Override
            public void onCancel() {
                EditTCDialog.this.task = null;
                EditTCDialog.this.setVisible(false);
            }
        };
        buttonPanel.add(main.getOkButton());
        buttonPanel.add(main.getCancelButton());
        panel = new TopCoderTaskPanel(project, task);
        main.add(panel, BorderLayout.CENTER);
        main.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(main);
        pack();
        Point center = Utilities.getLocation(project, main.getSize());
        setLocation(center);
    }

    public static TopCoderTask show(Project project, TopCoderTask task) {
        EditTCDialog dialog = new EditTCDialog(task, project);
        dialog.setVisible(true);
        if (dialog.isOk) {
            return dialog.task;
        } else {
            return task;
        }
    }
}
