package net.egork.chelper.actions;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Utilities;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CopyAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        if (!Utilities.isEligible(e.getDataContext())) {
            return;
        }
        final Project project = Utilities.getProject(e.getDataContext());
        RunnerAndConfigurationSettings selectedConfiguration =
                RunManagerImpl.getInstanceImpl(project).getSelectedConfiguration();
        if (selectedConfiguration == null) {
            return;
        }
        RunConfiguration configuration = selectedConfiguration.getConfiguration();
        if (configuration instanceof TaskConfiguration) {
            Task task = ((TaskConfiguration) configuration).getConfiguration();
            VirtualFile file = FileUtilities.getFile(project, Utilities.getData(project).outputDirectory + "/" + task.mainClass + ".java");
            if (file == null) {
                return;
            }
            String content = FileUtilities.readTextFile(file);
            if (content == null) {
                return;
            }
            StringSelection selection = new StringSelection(content);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        }
    }
}
