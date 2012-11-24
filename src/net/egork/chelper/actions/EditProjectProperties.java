package net.egork.chelper.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import net.egork.chelper.ProjectData;
import net.egork.chelper.tester.NewTester;
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
            //CojacInstaller.install(project);
            Utilities.addProjectData(project, result);
			ApplicationManager.getApplication().runWriteAction(new Runnable() {
				public void run() {
					LibraryTable table = ProjectLibraryTable.getInstance(project);
					String path = TopCoderAction.getJarPathForClass(NewTester.class);
					VirtualFile jar = VirtualFileManager.getInstance().findFileByUrl(VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, path) + JarFileSystem.JAR_SEPARATOR);
					Library library = table.getLibraryByName("CHelper");
					if (library == null) {
						library = table.createLibrary("CHelper");
						Library.ModifiableModel libraryModel = library.getModifiableModel();
						libraryModel.addRoot(jar, OrderRootType.CLASSES);
						libraryModel.commit();
					} else
						return;
					for (Module module : ModuleManager.getInstance(project).getModules()) {
						ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
						if (model.getModuleLibraryTable().getLibraryByName("CHelper") == null) {
							model.addLibraryEntry(library);
							model.commit();
						}
					}
				}
			});
        }
    }
}
