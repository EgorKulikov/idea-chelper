package net.egork.chelper.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.testFramework.PsiTestUtil;
import net.egork.chelper.ProjectData;
import net.egork.chelper.tester.NewTester;
import net.egork.chelper.ui.ProjectDataDialog;
import net.egork.chelper.util.Utilities;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class EditProjectProperties extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        Project project = Utilities.getProject(e.getDataContext());
        ProjectData data = Utilities.getData(project);
        ProjectData result = ProjectDataDialog.edit(project, data);
        if (result != null) {
            result.save(project);
            Utilities.addProjectData(project, result);
			LibraryTable table = ProjectLibraryTable.getInstance(project);
			if (table.getLibraryByName("CHelper") != null)
				return;
			String path = TopCoderAction.getJarPathForClass(NewTester.class);
			for (Module module : ModuleManager.getInstance(project).getModules()) {
				ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
				if (rootManager.getContentRoots().length > 0)
					PsiTestUtil.addLibrary(module, "CHelper", path, "");
			}
        }
    }
}
