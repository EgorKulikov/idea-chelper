package net.egork.chelper.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.egork.chelper.actions.ArchiveAction;
import net.egork.chelper.task.Task;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class TaskUtilities {
    public static void createSourceFile(Task task, Project project) {
        CodeGenerationUtilities.createSourceFile(task, project);
    }

    public static VirtualFile getFile(String location, String name, Project project) {
        return FileUtilities.getFile(project, location + "/" + name + ".java");
    }

    public static String getTaskFileName(String location, String name) {
        if (location != null && name != null)
            return location + "/" + ArchiveAction.canonize(name) + ".task";
        return null;
    }

    public static String getTopCoderTaskFileName(String location, String name) {
        if (location != null && name != null)
            return location + "/" + name + ".tctask";
        return null;
    }
}
