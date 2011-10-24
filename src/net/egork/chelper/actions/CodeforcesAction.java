package net.egork.chelper.actions;

import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TaskConfigurationType;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;
import net.egork.chelper.util.Utilities;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CodeforcesAction extends AnAction {
	public void actionPerformed(AnActionEvent e) {
		if (!Utilities.isEligible(e.getDataContext()))
			return;
		String id = JOptionPane.showInputDialog("Enter Codeforces contest id", "");
		Project project = Utilities.getProject(e.getDataContext());
		RunManagerImpl manager = RunManagerImpl.getInstanceImpl(project);
		String mainPage;
		try {
			mainPage = getWebPageContent("http://codeforces.ru/contest/" + id);
		} catch (IOException e1) {
			return;
		}
		RunnerAndConfigurationSettingsImpl firstConfiguration = null;
		for (char c = 'A'; c <= 'Z'; c++) {
			if (mainPage.indexOf("<a href=\"/contest/" + id + "/problem/" + c + "\">") == -1)
				continue;
			try {
				String text = getWebPageContent("http://codeforces.ru/contest/" + id + "/problem/" + c);
				int position = text.indexOf("<div class=\"memory-limit\">");
				if (position == -1)
					continue;
				text = text.substring(position);
				position = text.indexOf("</div>");
				if (position == -1)
					continue;
				text = text.substring(position + 6);
				position = text.indexOf("</div>");
				if (position == -1)
					continue;
				String heapMemory = text.substring(0, position).split(" ")[0] + "M";
				text = text.substring(position);
				position = text.indexOf("<div class=\"input-file\">");
				if (position == -1)
					continue;
				text = text.substring(position);
				position = text.indexOf("</div>");
				if (position == -1)
					continue;
				text = text.substring(position + 6);
				position = text.indexOf("</div>");
				if (position == -1)
					continue;
				String inputFileName = text.substring(0, position);
				text = text.substring(position);
				StreamConfiguration inputType;
				if ("standard input".equals(inputFileName))
					inputType = StreamConfiguration.STANDARD;
				else
					inputType = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, inputFileName);
				position = text.indexOf("<div class=\"output-file\">");
				if (position == -1)
					continue;
				text = text.substring(position);
				position = text.indexOf("</div>");
				if (position == -1)
					continue;
				text = text.substring(position + 6);
				position = text.indexOf("</div>");
				if (position == -1)
					continue;
				String outputFileName = text.substring(0, position);
				text = text.substring(position);
				StreamConfiguration outputType;
				if ("standard output".equals(outputFileName))
					outputType = StreamConfiguration.STANDARD;
				else
					outputType = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, outputFileName);
				List<Test> tests = new ArrayList<Test>();
				while (true) {
					position = text.indexOf("<div class=\"input\">");
					if (position == -1)
						break;
					text = text.substring(position);
					position = text.indexOf("<pre class=\"content\">");
					if (position == -1)
						break;
					text = text.substring(position + 21);
					position = text.indexOf("</pre>");
					if (position == -1)
						break;
					String testInput = text.substring(0, position).replace("<br />", "\n");
					text = text.substring(position);
					position = text.indexOf("<div class=\"output\">");
					if (position == -1)
						break;
					text = text.substring(position);
					position = text.indexOf("<pre class=\"content\">");
					if (position == -1)
						break;
					text = text.substring(position + 21);
					position = text.indexOf("</pre>");
					if (position == -1)
						break;
					String testOutput = text.substring(0, position).replace("<br />", "\n");
					text = text.substring(position);
					tests.add(new Test(testInput, testOutput, tests.size()));
				}
				String name = "Task" + c;
				Task task = new Task(name, Utilities.getData(project).defaultDir, TestType.SINGLE, inputType,
					outputType, heapMemory, "64M", project, tests.toArray(new Test[tests.size()]));
				task.initialize();
				RunnerAndConfigurationSettingsImpl configuration = new RunnerAndConfigurationSettingsImpl(manager,
					new TaskConfiguration(name, project, task,
					TaskConfigurationType.INSTANCE.getConfigurationFactories()[0]), false);
				manager.addConfiguration(configuration, false);
				if (firstConfiguration == null)
					firstConfiguration = configuration;
			} catch (MalformedURLException ignored) {
			} catch (IOException ignored) {
			}
		}
		if (firstConfiguration != null)
			manager.setActiveConfiguration(firstConfiguration);
	}

	private String getWebPageContent(String address) throws IOException {
		URL url = new URL(address);
		InputStream input = url.openStream();
		StringBuilder builder = new StringBuilder();
		int nextByte;
		while ((nextByte = input.read()) != -1)
			builder.append((char)nextByte);
		return builder.toString();
	}
}
