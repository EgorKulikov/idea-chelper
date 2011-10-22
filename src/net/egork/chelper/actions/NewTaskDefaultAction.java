package net.egork.chelper.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import net.egork.chelper.ProjectData;
import net.egork.chelper.Utilities;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class NewTaskDefaultAction extends AnAction {
	public void actionPerformed(AnActionEvent e) {
		if (!Utilities.isEligible(e.getDataContext()))
			return;
		Project project = Utilities.getProject(e.getDataContext());
		ProjectData data = Utilities.getData(project);
		PsiDirectory directory = Utilities.getPsiDirectory(project, data.defaultDir);
		if (directory == null)
			return;
		PsiElement[] result = NewTaskAction.createTask(null, directory);
		for (PsiElement element : result) {
			if (element instanceof PsiFile) {
				VirtualFile virtualFile = ((PsiFile) element).getVirtualFile();
				if (virtualFile == null)
					continue;
				FileEditorManager.getInstance(project).openFile(virtualFile, true);
			} else if (element instanceof PsiClass) {
				FileEditorManager.getInstance(project).openFile(Utilities.getFile(project,
					Utilities.getData(project).defaultDir + "/" + ((PsiClass) element).getName() + ".java"), true);
			}
		}
	}
}
