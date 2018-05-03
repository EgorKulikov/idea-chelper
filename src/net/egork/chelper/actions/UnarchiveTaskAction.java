package net.egork.chelper.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import net.egork.chelper.codegeneration.CodeGenerationUtilities;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.*;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class UnarchiveTaskAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        if (!Utilities.isEligible(e.getDataContext())) {
            return;
        }
        final Project project = Utilities.getProject(e.getDataContext());
        FileChooserDialog dialog = FileChooserFactory.getInstance().createFileChooser(new FileChooserDescriptor(true, false, false, false, false, true) {
            @Override
            public boolean isFileSelectable(VirtualFile file) {
                return super.isFileSelectable(file) && ("task".equals(file.getExtension()) || "tctask".equals(file.getExtension()) || "json".equals(file.getExtension()));
            }

            @Override
            public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                return super.isFileVisible(file, showHiddenFiles) &&
                        (file.isDirectory() || "task".equals(file.getExtension()) || "tctask".equals(file.getExtension()) || "json".equals(file.getExtension()));
            }
        }, project, null);
        final VirtualFile[] files = dialog.choose(FileUtilities.getFile(project, Utilities.getData(project).archiveDirectory), project);
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                try {
                    for (VirtualFile taskFile : files) {
                        if ("task".equals(taskFile.getExtension())) {
                            Task task = Task.loadTask(new InputReader(taskFile.getInputStream()));
                            if (unarchiveTask(taskFile, task, project)) {
                                return;
                            }
                        } else if ("tctask".equals(taskFile.getExtension())) {
                            TopCoderTask task = TopCoderTask.load(new InputReader(taskFile.getInputStream()));
                            if (unarchiveTask(taskFile, task, project)) {
                                return;
                            }
                        } else if ("json".equals(taskFile.getExtension())) {
                            Task task = TaskUtilities.mapper.readValue(FileUtilities.getInputStream(taskFile), Task.class);
                            if (task.testType == null) {
                                TopCoderTask topCoderTask = TaskUtilities.mapper.readValue(FileUtilities.getInputStream(taskFile), TopCoderTask.class);
                                if (unarchiveTask(taskFile, topCoderTask, project)) {
                                    return;
                                }
                            } else {
                                if (unarchiveTask(taskFile, task, project)) {
                                    return;
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private boolean unarchiveTask(VirtualFile taskFile, TopCoderTask task, Project project) throws IOException {
        VirtualFile baseDirectory = FileUtilities.getFile(project, Utilities.getData(project).defaultDirectory);
        if (baseDirectory == null) {
            Messenger.publishMessage("Directory where task was located is no longer exists",
                    NotificationType.ERROR);
            return true;
        }
        FileUtilities.saveConfiguration(TaskUtilities.getTaskFileName(task.name), task, baseDirectory);
        List<String> toCopy = new ArrayList<String>();
        VirtualFile mainFile = taskFile.getParent().findChild(task.name + ".java");
        if (mainFile != null) {
            FileUtilities.writeTextFile(baseDirectory, task.name + ".java", FileUtilities.readTextFile(mainFile));
        }
        Collections.addAll(toCopy, task.testClasses);
        for (String className : toCopy) {
            int position = className.lastIndexOf('.');
            if (position != -1) {
                className = className.substring(position + 1);
            }
            VirtualFile file = taskFile.getParent().findChild(className + ".java");
            if (file != null) {
                FileUtilities.writeTextFile(baseDirectory, className + ".java", FileUtilities.readTextFile(file));
            }
        }
        Utilities.createConfiguration(task, true, project);
        return false;
    }

    private boolean unarchiveTask(VirtualFile taskFile, Task task, Project project) throws IOException {
        VirtualFile baseDirectory = FileUtilities.getFile(project, task.location);
        if (baseDirectory == null) {
            Messenger.publishMessage("Directory where task was located is no longer exists",
                    NotificationType.ERROR);
            return true;
        }
        FileUtilities.saveConfiguration(TaskUtilities.getTaskFileName(task.name), task, baseDirectory);
        List<String> toCopy = new ArrayList<String>();
        toCopy.add(task.taskClass);
        toCopy.add(task.checkerClass);
        Collections.addAll(toCopy, task.testClasses);
        String aPackage = FileUtilities.getPackage(FileUtilities.getPsiDirectory(project, task.location));
        if (aPackage == null || aPackage.isEmpty()) {
            int result = JOptionPane.showOptionDialog(null, "Task location is not under source or in default" +
                            "package, do you want to put it in default directory instead?", "Restore task",
                    JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    IconLoader.getIcon("/icons/restore.png"), null, null);
            if (result == JOptionPane.YES_OPTION) {
                String defaultDirectory = Utilities.getData(project).defaultDirectory;
                baseDirectory = FileUtilities.getFile(project, defaultDirectory);
                aPackage = FileUtilities.getPackage(FileUtilities.getPsiDirectory(project, defaultDirectory));
                task = task.setLocation(defaultDirectory);
            }
        }
        for (String className : toCopy) {
            String fullClassName = className;
            int position = className.lastIndexOf('.');
            if (position != -1) {
                className = className.substring(position + 1);
            }
            VirtualFile file = taskFile.getParent().findChild(className + ".java");
            if (file != null) {
                String fileContent = FileUtilities.readTextFile(file);
                if (aPackage != null && !aPackage.isEmpty()) {
                    fileContent = CodeGenerationUtilities.changePackage(fileContent, aPackage);
                    String fqn = aPackage + "." + className;
                    if (task.taskClass.equals(fullClassName)) {
                        task = task.setTaskClass(fqn);
                    } else if (task.checkerClass.equals(fullClassName)) {
                        task = task.setCheckerClass(fqn);
                    } else {
                        for (int i = 0; i < task.testClasses.length; i++) {
                            if (task.testClasses[i].equals(fqn)) {
                                task.testClasses[i] = fqn;
                                break;
                            }
                        }
                    }
                }
                FileUtilities.writeTextFile(baseDirectory, className + ".java", fileContent);
            }
        }
        Utilities.createConfiguration(task, true, project);
        return false;
    }
}
