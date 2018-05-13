package net.egork.chelper.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import net.egork.chelper.ProjectData;
import net.egork.chelper.ui.ProjectDataDialog;
import net.egork.chelper.util.Utilities;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class EditProjectProperties extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        final Project project = Utilities.getProject(e.getDataContext());
        ProjectData data = Utilities.getData(project);
        ProjectData result = ProjectDataDialog.edit(project, data);
        if (result != null) {
            result.save(project);
            Utilities.addProjectData(project, result);
            Utilities.fixLibrary(project);
        }
    }
}
