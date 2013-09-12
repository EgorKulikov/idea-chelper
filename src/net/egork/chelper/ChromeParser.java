package net.egork.chelper;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import net.egork.chelper.actions.NewTaskDefaultAction;
import net.egork.chelper.parser.YandexParser;
import net.egork.chelper.task.Task;
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
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;

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
							final Socket socket = serverSocket.accept();
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									try {
										BufferedReader reader = new BufferedReader(
											new InputStreamReader(socket.getInputStream(), "UTF-8"));
										String s;
										while (!(s = reader.readLine()).isEmpty()) ;
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
										OutputStream out = socket.getOutputStream();
										out.write("HTTP/1.1 200 OK".getBytes());
										out.flush();
										NewTaskDefaultAction.createTaskInDefaultDirectory(project, task);
									} catch (Throwable ignored) {
									} finally {
										try {
											socket.close();
										} catch (IOException ignored) {
										}
									}
								}
							});
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

	public static void checkInstalled(Project project, ProjectData configuration) {
		if (!configuration.extensionProposed) {
			JPanel panel = new JPanel(new BorderLayout());
			JLabel description = new JLabel("<html>You can now use new CHelper extension to parse<br>" +
				"tasks directly from Google Chrome<br>(currently supported - Yandex.Contest)<br><br>Do you want to install it?</html>");
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
