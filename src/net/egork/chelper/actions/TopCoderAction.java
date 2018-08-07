package net.egork.chelper.actions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import net.egork.chelper.codegeneration.CodeGenerationUtilities;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.topcoder.CHelperArenaPlugin;
import net.egork.chelper.topcoder.Message;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.TaskUtilities;
import net.egork.chelper.util.Utilities;
import net.egork.chelper.util.ExecuteUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Properties;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class TopCoderAction extends AnAction {
    private final static String SEPARATOR = System.getProperty("path.separator");
    private static ServerSocket serverSocket = null;
    private static VirtualFileListener listener = null;

    public void actionPerformed(AnActionEvent e) {
        if (!Utilities.isEligible(e.getDataContext())) {
            return;
        }
        Project project = Utilities.getProject(e.getDataContext());
        fixTopCoderSettings();
        startServer(project);
        String arenaFileName = createArenaJar();
        if (arenaFileName == null) {
            return;
        }
        String javaExecutable = System.getProperty("java.home") + File.separator + "bin" + File.separator + "javaws";
        try {
            new ProcessBuilder(javaExecutable, arenaFileName).start();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String createArenaJar() {
        try {
            File tempFile = File.createTempFile("ContestAppletProd", ".jnlp");
            tempFile.deleteOnExit();
            InputStream inputStream = new URL("http://www.topcoder.com/contest/arena/ContestAppletProd.jnlp").openStream();
            OutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1)
                outputStream.write(buffer, 0, bytesRead);
            inputStream.close();
            outputStream.close();
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Arena is not accessible, check Internet connection", "Connection error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private static void startServer(final Project project) {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            serverSocket = new ServerSocket(CHelperArenaPlugin.PORT);
            new Thread(new Runnable() {
                private ServerSocket serverSocket;

                public void run() {
                    serverSocket = TopCoderAction.serverSocket;
                    while (true) {
                        try {
                            if (serverSocket.isClosed()) {
                                return;
                            }
                            Socket socket = serverSocket.accept();
                            Message message = new Message(socket);
                            try {
                                String type = message.in.readString();
                                if (Message.GET_SOURCE.equals(type)) {
                                    String taskName = message.in.readString();
                                    VirtualFile directory = FileUtilities.getFile(project, Utilities.getData(project).outputDirectory);
                                    VirtualFile file = directory.findChild(taskName + ".java");
                                    if (file != null) {
                                        message.out.printString(Message.OK);
                                        message.out.printString(FileUtilities.readTextFile(file));
                                    } else {
                                        message.out.printString(Message.OTHER_ERROR);
                                    }
                                } else if (Message.NEW_TASK.equals(type)) {
                                    final TopCoderTask task = TaskUtilities.mapper.readValue(message.in, TopCoderTask.class);
                                    if (task == null) {
                                        message.out.printString(Message.OTHER_ERROR);
                                    } else {
                                        final VirtualFile directory = FileUtilities.getFile(project, Utilities.getData(project).defaultDirectory);
                                        VirtualFile taskFile = null;
                                        if (directory != null) {
                                            taskFile = directory.findChild(TaskUtilities.getTaskFileName(task.name));
                                        }
                                        if (taskFile != null) {
                                            message.out.printString(Message.ALREADY_DEFINED);
                                        } else {
                                            message.out.printString(Message.OK);
                                            ExecuteUtils.executeStrictWriteActionAndWait(new Runnable() {
                                                @Override
                                                public void run() {
                                                            FileUtilities.createDirectoryIfMissing(project, Utilities.getData(project).defaultDirectory);
                                                            createConfiguration(project, task);
                                                        }
                                                    });
                                                }
                                        }
                                    }
                            } catch (Throwable ignored) {
                            } finally {
                                socket.close();
                            }
                        } catch (IOException ignored) {
                        }
                    }
                }
            }).start();
        } catch (IOException e) {
            serverSocket = null;
        }
        if (listener != null) {
            VirtualFileManager.getInstance().removeVirtualFileListener(listener);
        }
        VirtualFileManager.getInstance().addVirtualFileListener(listener = new VirtualFileAdapter() {
            @Override
            public void fileCreated(VirtualFileEvent event) {
                process(event);
            }

            @Override
            public void contentsChanged(VirtualFileEvent event) {
                process(event);
            }

            private void process(VirtualFileEvent event) {
                final VirtualFile file = event.getFile();
                if (".tcjson".equals(file.getName())) {
                    try {
                        TopCoderTask task = TaskUtilities.mapper.readValue(file.getInputStream(), TopCoderTask.class);
                        if (task == null) {
                            return;
                        }
                        VirtualFile taskFile = FileUtilities.getFile(project, Utilities.getData(project).defaultDirectory + "/" + task.name + ".tctask");
                        if (taskFile != null) {
                            return;
                        }
                        createConfiguration(project, task);
                        ApplicationManager.getApplication().runWriteAction(new Runnable() {
                            public void run() {
                                try {
                                    file.delete(null);
                                } catch (IOException ignored) {
                                }
                            }
                        });
                    } catch (IOException ignored) {
                    }
                }
            }
        });
        VirtualFile file = FileUtilities.writeTextFile(LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home")), ".chelper", project.getBasePath() + "\n");
        new File(file.getCanonicalPath()).deleteOnExit();
    }

    private static void createConfiguration(Project project, TopCoderTask task) {
        String defaultDir = Utilities.getData(project).defaultDirectory;
        FileUtilities.createDirectoryIfMissing(project, defaultDir);
        String packageName = FileUtilities.getPackage(FileUtilities.getPsiDirectory(project, defaultDir));
        if (packageName == null || packageName.length() == 0) {
            JOptionPane.showMessageDialog(null, "defaultDirectory should be under source and in non-default package");
            return;
        }
        String fqn = (packageName.length() == 0 ? "" : packageName + ".") + task.name;
        TopCoderTask taskToWrite = task.setFQN(fqn).setFailOnOverflow(Utilities.getData(project).failOnIntegerOverflowForNewTasks);
        if (FileUtilities.getFile(project, defaultDir + "/" + task.name + ".java") == null) {
            FileUtilities.writeTextFile(FileUtilities.getFile(project, defaultDir),
                    task.name + ".java", CodeGenerationUtilities.createTopCoderStub(task, project, packageName));
        }
        Utilities.createConfiguration(taskToWrite, true, project);
        final PsiElement main = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
        Utilities.openElement(project, main);
    }

    private void fixTopCoderSettings() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(new File(System.getProperty("user.home") + File.separator + "contestapplet.conf")));
        } catch (IOException ignored) {
        }
        properties.put("editor.defaultname", "CHelper");
        int pluginCount = Integer.parseInt(properties.getProperty("editor.numplugins", "0"));
        int index = pluginCount + 1;
        for (int i = 1; i <= pluginCount; i++) {
            if ("CHelper".equals(properties.getProperty("editor." + i + ".name"))) {
                index = i;
                break;
            }
        }
        pluginCount = Math.max(pluginCount, index);
        properties.put("editor.numplugins", Integer.toString(pluginCount));
        properties.put("editor." + index + ".name", "CHelper");
        properties.put("editor." + index + ".entrypoint", CHelperArenaPlugin.class.getName());
        properties.put("editor." + index + ".classpath", getJarPathForClass(CHelperArenaPlugin.class) + SEPARATOR +
                getJarPathForClass(JsonCreator.class) + SEPARATOR + getJarPathForClass(ObjectMapper.class) + SEPARATOR +
                getJarPathForClass(JsonParser.class));
        properties.put("editor." + index + ".eager", "0");
        try {
            OutputStream outputStream = new FileOutputStream(new File(System.getProperty("user.home") + File.separator + "contestapplet.conf"));
            properties.store(outputStream, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getJarPathForClass(@NotNull Class aClass) {
        final String resourceRoot = PathManager.getResourceRoot(aClass, "/" + aClass.getName().replace('.', '/') + ".class");
        return resourceRoot != null ? new File(resourceRoot).getAbsolutePath() : null;
    }

    public static void start(Project project) {
        if (serverSocket == null) {
            startServer(project);
        }
    }
}
