package net.egork.chelper.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.search.searches.ReferencesSearch;
import net.egork.chelper.task.*;

import javax.swing.*;
import java.util.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CodeGenerationUtilities {
	private static final String[] classStarts = {"class ", "abstract class", "interface ", "enum "};

	public static String[] createInlinedSource(Project project, Set<String> mandatoryImports, PsiFile originalSource)
	{
		InlineVisitor inlineVisitor = new InlineVisitor(project, originalSource);
		List<PsiClass> toInline = inlineVisitor.toInline;
		Set<String> toImport = inlineVisitor.toImport;
		toImport.addAll(mandatoryImports);
		StringBuilder importsBuilder = new StringBuilder();
		for (String aImport : toImport) {
			if (!aImport.startsWith("import java.lang."))
				importsBuilder.append(aImport).append("\n");
		}
		importsBuilder.append("\n");
		importsBuilder.append("/**\n");
		importsBuilder.append(" * Built using CHelper plug-in\n");
		importsBuilder.append(" * Actual solution is at the top\n");
		String author = Utilities.getData(project).author;
		if (author.length() != 0)
			importsBuilder.append(" * @author ").append(author).append("\n");
		importsBuilder.append(" */\n");
		String imports = importsBuilder.toString();
		final StringBuilder text = new StringBuilder();
		for (PsiClass aClass : toInline) {
			String classText = aClass.getText();
			int startIndex = -1;
			for (String start : classStarts) {
				int index = classText.indexOf(start);
				if (index != -1 && (startIndex == -1 || startIndex > index))
					startIndex = index;
			}
			if (startIndex == -1)
				continue;
			text.append(classText.substring(startIndex));
			text.append("\n\n");
		}
		return new String[]{imports, text.toString()};
	}

	public static void removeUnusedCode(final Project project, final VirtualFile virtualFile, final String mainClass,
		final String mainMethod)
	{
		PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
		if (file == null)
			return;
		PsiDirectory parent = file.getParent();
		PsiPackage aPackage = parent == null ? null : JavaDirectoryService.getInstance().getPackage(parent);
		if (aPackage == null || aPackage.getName() != null) {
			JOptionPane.showMessageDialog(null, "outputDirectory should be under source and in default package");
			return;
		}
		while (true) {
			final List<PsiElement> toRemove = new ArrayList<PsiElement>();
			file.acceptChildren(new PsiElementVisitor() {
				private boolean visitElementImpl(PsiElement element) {
					if (element instanceof PsiMethod)
						toRemove.addAll(Arrays.asList(((PsiMethod) element).getModifierList().getAnnotations()));
					if (!(element instanceof PsiClass) && !(element instanceof PsiMethod) && !(element instanceof PsiField))
						return true;
					if (element instanceof PsiMethod && PsiClassImplUtil.isMainMethod(
						(PsiMethod) element))
						return false;
					if (element instanceof PsiMethod && ((PsiMethod) element).findSuperMethods().length != 0)
						return false;
					if (element instanceof PsiMethod && ((PsiMethod) element).isConstructor())
						return false;
					if (element instanceof PsiAnonymousClass)
						return false;
					if (element instanceof PsiMethod && mainMethod.equals(((PsiMethod) element).getName())) {
						PsiElement parent = element.getParent();
						if (parent instanceof PsiClass && mainClass.equals(((PsiClass) parent).getQualifiedName()))
							return false;
					}
					if (element instanceof PsiClass && mainClass.equals(((PsiClass) element).getQualifiedName()))
						return true;
					for (PsiReference reference : ReferencesSearch.search(element)) {
						PsiElement referenceElement = reference.getElement();
						while (referenceElement != null && referenceElement != element)
							referenceElement = referenceElement.getParent();
						if (referenceElement == null)
							return element instanceof PsiClass;
					}
					toRemove.add(element);
					return false;
				}

				@Override
				public void visitElement(PsiElement element) {
					if (visitElementImpl(element))
						element.acceptChildren(this);
				}
			});
			if (toRemove.isEmpty())
				break;
			for (PsiElement element : toRemove) {
				if (element.isValid())
					element.delete();
			}
		}
		FileUtilities.synchronizeFile(virtualFile);
	}

	public static String createMainClass(Task task)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("public class Main {\n");
		builder.append("\tpublic static void main(String[] args) {\n");
		if (task.input.type == StreamConfiguration.StreamType.STANDARD)
			builder.append("\t\tInputStream inputStream = System.in;\n");
		else {
			builder.append("\t\tInputStream inputStream;\n");
			builder.append("\t\ttry {\n");
			builder.append("\t\t\tinputStream = new FileInputStream(\"").append(task.input.
				getFileName(task.name, ".in")).append("\");\n");
			builder.append("\t\t} catch (IOException e) {\n");
			builder.append("\t\t\tthrow new RuntimeException(e);\n");
			builder.append("\t\t}\n");
		}
		if (task.output.type == StreamConfiguration.StreamType.STANDARD)
			builder.append("\t\tOutputStream outputStream = System.out;\n");
		else {
			builder.append("\t\tOutputStream outputStream;\n");
			builder.append("\t\ttry {\n");
			builder.append("\t\t\toutputStream = new FileOutputStream(\"").append(task.output.getFileName(task.name,
				".out")).append("\");\n");
			builder.append("\t\t} catch (IOException e) {\n");
			builder.append("\t\t\tthrow new RuntimeException(e);\n");
			builder.append("\t\t}\n");
		}
		String inputClass = Utilities.getData(task.project).inputClass;
		String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
		builder.append("\t\t").append(inputClassShort).append(" in = new ").append(inputClassShort).
			append("(inputStream);\n");
		String outputClass = Utilities.getData(task.project).outputClass;
		String outputClassShort = outputClass.substring(outputClass.lastIndexOf('.') + 1);
		builder.append("\t\t").append(outputClassShort).append(" out = new ").append(outputClassShort).
			append("(outputStream);\n");
		builder.append("\t\t").append(task.name).append(" solver = new ").append(task.name).append("();\n");
		switch (task.testType) {
		case SINGLE:
			builder.append("\t\tsolver.solve(1, in, out);\n");
			builder.append("\t\tout.close();\n");
			break;
		case MULTI_EOF:
			builder.append("\t\ttry {\n");
			builder.append("\t\t\tint testNumber = 1;\n");
			builder.append("\t\t\twhile (true)\n");
			builder.append("\t\t\t\tsolver.solve(testNumber++, in, out);\n");
			builder.append("\t\t} catch (UnknownError e) {\n");
			builder.append("\t\t\tout.close();\n");
			builder.append("\t\t}\n");
			break;
		case MULTI_NUMBER:
			builder.append("\t\tint testCount = Integer.parseInt(in.next());\n");
			builder.append("\t\tfor (int i = 1; i <= testCount; i++)\n");
			builder.append("\t\t\tsolver.solve(i, in, out);\n");
			builder.append("\t\tout.close();\n");
			break;
		}
		builder.append("\t}\n");
		builder.append("}\n\n");
		return builder.toString();
	}

	public static void createSourceFile(final Task task) {
		ApplicationManager.getApplication().runWriteAction(new Runnable() {
			public void run() {
				Set<String> toImport = new HashSet<String>();
				toImport.add("import java.io.InputStream;");
				toImport.add("import java.io.OutputStream;");
				toImport.add("import java.io.IOException;");
				if (task.input.type != StreamConfiguration.StreamType.STANDARD)
					toImport.add("import java.io.FileInputStream;");
				if (task.output.type != StreamConfiguration.StreamType.STANDARD)
					toImport.add("import java.io.FileOutputStream;");
				PsiFile originalSource = FileUtilities.getPsiFile(task.project, task.location + "/" + task.name + ".java");
				final String[] textParts = createInlinedSource(task.project, toImport, originalSource);
				final StringBuilder text = new StringBuilder();
				text.append(textParts[0]);
				text.append(createMainClass(task));
				text.append(textParts[1]);
				String outputDirectory = Utilities.getData(task.project).outputDirectory;
				VirtualFile directory = FileUtilities.createDirectoryIfMissing(task.project, outputDirectory);
				if (directory == null)
					return;
				final VirtualFile file = FileUtilities.writeTextFile(directory, "Main.java", text.toString());
				FileUtilities.synchronizeFile(file);
				removeUnusedCode(task.project, file, "Main", "main");
			}
		});
	}

	public static String createCheckerStub(Task task) {
		PsiDirectory directory = FileUtilities.getPsiDirectory(task.project, task.location);
		String inputClass = Utilities.getData(task.project).inputClass;
		String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
		StringBuilder builder = new StringBuilder();
		String packageName = FileUtilities.getPackage(directory);
		if (packageName != null && packageName.length() != 0)
			builder.append("package ").append(packageName).append(";\n\n");
		builder.append("import ").append(inputClass).append(";\n");
		builder.append("import net.egork.chelper.task.Test;\n");
		builder.append("import net.egork.chelper.tester.Verdict;\n");
		builder.append("\n");
		builder.append("import java.util.Collection;\n");
		builder.append("import java.util.Collections;\n");
		builder.append("\n");
		builder.append("public class ").append(task.name).append("Checker {\n");
		builder.append("\tpublic Verdict check(").append(inputClassShort).append(" input, ").append(inputClassShort)
			.append(" expected, ").append(inputClassShort).append(" actual) {\n");
		builder.append("\t\treturn Verdict.UNDECIDED;\n");
		builder.append("\t}\n");
		builder.append("\n");
		builder.append("\tpublic double getCertainty() {\n");
		builder.append("\t\treturn 0;\n");
		builder.append("\t}\n");
		builder.append("\n");
		builder.append("\tpublic Collection<? extends Test> generateTests() {\n");
		builder.append("\t\treturn Collections.emptyList();\n");
		builder.append("\t}\n");
		builder.append("}\n");
		return builder.toString();
	}

	public static String createStub(Task task) {
		PsiDirectory directory = FileUtilities.getPsiDirectory(task.project, task.location);
		String inputClass = Utilities.getData(task.project).inputClass;
		String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
		String outputClass = Utilities.getData(task.project).outputClass;
		String outputClassShort = outputClass.substring(outputClass.lastIndexOf('.') + 1);
		StringBuilder builder = new StringBuilder();
		String packageName = FileUtilities.getPackage(directory);
		if (packageName != null && packageName.length() != 0)
			builder.append("package ").append(packageName).append(";\n\n");
		builder.append("import ").append(inputClass).append(";\n");
		builder.append("import ").append(outputClass).append(";\n");
		builder.append("\n");
		builder.append("public class ").append(task.name).append(" {\n");
		builder.append("\tpublic void solve(int testNumber, ").append(inputClassShort).append(" in, ").
			append(outputClassShort).append(" out) {\n");
		builder.append("\t}\n");
		builder.append("}\n");
		return builder.toString();
	}

	public static TopCoderTask parseTopCoderStub(VirtualFile file, final Project project) {
		String text = FileUtilities.readTextFile(file);
		if (text == null)
			return null;
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
		String testStart = "switch";
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
				String defaultDir = Utilities.getData(project).defaultDir;
				FileUtilities.createDirectoryIfMissing(project, defaultDir);
				String packageName = FileUtilities.getPackage(FileUtilities.getPsiDirectory(project, defaultDir));
				if (packageName != null && packageName.length() != 0) {
					FileUtilities.writeTextFile(FileUtilities.getFile(project, defaultDir),
						name + ".java", "package " + packageName + ";\n\n" + finalOriginalText);
				} else {
					FileUtilities.writeTextFile(FileUtilities.getFile(project, defaultDir),
						name + ".java", finalOriginalText);
				}
			}
		});
		FileEditorManager.getInstance(project).openFile(FileUtilities.getFile(project,
			Utilities.getData(project).defaultDir + "/" + name + ".java"), true);
		return new TopCoderTask(project, name, methodSignature, tests.toArray(new TopCoderTest[tests.size()]));
	}

	public static void createSourceFile(final TopCoderTask task) {
		ApplicationManager.getApplication().runWriteAction(new Runnable() {
			public void run() {
				PsiFile originalSource = FileUtilities.getPsiFile(task.project,
					Utilities.getData(task.project).defaultDir + "/" + task.name + ".java");
				String[] textParts = createInlinedSource(task.project, Collections.<String>emptySet(),
						originalSource);
				final StringBuilder text = new StringBuilder();
				text.append(textParts[0]);
				text.append("public ");
				text.append(textParts[1]);
				String outputDirectory = Utilities.getData(task.project).topcoderDir;
				VirtualFile directory = FileUtilities.createDirectoryIfMissing(task.project, outputDirectory);
				if (directory == null)
					return;
				final VirtualFile file = FileUtilities.writeTextFile(directory, task.name + ".java", text.toString());
				FileUtilities.synchronizeFile(file);
				removeUnusedCode(task.project, file, task.name, task.signature.name);
			}
		});
	}

	public static void createUnitTest(Task task) {
		if (!Utilities.getData(task.project).enableUnitTests)
			return;
		Test[] tests = task.tests;
		for (int i = 0, testsLength = tests.length; i < testsLength; i++)
			tests[i] = tests[i].setActive(true);
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		String path = Utilities.getData(task.project).testDir + "/on" + year + "_" + month + "_" + day + "/" +
			task.name.toLowerCase();
		String originalPath = path;
		int index = 0;
		while (FileUtilities.getFile(task.project, path) != null)
			path = originalPath + (index++);
		VirtualFile directory = FileUtilities.createDirectoryIfMissing(task.project, path);
		String packageName = FileUtilities.getPackage(FileUtilities.getPsiDirectory(task.project, path));
		if (packageName == null) {
			JOptionPane.showMessageDialog(null, "testDirectory should be under project source");
			return;
		}
		String sourceFile = FileUtilities.readTextFile(FileUtilities.getFile(task.project,
			task.location + "/" + task.name + ".java"));
		sourceFile = changePackage(sourceFile, packageName);
		String checkerFile = FileUtilities.readTextFile(FileUtilities.getFile(task.project,
			task.location + "/" + task.name + "Checker.java"));
		checkerFile = changePackage(checkerFile, packageName);
		FileUtilities.writeTextFile(directory, task.name + ".java", sourceFile);
		FileUtilities.writeTextFile(directory, task.name + "Checker.java", checkerFile);
		String tester = generateTester(task, path);
		tester = changePackage(tester, packageName);
		FileUtilities.writeTextFile(directory, "Main.java", tester);
	}

	public static void createUnitTest(TopCoderTask task) {
		if (!Utilities.getData(task.project).enableUnitTests)
			return;
		TopCoderTest[] tests = task.tests;
		for (int i = 0, testsLength = tests.length; i < testsLength; i++)
			tests[i] = tests[i].setActive(true);
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		String path = Utilities.getData(task.project).testDir + "/on" + year + "_" + month + "_" + day + "/" +
			task.name.toLowerCase();
		String originalPath = path;
		int index = 0;
		while (FileUtilities.getFile(task.project, path) != null)
			path = originalPath + (index++);
		VirtualFile directory = FileUtilities.createDirectoryIfMissing(task.project, path);
		String packageName = FileUtilities.getPackage(FileUtilities.getPsiDirectory(task.project, path));
		if (packageName == null) {
			JOptionPane.showMessageDialog(null, "testDirectory should be under project source");
			return;
		}
		String sourceFile = FileUtilities.readTextFile(FileUtilities.getFile(task.project,
			Utilities.getData(task.project).defaultDir + "/" + task.name + ".java"));
		sourceFile = changePackage(sourceFile, packageName);
		FileUtilities.writeTextFile(directory, task.name + ".java", sourceFile);
		String tester = generateTester(task, path);
		tester = changePackage(tester, packageName);
		FileUtilities.writeTextFile(directory, "Main.java", tester);
	}

	private static String generateTester(Task task, String path) {
		StringBuilder builder = new StringBuilder();
		builder.append("import net.egork.chelper.tester.Tester;\n");
		builder.append("import org.junit.Assert;\n");
		builder.append("import org.junit.Test;\n\n");
		builder.append("public class Main {\n");
		builder.append("\t@Test\n");
		builder.append("\tpublic void test() throws Exception {\n");
		builder.append("\t\tif (!Tester.test(\"").append(Utilities.getData(task.project).inputClass).append("\",\n");
		builder.append("\t\t\t\"")
			.append(FileUtilities.getFQN(FileUtilities.getPsiDirectory(task.project, path), task.name))
			.append("\",\n");
		builder.append("\t\t\t\"").append(task.testType.name()).append("\",\n");
		builder.append("\t\t\t\"").append(escape(EncodingUtilities.encodeTests(task.tests))).append("\",\n");
		builder.append("\t\t\t\"").append(Utilities.getData(task.project).outputClass).append("\"))\n");
		builder.append("\t\t{\n");
		builder.append("\t\t\tAssert.fail();\n");
		builder.append("\t\t}\n");
		builder.append("\t}\n");
		builder.append("}\n");
		return builder.toString();
	}

	private static String generateTester(TopCoderTask task, String path) {
		StringBuilder builder = new StringBuilder();
		builder.append("import net.egork.chelper.tester.TopCoderTester;\n");
		builder.append("import org.junit.Assert;\n");
		builder.append("import org.junit.Test;\n\n");
		builder.append("public class Main {\n");
		builder.append("\t@Test\n");
		builder.append("\tpublic void test() throws Exception {\n");
		builder.append("\t\tif (!TopCoderTester.test(\"").append(task.signature).append("\",\n");
		builder.append("\t\t\t\"")
			.append(FileUtilities.getFQN(FileUtilities.getPsiDirectory(task.project, path), task.name))
			.append("\",\n");
		builder.append("\t\t\t\"").append(escape(EncodingUtilities.encodeTests(task.tests))).append("\"))\n");
		builder.append("\t\t{\n");
		builder.append("\t\t\tAssert.fail();\n");
		builder.append("\t\t}\n");
		builder.append("\t}\n");
		builder.append("}\n");
		return builder.toString();
	}

	private static String escape(String s) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '"')
				builder.append("\\\"");
			else if (c == '\\')
				builder.append("\\\\");
			else
				builder.append(c);
		}
		return builder.toString();
	}

	private static String changePackage(String sourceFile, String packageName) {
		if (sourceFile.startsWith("package ")) {
			int index = sourceFile.indexOf(';');
			if (index == -1)
				return sourceFile;
			sourceFile = sourceFile.substring(index + 1);
		}
		if (packageName.length() == 0)
			return sourceFile;
		sourceFile = "package " + packageName + ";\n\n" + sourceFile;
		return sourceFile;
	}

	public static class InlineVisitor extends PsiElementVisitor {
		private final String[] excluded;
		private final HashSet<PsiClass> set;
		public final List<PsiClass> toInline;
		public final Set<String> toImport;
		private final Project project;

		private InlineVisitor(Project project, PsiFile originalSource) {
			this.project = project;
			excluded = Utilities.getData(project).excludedPackages;
			toImport = new HashSet<String>();
			toInline = new ArrayList<PsiClass>();
			for (PsiElement element : originalSource.getChildren()) {
				if (element instanceof PsiClass)
					toInline.add((PsiClass)element);
			}
			set = new HashSet<PsiClass>(toInline);
			//noinspection ForLoopReplaceableByForEach
			for (int i = 0; i < toInline.size(); i++) {
				PsiClass aClass = toInline.get(i);
				processClass(aClass);
				aClass.acceptChildren(this);
			}
		}

		private void processClass(PsiClass aClass) {
			PsiClass superClass = aClass.getSuperClass();
			addClass(superClass);
			PsiClass[] interfaces = aClass.getInterfaces();
			for (PsiClass aInterface : interfaces)
				addClass(aInterface);
		}

		private void addClass(PsiClass aClass) {
			if (!(aClass.getScope() instanceof PsiFile))
				return;
			if (!shouldSkip(aClass)) {
				if (!set.contains(aClass)) {
					set.add(aClass);
					toInline.add(aClass);
				}
			} else {
				PsiImportStatement aImport = JavaPsiFacade.getElementFactory(project).createImportStatement(aClass);
				toImport.add(aImport.getText());
			}
		}

		@Override
		public void visitElement(PsiElement element) {
			if (element instanceof PsiClass) {
				processClass((PsiClass) element);
			} else if (element instanceof PsiVariable) {
				PsiType type = ((PsiVariable) element).getType();
				processType(type);
			} else if (element instanceof PsiMethod) {
				processMethod((PsiMethod) element);
			} else if (element instanceof PsiMethodCallExpression) {
				PsiMethod method = ((PsiMethodCallExpression) element).resolveMethod();
				if (method != null) {
					addClass(method.getContainingClass());
					processMethod(method);
				}
			} else if (element instanceof PsiNewExpression) {
				processType(((PsiNewExpression) element).getType());
			} else if (element instanceof PsiReferenceExpression) {
				PsiElement resolved = ((PsiReferenceExpression) element).resolve();
				if (resolved instanceof PsiClass)
					addClass((PsiClass) resolved);
			}
			element.acceptChildren(this);
		}

		private void processMethod(PsiMethod element) {
			processType(element.getReturnType());
			for (PsiParameter parameter : element.getParameterList().getParameters())
				processType(parameter.getType());
		}

		private void processType(PsiType type) {
			while (type instanceof PsiArrayType)
				type = ((PsiArrayType) type).getComponentType();
			if (type instanceof PsiClassType)
				addClass(((PsiClassType) type).resolve());
		}

		private boolean shouldSkip(PsiClass aClass) {
			String fqn = aClass.getQualifiedName();
			if (fqn == null)
				return false;
			for (String aPackage : excluded) {
				if (fqn.startsWith(aPackage))
					return true;
			}
			return false;
		}

	}
}
