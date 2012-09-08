package net.egork.chelper.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import net.egork.chelper.actions.ArchiveAction;
import net.egork.chelper.task.Task;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class TaskUtilities {
    public static PsiElement initialize(Task task, Project project) {
        if (task.location == null)
            return null;
        FileUtilities.createDirectoryIfMissing(project, task.location);
        PsiDirectory directory = FileUtilities.getPsiDirectory(project, task.location);
        if (directory == null)
            return null;
        PsiClass[] psiClasses = JavaDirectoryService.getInstance().getClasses(directory);
        Map<String, PsiClass> classes = new HashMap<String, PsiClass>();
        for (PsiClass psiClass : psiClasses)
            classes.put(psiClass.getName(), psiClass);
        PsiElement main;
        if (!classes.containsKey(task.name))
            main = createMainClass(task, project);
        else
            main = classes.get(task.name);
        if (!classes.containsKey(task.name + "Checker"))
            createCheckerClass(task, project);
        return main;
    }

    private static PsiElement createMainClass(Task task, Project project) {
        String mainFileContent = CodeGenerationUtilities.createStub(task.location, task.name, project);
        VirtualFile file = FileUtilities
            .writeTextFile(FileUtilities.getFile(project, task.location), task.name + ".java", mainFileContent);
        if (file == null)
            return null;
        return PsiManager.getInstance(project).findFile(file);
    }

    private static PsiElement createCheckerClass(Task task, Project project) {
        String checkerFileContent = CodeGenerationUtilities.createCheckerStub(task.location, task.name + "Checker", project);
        VirtualFile file = FileUtilities.writeTextFile(FileUtilities.getFile(project, task.location), task.name +
            "Checker.java", checkerFileContent);
        if (file == null)
            return null;
        return PsiManager.getInstance(project).findFile(file);
    }

    public static String getFQN(String location, String name, Project project) {
        return FileUtilities.getFQN(FileUtilities.getPsiDirectory(project, location), name);
    }

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

    public static VirtualFile getCheckerFile(String location, String name, Project project) {
        return FileUtilities.getFile(project, location + "/" + name + "Checker.java");
    }

}
