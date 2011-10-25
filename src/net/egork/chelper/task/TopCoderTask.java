package net.egork.chelper.task;

import com.intellij.openapi.project.Project;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Utilities;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderTask {
	public final Project project;
	public final String name;
	public final MethodSignature signature;
	public final TopCoderTest[] tests;

	public TopCoderTask(Project project, String name, MethodSignature signature) {
		this(project, name, signature, new TopCoderTest[0]);
	}

	public TopCoderTask(Project project, String name, MethodSignature signature, TopCoderTest[] tests) {
		this.project = project;
		this.name = name;
		this.signature = signature;
		this.tests = tests;
	}

	public String getSignature() {
		return signature.toString();
	}

	public String getFQN() {
		return FileUtilities.getFQN(FileUtilities.getPsiDirectory(project,
			Utilities.getData(project).defaultDir), name);
	}

	public TopCoderTask setTests(TopCoderTest[] tests) {
		return new TopCoderTask(project, name, signature, tests);
	}
}
