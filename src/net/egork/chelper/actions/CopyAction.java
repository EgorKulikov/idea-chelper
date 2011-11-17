package net.egork.chelper.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Utilities;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CopyAction extends AnAction {
	public void actionPerformed(AnActionEvent e) {
		if (!Utilities.isEligible(e.getDataContext()))
			return;
		final Project project = Utilities.getProject(e.getDataContext());
		VirtualFile file = FileUtilities.getFile(project, Utilities.getData(project).outputDirectory + "/Main.java");
		if (file == null)
			return;
		String content = FileUtilities.readTextFile(file);
		if (content == null)
			return;
		StringSelection selection = new StringSelection(content);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
	}
}
