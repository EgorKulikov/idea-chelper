package net.egork.chelper.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import net.egork.chelper.util.FileUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class DirectorySelector extends JPanel {
    private final JTextField textField;
    private JButton button;

    public DirectorySelector(final Project project, String initialValue) {
        super(new BorderLayout());
        textField = new JTextField(initialValue);
        button = new JButton("...");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PathChooserDialog dialog = FileChooserFactory.getInstance().createPathChooser(new FileChooserDescriptor(false, true, false, false, false, false) {
                    @Override
                    public boolean isFileSelectable(VirtualFile file) {
                        return super.isFileSelectable(file) && FileUtilities.isChild(project.getBaseDir(), file);
                    }

                    @Override
                    public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                        return super.isFileVisible(file, showHiddenFiles) && (FileUtilities.isChild(project.getBaseDir(), file) || FileUtilities.isChild(file, project.getBaseDir()));
                    }
                }, project, DirectorySelector.this);
                VirtualFile toSelect = project.getBaseDir().findFileByRelativePath(textField.getText());
                if (toSelect == null)
                    toSelect = project.getBaseDir();
                dialog.choose(toSelect, new Consumer<List<VirtualFile>>() {
                    public void consume(List<VirtualFile> virtualFiles) {
                        if (virtualFiles.size() == 1) {
                            String path = FileUtilities.getRelativePath(project.getBaseDir(), virtualFiles.get(0));
                            if (path != null)
                                textField.setText(path);
                        }
                    }
                });
            }
        });
        add(textField, BorderLayout.CENTER);
        add(button, BorderLayout.EAST);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        button.setEnabled(enabled);
    }

    public String getText() {
        return textField.getText();
    }
}
