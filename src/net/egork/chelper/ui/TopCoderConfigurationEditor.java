package net.egork.chelper.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.task.TopCoderTask;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderConfigurationEditor extends SettingsEditor<TopCoderConfiguration> {
	private final TopCoderConfiguration configuration;

	public TopCoderConfigurationEditor(TopCoderConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	protected void resetEditorFrom(TopCoderConfiguration s) {
	}

	@Override
	protected void applyEditorTo(TopCoderConfiguration s) throws ConfigurationException {
	}

	@NotNull
	@Override
	protected JComponent createEditor() {
		JButton editTests = new JButton("Edit Tests");
		editTests.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TopCoderTask task = configuration.getConfiguration();
				configuration.setConfiguration(task.setTests(TopCoderEditTestsDialog.editTests(task)));
			}
		});
		return editTests;
	}

	@Override
	protected void disposeEditor() {

	}
}
