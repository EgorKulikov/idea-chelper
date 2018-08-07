package net.egork.chelper.util;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import net.egork.chelper.codegeneration.CodeGenerationUtilities;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TopCoderTask;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class FileUtilities {
    public static Properties loadProperties(VirtualFile file) {
        InputStream is = getInputStream(file);
        if (is == null) {
            return null;
        }
        Properties properties = new Properties();
        try {
            properties.load(is);
            return properties;
        } catch (IOException e) {
            return null;
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static InputStream getInputStream(VirtualFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    public static PsiDirectory getDirectory(DataContext dataContext) {
        IdeView view = getView(dataContext);
        if (view == null) {
            return null;
        }
        PsiDirectory[] directories = view.getDirectories();
        if (directories.length != 1) {
            return null;
        }
        return directories[0];
    }

    public static IdeView getView(DataContext dataContext) {
        return LangDataKeys.IDE_VIEW.getData(dataContext);
    }

    public static boolean isJavaDirectory(PsiDirectory directory) {
        return directory != null && JavaDirectoryService.getInstance().getPackage(directory) != null;
    }

    public static VirtualFile writeTextFile(final VirtualFile location, final String fileName, final String fileContent) {
        ExecuteUtils.executeStrictWriteActionAndWait(new Runnable() {
            public void run() {
                if (location == null) {
                    return;
                }
                OutputStream stream = null;
                try {
                    VirtualFile file = location.findOrCreateChildData(null, fileName);
                    stream = file.getOutputStream(null);
                    stream.write(fileContent.getBytes(Charset.forName("UTF-8")));
                } catch (IOException ignored) {
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        });
        if (location == null) {
            return null;
        }
        return location.findChild(fileName);
    }

    public static String readTextFile(VirtualFile file) {
        try {
            return VfsUtil.loadText(file);
        } catch (IOException e) {
            return null;
        }
    }

    public static PsiDirectory getPsiDirectory(Project project, String location) {
        VirtualFile file = getFile(project, location);
        if (file == null) {
            return null;
        }
        return PsiManager.getInstance(project).findDirectory(file);
    }

    public static VirtualFile getFile(Project project, String location) {
        if (location == null) {
            return null;
        }
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }
        return baseDir.findFileByRelativePath(location);
    }

    public static String getRelativePath(VirtualFile baseDir, VirtualFile file) {
        if (file == null) {
            return null;
        }
        if (baseDir == null) {
            return file.getPath();
        }
        if (!isChild(baseDir, file)) {
            return null;
        }
        String basePath = baseDir.getPath();
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }
        String filePath = file.getPath();
        return filePath.substring(Math.min(filePath.length(), basePath.length()));
    }

    public static String getPackage(PsiDirectory directory) {
        if (directory == null) {
            return null;
        }
        PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(directory);
        String packageName = null;
        if (aPackage != null) {
            packageName = aPackage.getQualifiedName();
        }
        return packageName;
    }

    public static String getFQN(PsiDirectory directory, String name) {
        String packageName = getPackage(directory);
        if (packageName == null || packageName.length() == 0) {
            return name;
        }
        return packageName + "." + name;
    }

    public static PsiFile getPsiFile(Project project, String location) {
        VirtualFile file = getFile(project, location);
        if (file == null) {
            return null;
        }
        return PsiManager.getInstance(project).findFile(file);
    }

    public static VirtualFile createDirectoryIfMissing(final Project project, final String location) {
        ExecuteUtils.executeStrictWriteActionAndWait(new Runnable() {
            public void run() {
                VirtualFile baseDir = project.getBaseDir();
                if (baseDir == null) {
                    return;
                }
                try {
                    VfsUtil.createDirectoryIfMissing(baseDir, location);
                } catch (IOException ignored) {
                }
            }
        });
        return getFile(project, location);
    }

    public static void synchronizeFile(VirtualFile file) {
        FileDocumentManager.getInstance().saveDocument(FileDocumentManager.getInstance().getDocument(file));
    }

    public static String getWebPageContent(String address) {
        return getWebPageContent(address, "UTF-8");
    }

    public static String getWebPageContent(String address, String charset) {
        for (int i = 0; i < 10; i++) {
            try {
                URL url = new URL(address);
                InputStream input = url.openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, Charset.forName(charset)));
                StringBuilder builder = new StringBuilder();
                String s;
                while ((s = reader.readLine()) != null) {
                    builder.append(s).append('\n');
                }
                return new String(builder.toString().getBytes("UTF-8"), "UTF-8");
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    public static Task readTask(String fileName, Project project) {
        try {
            return TaskUtilities.mapper.readValue(getInputStream(getFile(project, fileName)), Task.class);
        } catch (IOException e) {
            return null;
        }
    }

    public static Task readLegacyTask(String fileName, Project project) {
        return Task.loadTask(new InputReader(getInputStream(getFile(project, fileName))));
    }

    public static TopCoderTask readTopCoderTask(String fileName, Project project) {
        try {
            return TaskUtilities.mapper.readValue(getInputStream(getFile(project, fileName)), TopCoderTask.class);
        } catch (IOException e) {
            return null;
        }
    }

    public static TopCoderTask readLegacyTopCoderTask(String fileName, Project project) {
        return TopCoderTask.load(new InputReader(getInputStream(getFile(project, fileName))));
    }

    public static void saveConfiguration(final String locationName, final String fileName, final Task configuration, final Project project) {
        if (locationName == null) {
            return;
        }
        VirtualFile location = FileUtilities.getFile(project, locationName);
        if (location == null) {
            return;
        }
        saveConfiguration(fileName, configuration, location);
    }

    public static void saveConfiguration(String fileName, Task configuration, VirtualFile location) {
        ExecuteUtils.executeStrictWriteAction(new Runnable() {
            public void run() {
                OutputStream stream = null;
                try {
                    VirtualFile file = location.findOrCreateChildData(null, fileName);
                    stream = file.getOutputStream(null);
                    TaskUtilities.mapper.writeValue(stream, configuration);
                } catch (IOException ignored) {
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        });
    }

    public static void saveConfiguration(final String locationName, final String fileName, final TopCoderTask configuration, final Project project) {
        if (locationName == null) {
            return;
        }
        VirtualFile location = FileUtilities.getFile(project, locationName);
        if (location == null) {
            return;
        }
        saveConfiguration(fileName, configuration, location);
    }

    public static void saveConfiguration(String fileName, TopCoderTask configuration, VirtualFile location) {
        ExecuteUtils.executeStrictWriteAction(new Runnable() {
            public void run() {
                OutputStream stream = null;
                try {
                    VirtualFile file = location.findOrCreateChildData(null, fileName);
                    stream = file.getOutputStream(null);
                    TaskUtilities.mapper.writeValue(stream, configuration);
                } catch (IOException ignored) {
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        });
    }

    public static boolean isChild(VirtualFile parent, VirtualFile child) {
        if (!parent.isDirectory()) {
            return false;
        }
        String parentPath = parent.getPath();
        if (!parentPath.endsWith("/")) {
            parentPath += "/";
        }
        String childPath = child.getPath();
        if (child.isDirectory() && !childPath.endsWith("/")) {
            childPath += "/";
        }
        return childPath.startsWith(parentPath);
    }

    public static boolean isValidClassName(String name) {
        return name.matches("[a-zA-Z_$][a-zA-Z\\d_$]*");
    }

    public static String createTaskClass(Task task, Project project, String path, String name) {
        VirtualFile directory = FileUtilities.createDirectoryIfMissing(project, path);
        String mainClass = CodeGenerationUtilities.createStub(task, path, name, project);
        if (directory.findChild(name + ".java") == null) {
            writeTextFile(directory, name + ".java", mainClass);
        }
        PsiDirectory psiDirectory = getPsiDirectory(project, path);
        String aPackage = getPackage(psiDirectory);
        return aPackage + "." + name;
    }

    public static String createCheckerClass(Project project, String path, String name, Task task) {
        String mainClass = CodeGenerationUtilities.createCheckerStub(path, name, project, task);
        VirtualFile directory = FileUtilities.createDirectoryIfMissing(project, path);
        writeTextFile(directory, name + ".java", mainClass);
        PsiDirectory psiDirectory = getPsiDirectory(project, path);
        String aPackage = getPackage(psiDirectory);
        String fqn = aPackage + "." + name;
        Utilities.openElement(project, Utilities.getPsiElement(project, fqn));
        return fqn;
    }

    public static String createInteractorClass(Project project, String path, String name, Task task) {
        String mainClass = CodeGenerationUtilities.createInteractorStub(path, name, project, task);
        VirtualFile directory = FileUtilities.createDirectoryIfMissing(project, path);
        writeTextFile(directory, name + ".java", mainClass);
        PsiDirectory psiDirectory = getPsiDirectory(project, path);
        String aPackage = getPackage(psiDirectory);
        String fqn = aPackage + "." + name;
        Utilities.openElement(project, Utilities.getPsiElement(project, fqn));
        return fqn;
    }

    public static String createTestClass(Project project, String path, String name, Task task) {
        String mainClass = CodeGenerationUtilities.createTestStub(path, name, project, task);
        VirtualFile directory = FileUtilities.createDirectoryIfMissing(project, path);
        writeTextFile(directory, name + ".java", mainClass);
        PsiDirectory psiDirectory = getPsiDirectory(project, path);
        String aPackage = getPackage(psiDirectory);
        String fqn = aPackage + "." + name;
        Utilities.openElement(project, Utilities.getPsiElement(project, fqn));
        return fqn;
    }

    public static String createTopCoderTestClass(Project project, String path, String name) {
        VirtualFile directory = FileUtilities.createDirectoryIfMissing(project, path);
        PsiDirectory psiDirectory = getPsiDirectory(project, path);
        String aPackage = getPackage(psiDirectory);
        String mainClass = CodeGenerationUtilities.createTopCoderTestStub(project, aPackage, name);
        writeTextFile(directory, name + ".java", mainClass);
        String fqn = aPackage + "." + name;
        Utilities.openElement(project, Utilities.getPsiElement(project, fqn));
        return fqn;
    }

    public static String createIfNeeded(Task task, String taskClass, Project project, String location) {
        if (taskClass.indexOf('.') == -1) {
            taskClass = createTaskClass(task, project, location, taskClass);
        }
        return taskClass;
    }

    public static VirtualFile getFileByFQN(String fqn, Project project) {
        if (fqn == null) {
            return null;
        }
        PsiElement main = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
        return main == null ? null : main.getContainingFile() == null ? null : main.getContainingFile().getVirtualFile();
    }

    public static void removeFile(String fileName, Project project) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    getFile(project, fileName).delete(null);
                } catch (IOException ignored) {
                }
            }
        });
    }
}
