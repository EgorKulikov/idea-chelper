package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import net.egork.chelper.task.Test;
import net.egork.chelper.util.FileUtilities;

import javax.swing.*;
import java.io.File;
import java.util.*;

/**
 * @author egorku@yandex-team.ru
 */
public class BulkAddTestsPanel extends JPanel {
    private final Project project;
    private JTextField inputExtension = new JTextField("in");
    private JTextField answerExtension = new JTextField("out");
    private DirectorySelector directorySelector;

    public BulkAddTestsPanel(final Project project) {
        super(new VerticalFlowLayout());
        this.project = project;
        add(new JLabel("Input files extension:"));
        add(inputExtension);
        add(new JLabel("Answer files extension:"));
        add(answerExtension);
        add(new JLabel("Directory:"));
        directorySelector = new DirectorySelector(project, project.getBaseDir().getPath(), true);
        add(directorySelector);
    }

    public List<Test> getTests() {
        VirtualFile directory = VfsUtil.findFileByIoFile(new File(directorySelector.getText()), false);
        if (directory == null || !directory.isDirectory()) {
            return Collections.emptyList();
        }
        List<Test> tests = new ArrayList<Test>();
        Map<String, VirtualFile> inputs = new HashMap<String, VirtualFile>();
        for (VirtualFile file : directory.getChildren()) {
            if (!file.isDirectory() && inputExtension.getText().equals(file.getExtension())) {
                inputs.put(file.getNameWithoutExtension(), file);
            }
        }
        for (VirtualFile file : directory.getChildren()) {
            if (!file.isDirectory() && answerExtension.getText().equals(file.getExtension()) && inputs.containsKey(file.getNameWithoutExtension())) {
                VirtualFile inputFile = inputs.get(file.getNameWithoutExtension());
                String input = FileUtilities.readTextFile(inputFile);
                String answer = FileUtilities.readTextFile(file);
                tests.add(new Test(input, answer));
            }
        }
        return tests;
    }
}
