package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.task.Test;
import net.egork.chelper.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class BulkAddTestsDialog extends JDialog {
    private boolean isOk = false;
    private BulkAddTestsPanel panel;
    private List<Test> newTests = Collections.emptyList();

    public BulkAddTestsDialog(Project project) {
        super(null, "Bulk Add Tests", ModalityType.APPLICATION_MODAL);
        setIconImage(Utilities.iconToImage(IconLoader.getIcon("/icons/editTests.png")));
        setAlwaysOnTop(true);
        setResizable(false);
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        OkCancelPanel main = new OkCancelPanel(new BorderLayout()) {
            @Override
            public void onOk() {
                isOk = true;
                newTests = panel.getTests();
                BulkAddTestsDialog.this.setVisible(false);
            }

            @Override
            public void onCancel() {
                BulkAddTestsDialog.this.setVisible(false);
            }
        };
        buttonPanel.add(main.getOkButton());
        buttonPanel.add(main.getCancelButton());
        panel = new BulkAddTestsPanel(project);
        main.add(panel, BorderLayout.CENTER);
        main.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(main);
        pack();
        Point center = Utilities.getLocation(project, main.getSize());
        setLocation(center);
    }

    public static Test[] show(Project project, Test[] oldTests) {
        BulkAddTestsDialog dialog = new BulkAddTestsDialog(project);
        dialog.setVisible(true);
        if (dialog.isOk) {
            if (dialog.newTests.isEmpty()) {
                return oldTests;
            }
            List<Test> result = new ArrayList<Test>(Arrays.asList(oldTests));
            result.addAll(dialog.newTests);
            return result.toArray(new Test[result.size()]);
        } else {
            return oldTests;
        }
    }
}
