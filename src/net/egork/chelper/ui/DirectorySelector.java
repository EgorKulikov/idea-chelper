package net.egork.chelper.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import net.egork.chelper.util.FileUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class DirectorySelector extends JPanel {
    private final JTextField textField;
    private JButton button;

    public DirectorySelector(final Project project, String initialValue) {
        this(project, initialValue, false);
    }

    public DirectorySelector(final Project project, String initialValue, final boolean allowAllDirectories) {
        super(new BorderLayout());
        textField = new JTextField(initialValue);
        button = new JButton("...") {
            @Override
            public Dimension getPreferredSize() {
                Dimension dimension = super.getPreferredSize();
                //noinspection SuspiciousNameCombination
                dimension.width = dimension.height;
                return dimension;
            }
        };
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PathChooserDialog dialog = FileChooserFactory.getInstance().createPathChooser(new FileChooserDescriptor(false, true, false, false, false, false) {
                    @Override
                    public boolean isFileSelectable(VirtualFile file) {
                        return super.isFileSelectable(file) && (allowAllDirectories || FileUtilities.isChild(project.getBaseDir(), file));
                    }

                    @Override
                    public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                        return super.isFileVisible(file, showHiddenFiles) && (allowAllDirectories || (FileUtilities.isChild(project.getBaseDir(), file) || FileUtilities.isChild(file, project.getBaseDir())));
                    }
                }, project, DirectorySelector.this);
                VirtualFile toSelect = allowAllDirectories ? VfsUtil.findFileByIoFile(new File(textField.getText()), false) : project.getBaseDir().findFileByRelativePath(textField.getText());
                if (toSelect == null) {
                    toSelect = project.getBaseDir();
                }
                dialog.choose(toSelect, new Consumer<List<VirtualFile>>() {
                    public void consume(List<VirtualFile> virtualFiles) {
                        if (virtualFiles.size() == 1) {
                            String path = allowAllDirectories ? virtualFiles.get(0).getPath() : FileUtilities.getRelativePath(project.getBaseDir(), virtualFiles.get(0));
                            if (path != null) {
                                textField.setText(path);
                            }
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
