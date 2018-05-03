package net.egork.chelper.actions;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.ui.BulkAddTestsDialog;
import net.egork.chelper.util.Utilities;

/**
 * @author egorku@yandex-team.ru
 */
public class BulkAddTests extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        if (!Utilities.isEligible(e.getDataContext())) {
            return;
        }
        Project project = Utilities.getProject(e.getDataContext());
        RunnerAndConfigurationSettings selectedConfiguration =
                RunManagerImpl.getInstanceImpl(project).getSelectedConfiguration();
        if (selectedConfiguration == null) {
            return;
        }
        RunConfiguration configuration = selectedConfiguration.getConfiguration();
        if (configuration instanceof TaskConfiguration) {
            TaskConfiguration taskConfiguration = (TaskConfiguration) configuration;
            Task task = taskConfiguration.getConfiguration();
            taskConfiguration.setConfiguration(task.setTests(BulkAddTestsDialog.show(project, task.tests)));
        }
    }
}
