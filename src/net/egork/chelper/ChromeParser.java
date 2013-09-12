package net.egork.chelper;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import net.egork.chelper.actions.NewTaskDefaultAction;
import net.egork.chelper.parser.YandexParser;
import net.egork.chelper.task.Task;
import net.egork.chelper.util.Messenger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author egorku@yandex-team.ru
 */
public class ChromeParser implements ProjectComponent {
	private static final int PORT = 4243;
	private static final String YANDEX = "yandex";

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
		if (ProjectData.load(project) == null)
			return;
		try {
			serverSocket = new ServerSocket(PORT);
			new Thread(new Runnable() {
				public void run() {
					while (true) {
						try {
							if (serverSocket.isClosed())
								return;
							Socket socket = serverSocket.accept();
							try {
								BufferedReader reader = new BufferedReader(
									new InputStreamReader(socket.getInputStream(), "UTF-8"));
								String s;
								while (!(s = reader.readLine()).isEmpty());
								String type = reader.readLine();
								StringBuilder builder = new StringBuilder();
								while ((s = reader.readLine()) != null)
									builder.append(s).append('\n');
								String page = builder.toString();
								final Task task;
								if (type.startsWith(YANDEX)) {
									task = new YandexParser().parseTaskFromHTML(page);
								} else {
									Messenger.publishMessage("Unknown task type from Chrome parser: " + s,
										NotificationType.WARNING);
									return;
								}
								ApplicationManager.getApplication().runReadAction(new Runnable() {
									public void run() {
										NewTaskDefaultAction.createTaskInDefaultDirectory(project, task);
									}
								});
							} catch (Throwable ignored) {
							} finally {
								socket.close();
							}
						} catch (IOException ignored) {}
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
}
