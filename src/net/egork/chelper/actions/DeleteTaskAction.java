package net.egork.chelper.actions;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.TaskUtilities;
import net.egork.chelper.util.Utilities;

import javax.swing.*;
import java.io.IOException;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class DeleteTaskAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        if (!Utilities.isEligible(e.getDataContext())) {
            return;
        }
        final Project project = Utilities.getProject(e.getDataContext());
        final RunManagerImpl manager = RunManagerImpl.getInstanceImpl(project);
        RunnerAndConfigurationSettings selectedConfiguration =
                manager.getSelectedConfiguration();
        if (selectedConfiguration == null) {
            return;
        }
        RunConfiguration configuration = selectedConfiguration.getConfiguration();
        int result = JOptionPane.showConfirmDialog(WindowManager.getInstance().getFrame(project), "Are you sure you want to delete current configuration?",
                "Delete Task", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        if (configuration instanceof TaskConfiguration) {
            final Task task = ((TaskConfiguration) configuration).getConfiguration();
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                public void run() {
                    try {
                        PsiElement main = JavaPsiFacade.getInstance(project).findClass(task.taskClass, GlobalSearchScope.allScope(project));
                        VirtualFile mainFile = main == null ? null : main.getContainingFile() == null ? null : main.getContainingFile().getVirtualFile();
                        if (mainFile != null) {
                            mainFile.delete(this);
                        }
                        PsiElement checker = JavaPsiFacade.getInstance(project).findClass(task.checkerClass, GlobalSearchScope.allScope(project));
                        VirtualFile checkerFile = checker == null ? null : checker.getContainingFile() == null ? null : checker.getContainingFile().getVirtualFile();
                        if (checkerFile != null && mainFile != null && checkerFile.getParent().equals(mainFile.getParent())) {
                            checkerFile.delete(this);
                        }
                        PsiElement interactor = task.interactor == null ? null : JavaPsiFacade.getInstance(project).findClass(task.interactor, GlobalSearchScope.allScope(project));
                        VirtualFile interactorFile = interactor == null ? null : interactor.getContainingFile() == null ? null : interactor.getContainingFile().getVirtualFile();
                        if (interactorFile != null && mainFile != null && interactorFile.getParent().equals(mainFile.getParent())) {
                            interactorFile.delete(this);
                        }
                        for (String testClass : task.testClasses) {
                            PsiElement test = JavaPsiFacade.getInstance(project).findClass(testClass, GlobalSearchScope.allScope(project));
                            VirtualFile testFile = test == null ? null : test.getContainingFile() == null ? null : test.getContainingFile().getVirtualFile();
                            if (testFile != null) {
                                testFile.delete(this);
                            }
                        }
                        VirtualFile taskFile = FileUtilities.getFile(project, TaskUtilities.getTaskFileLocation(task.location, task.name));
                        if (taskFile != null) {
                            taskFile.delete(this);
                        }
                        manager.removeConfiguration(manager.getSelectedConfiguration());
                        ArchiveAction.setOtherConfiguration(manager, task);
                    } catch (IOException ignored) {
                    }
                }
            });
        }
        if (configuration instanceof TopCoderConfiguration) {
            final TopCoderTask task = ((TopCoderConfiguration) configuration).getConfiguration();
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                public void run() {
                    try {
                        VirtualFile mainFile = FileUtilities.getFile(project, Utilities.getData(project).defaultDirectory
                                + "/" + task.name + ".java");
                        if (mainFile != null) {
                            mainFile.delete(this);
                        }
                        for (String testClass : task.testClasses) {
                            PsiElement test = JavaPsiFacade.getInstance(project).findClass(testClass, GlobalSearchScope.allScope(project));
                            VirtualFile testFile = test == null ? null : test.getContainingFile() == null ? null : test.getContainingFile().getVirtualFile();
                            if (testFile != null) {
                                testFile.delete(this);
                            }
                        }
                        VirtualFile taskFile = FileUtilities.getFile(project, TaskUtilities.getTaskFileLocation(Utilities.getData(project).defaultDirectory, task.name));
                        if (taskFile != null) {
                            taskFile.delete(this);
                        }
                        manager.removeConfiguration(manager.getSelectedConfiguration());
                        ArchiveAction.setOtherConfiguration(manager, task);
                    } catch (IOException ignored) {
                    }
                }
            });
        }
    }
}
