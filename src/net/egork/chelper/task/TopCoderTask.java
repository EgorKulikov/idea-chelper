package net.egork.chelper.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import net.egork.chelper.Utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

	public void createSourceFile() {
		ApplicationManager.getApplication().runWriteAction(new Runnable() {
			public void run() {
				PsiFile originalSource = Utilities.getPsiFile(project, Utilities.getData(project).defaultDir + "/" +
					name + ".java");
				String[] textParts = Task.generateInlinedSource(project, Collections.<String>emptySet(),
					originalSource);
				final StringBuilder text = new StringBuilder();
				text.append(textParts[0]);
				text.append(textParts[1]);
				String outputDirectory = Utilities.getData(project).topcoderDir;
				VirtualFile directory = Utilities.createDirectoryIfMissing(project, outputDirectory);
				if (directory == null)
					return;
				final VirtualFile file = Utilities.writeTextFile(directory, name + ".java", text.toString());
				Utilities.synchronizeFile(file);
				Task.removeUnusedCode(project, file, name, signature.name);
			}
		});
	}

	public String getSignature() {
		return signature.toString();
	}

	public String encodeTests() {
		if (tests.length == 0)
			return "empty";
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (TopCoderTest test : tests) {
			if (first)
				first = false;
			else
				builder.append(Task.SEPARATOR);
			builder.append(test.encode());
		}
		return builder.toString();
	}

	public static TopCoderTask read(String taskConf, Project project) {
		String[] tokens = taskConf.split(Task.SEPARATOR, -1);
		String name = tokens[0];
		MethodSignature signature = MethodSignature.parse(tokens[1]);
		if ("empty".equals(tokens[2]))
			return new TopCoderTask(project, name, signature);
		TopCoderTest[] tests = new TopCoderTest[tokens.length - 2];
		for (int i = 0; i < tests.length; i++)
			tests[i] = TopCoderTest.decode(i, tokens[i + 2]);
		return new TopCoderTask(project, name, signature, tests);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(name).append(Task.SEPARATOR).append(signature == null ? "" : signature).append(Task.SEPARATOR);
		builder.append(encodeTests());
		return builder.toString();
	}

	public String getFQN() {
		return Utilities.getFQN(project, Utilities.getPsiDirectory(project, Utilities.getData(project).defaultDir),
			name);
	}

	public TopCoderTask setTests(TopCoderTest[] tests) {
		return new TopCoderTask(project, name, signature, tests);
	}

	public static TopCoderTask parseFile(VirtualFile file, final Project project) {
		try {
			String text = VfsUtil.loadText(file);
			String originalText = text;
			final String name = file.getNameWithoutExtension();
			String classSignature = "public class " + name;
			int index = text.indexOf(classSignature);
			if (index == -1)
				return null;
			text = text.substring(index + classSignature.length());
			int openBracketIndex = text.indexOf("{");
			if (openBracketIndex == -1)
				return null;
			text = text.substring(openBracketIndex + 1);
			int methodSignatureEnd = text.indexOf("{");
			if (methodSignatureEnd == -1)
				return null;
			String signatureText = text.substring(0, methodSignatureEnd).trim();
			MethodSignature methodSignature = MethodSignature.parse(signatureText);
			String testStart = "switch( casenum ) {";
			int testStartIndex = text.indexOf(testStart);
			if (testStartIndex == -1)
				return null;
			text = text.substring(testStartIndex + testStart.length());
			String testEnd = "// custom cases";
			int testEndIndex = text.indexOf(testEnd);
			if (testEndIndex == -1)
				return null;
			text = text.substring(0, testEndIndex);
			List<TopCoderTest> tests = new ArrayList<TopCoderTest>();
			for (int i = 0; ; i++) {
				String nextTestStart = "case " + i;
				int nextTestStartIndex = text.indexOf(nextTestStart);
				if (nextTestStartIndex == -1)
					break;
				text = text.substring(nextTestStartIndex);
				String[] argumentsAndResult = new String[methodSignature.arguments.length + 1];
				for (int j = 0; j < argumentsAndResult.length; j++) {
					int equalsIndex = text.indexOf('=');
					if (equalsIndex == -1)
						return null;
					text = text.substring(equalsIndex + 1);
					int lineEnd = text.indexOf('\n');
					if (lineEnd == -1)
						return null;
					argumentsAndResult[j] = text.substring(0, lineEnd).trim();
					argumentsAndResult[j] = argumentsAndResult[j].substring(0, argumentsAndResult[j].length() - 1);
					text = text.substring(lineEnd);
				}
				tests.add(new TopCoderTest(Arrays.copyOf(argumentsAndResult, argumentsAndResult.length - 1),
					argumentsAndResult[argumentsAndResult.length - 1], i));
			}
			String tailStart = "// BEGIN CUT HERE";
			int tailIndex = originalText.indexOf(tailStart);
			if (tailIndex == -1)
				return null;
			originalText = originalText.substring(0, tailIndex) + "}\n\n";
			final String finalOriginalText = originalText;
			ApplicationManager.getApplication().runWriteAction(new Runnable() {
				public void run() {
					Utilities.writeTextFile(Utilities.getFile(project, Utilities.getData(project).defaultDir),
						name + ".java", finalOriginalText);
				}
			});
			FileEditorManager.getInstance(project).openFile(Utilities.getFile(project,
				Utilities.getData(project).defaultDir + "/" + name + ".java"), true);
			return new TopCoderTask(project, name, methodSignature, tests.toArray(new TopCoderTest[tests.size()]));
		} catch (IOException e) {
			return null;
		}
	}
}
