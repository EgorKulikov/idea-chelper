package net.egork.chelper;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import net.egork.chelper.actions.NewTaskDefaultAction;
import net.egork.chelper.parser.*;
import net.egork.chelper.task.Task;
import net.egork.chelper.util.ExecuteUtils;
import net.egork.chelper.util.Messenger;
import net.egork.chelper.util.Utilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author egorku@yandex-team.ru
 */
public class ChromeParser implements ProjectComponent {
    private static final int PORT = 4243;
    private static final Map<String, Parser> TASK_PARSERS;

    static {
        Map<String, Parser> taskParsers = new HashMap<String, Parser>();
        taskParsers.put("yandex", new YandexParser());
        taskParsers.put("codeforces", new CodeforcesParser());
        taskParsers.put("hackerrank", new HackerRankParser());
        taskParsers.put("facebook", new FacebookParser());
        taskParsers.put("usaco", new UsacoParser());
        taskParsers.put("gcj", new GCJParser());
        taskParsers.put("bayan", new BayanParser());
        taskParsers.put("kattis", new KattisParser());
        taskParsers.put("codechef", new CodeChefParser());
        taskParsers.put("hackerearth", new HackerEarthParser());
        taskParsers.put("atcoder", new AtCoderParser());
        taskParsers.put("csacademy", new CSAcademyParser());
        taskParsers.put("new-gcj", new NewGCJParser());
        taskParsers.put("json", new JSONParser());
        TASK_PARSERS = Collections.unmodifiableMap(taskParsers);
    }

    private final Project project;
    private ServerSocket serverSocket;

    public ChromeParser(Project project) {
        this.project = project;
    }

    public void initComponent() {
        // TODO: insert component initialization logic here
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName() {
        return "ChromeParser";
    }

    public void projectOpened() {
        if (ProjectData.load(project) == null) {
            return;
        }
        try {
            serverSocket = new ServerSocket(PORT);
            new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            if (serverSocket.isClosed()) {
                                return;
                            }
                            Socket socket = serverSocket.accept();
                            try {
                                BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(socket.getInputStream(), "UTF-8"));
                                while (!reader.readLine().isEmpty()) ;
                                final String type = reader.readLine();
                                StringBuilder builder = new StringBuilder();
                                String s;
                                while ((s = reader.readLine()) != null)
                                    builder.append(s).append('\n');
                                final String page = builder.toString();
                                ExecuteUtils.executeStrictWriteActionAndWait(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (TASK_PARSERS.containsKey(type)) {
                                            System.err.println(page);
                                            Collection<Task> tasks = TASK_PARSERS.get(type).parseTaskFromHTML(page);
                                            if (tasks.isEmpty()) {
                                                Messenger.publishMessage("Unable to parse task from " + type, NotificationType.WARNING);
                                                return;
                                            }
                                            JFrame projectFrame = WindowManager.getInstance().getFrame(project);
                                            if (projectFrame.getState() == JFrame.ICONIFIED) {
                                                projectFrame.setState(Frame.NORMAL);
                                            }
                                            for (Task task : tasks) {
                                                task = task.setTemplate(Utilities.getDefaultTask().template);
                                                NewTaskDefaultAction.createTaskInDefaultDirectory(project, task);
                                            }
                                        } else {
                                            Messenger.publishMessage("Unknown task type from Chrome parser: " + type,
                                                    NotificationType.WARNING);
                                            System.err.println(page);
                                        }
                                    }
                                });
                            } finally {
                                socket.close();
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }).start();
        } catch (IOException e) {
            Messenger.publishMessage("Could not create serverSocket for Chrome parser, probably another CHelper-" +
                    "eligible project is running?", NotificationType.ERROR);
        }
    }

    public void projectClosed() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }
    }

    public static void checkInstalled(Project project, ProjectData configuration) {
        if (!configuration.extensionProposed) {
            JPanel panel = new JPanel(new BorderLayout(15, 15));
            JLabel description = new JLabel("<html>You can now use new CHelper extension to parse<br>" +
                    "tasks directly from Google Chrome<br>(currently supported - Yandex.Contest, Codeforces and HackerRank)<br><br>Do you want to install it?</html>");
            JButton download = new JButton("Download");
            JButton close = new JButton("Close");
            JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.add(download, BorderLayout.WEST);
            buttonPanel.add(close, BorderLayout.EAST);
            panel.add(buttonPanel, BorderLayout.SOUTH);
            panel.add(description, BorderLayout.CENTER);
            final JDialog dialog = new JDialog();
            close.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            });
            download.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(new URL("https://chrome.google.com/webstore/detail/chelper-extension/eicjndbmlajfjdhephbcjdeegmmoadip").toURI());
                        } catch (IOException ignored) {
                        } catch (URISyntaxException ignored) {
                        }
                    }
                    dialog.setVisible(false);
                }
            });
            panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            dialog.setContentPane(panel);
            dialog.pack();
            Point center = Utilities.getLocation(project, panel.getSize());
            dialog.setLocation(center);
            dialog.setVisible(true);
            configuration.completeExtensionProposal(project);
        }
    }
}
