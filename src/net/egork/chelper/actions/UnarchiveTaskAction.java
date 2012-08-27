package net.egork.chelper.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.InputReader;
import net.egork.chelper.util.OutputWriter;
import net.egork.chelper.util.Utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class UnarchiveTaskAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        if (!Utilities.isEligible(e.getDataContext()))
            return;
        final Project project = Utilities.getProject(e.getDataContext());
        FileChooserDialog dialog = FileChooserFactory.getInstance().createFileChooser(new
                FileChooserDescriptor(true, false, false, false, false, true) {
                    @Override
                    public boolean isFileSelectable(VirtualFile file) {
                        return super.isFileSelectable(file) && ("task".equals(file.getExtension()) || "tctask".equals(file.getExtension()));
                    }

                    @Override
                    public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                        return super.isFileVisible(file, showHiddenFiles) &&
                                (file.isDirectory() || "task".equals(file.getExtension()) || "tctask".equals(file.getExtension())) &&
                                (FileUtilities.isChild(project.getBaseDir(), file) ||
                                        FileUtilities.isChild(file, project.getBaseDir()));
                    }
                }, project);
        final VirtualFile[] files = dialog.choose(FileUtilities.getFile(project, Utilities.getData(project).archiveDirectory), project);
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                try {
                    for (VirtualFile taskFile : files) {
						if ("task".equals(taskFile.getExtension())) {
							Task task = Task.loadTask(new InputReader(taskFile.getInputStream()));
							VirtualFile baseDirectory = FileUtilities.getFile(project, task.location);
							task.saveTask(new OutputWriter(baseDirectory.createChildData(null, task.name + ".task").
									getOutputStream(null)));
							List<String> toCopy = new ArrayList<String>();
							toCopy.add(task.taskClass);
							toCopy.add(task.checkerClass);
							Collections.addAll(toCopy, task.testClasses);
							for (String className : toCopy) {
								int position = className.lastIndexOf('.');
								if (position != -1)
									className = className.substring(position + 1);
								VirtualFile file = taskFile.getParent().findChild(className + ".java");
								if (file != null)
									FileUtilities.writeTextFile(baseDirectory, className + ".java", FileUtilities.readTextFile(file));
							}
							Utilities.createConfiguration(task, true, project);
						} else if ("tctask".equals(taskFile.getExtension())) {
							TopCoderTask task = TopCoderTask.load(new InputReader(taskFile.getInputStream()));
							VirtualFile baseDirectory = FileUtilities.getFile(project, Utilities.getData(project).defaultDirectory);
							task.saveTask(new OutputWriter(baseDirectory.createChildData(null, task.name + ".tctask").
									getOutputStream(null)));
							List<String> toCopy = new ArrayList<String>();
							VirtualFile mainFile = taskFile.getParent().findChild(task.name + ".java");
							if (mainFile != null)
								FileUtilities.writeTextFile(baseDirectory, task.name + ".java", FileUtilities.readTextFile(mainFile));
							Collections.addAll(toCopy, task.testClasses);
							for (String className : toCopy) {
								int position = className.lastIndexOf('.');
								if (position != -1)
									className = className.substring(position + 1);
								VirtualFile file = taskFile.getParent().findChild(className + ".java");
								if (file != null)
									FileUtilities.writeTextFile(baseDirectory, className + ".java", FileUtilities.readTextFile(file));
							}
							Utilities.createConfiguration(task, true, project);
						}
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
