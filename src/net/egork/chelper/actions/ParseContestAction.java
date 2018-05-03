package net.egork.chelper.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import net.egork.chelper.task.Task;
import net.egork.chelper.ui.ParseDialog;
import net.egork.chelper.util.Utilities;

import java.util.Collection;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class ParseContestAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        if (!Utilities.isEligible(e.getDataContext())) {
            return;
        }
        Project project = Utilities.getProject(e.getDataContext());
        Collection<Task> tasks = ParseDialog.parseContest(project);
        boolean firstConfiguration = true;
        PsiElement firstElement = null;
        for (Task task : tasks) {
            PsiElement element = JavaPsiFacade.getInstance(project).findClass(task.taskClass, GlobalSearchScope.allScope(project));
            Utilities.createConfiguration(task, firstConfiguration, project);
            firstConfiguration = false;
            if (firstElement == null) {
                firstElement = element;
            }
        }
        if (firstElement != null) {
            Utilities.openElement(project, firstElement);
        }
    }
}
