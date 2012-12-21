package net.egork.chelper.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import net.egork.chelper.actions.ArchiveAction;
import net.egork.chelper.task.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
					if (element instanceof PsiTypeParameter)
						return false;
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

	public static String createMainClass(Task task, Project project) {
		StringBuilder builder = new StringBuilder();
		builder.append("public class ").append(task.mainClass).append(" {\n");
		builder.append("\tpublic static void main(String[] args) {\n");
		if (task.includeLocale)
			builder.append("\t\tLocale.setDefault(Locale.US);\n");
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
		String inputClass = task.inputClass;
		String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
		builder.append("\t\t").append(inputClassShort).append(" in = new ").append(inputClassShort).
			append("(inputStream);\n");
		String outputClass = task.outputClass;
		String outputClassShort = outputClass.substring(outputClass.lastIndexOf('.') + 1);
		builder.append("\t\t").append(outputClassShort).append(" out = new ").append(outputClassShort).
			append("(outputStream);\n");
		String className = Utilities.getSimpleName(task.taskClass);
		builder.append("\t\t").append(className).append(" solver = new ").append(className).append("();\n");
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

	public static void createSourceFile(final Task task, final Project project) {
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
				if (task.includeLocale)
					toImport.add("import java.util.Locale;");
				VirtualFile originalFile = FileUtilities.getFileByFQN(task.taskClass, project);
				PsiFile originalSource = PsiManager.getInstance(project).findFile(originalFile);
				final String[] textParts = createInlinedSource(project, toImport, originalSource);
				final StringBuilder text = new StringBuilder();
				text.append(textParts[0]);
				text.append(createMainClass(task, project));
				text.append(textParts[1]);
				String outputDirectory = Utilities.getData(project).outputDirectory;
				VirtualFile directory = FileUtilities.createDirectoryIfMissing(project, outputDirectory);
				if (directory == null)
					return;
                for (VirtualFile file : directory.getChildren()) {
                    if ("java".equals(file.getExtension())) {
                        try {
                            file.delete(null);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
				final VirtualFile file = FileUtilities.writeTextFile(directory, task.mainClass + ".java", text.toString());
				FileUtilities.synchronizeFile(file);
				removeUnusedCode(project, file, task.mainClass, "main");
			}
		});
	}

	public static String createCheckerStub(String location, String name, Project project) {
		PsiDirectory directory = FileUtilities.getPsiDirectory(project, location);
		StringBuilder builder = new StringBuilder();
		String packageName = FileUtilities.getPackage(directory);
		if (packageName != null && packageName.length() != 0)
			builder.append("package ").append(packageName).append(";\n\n");
		builder.append("import net.egork.chelper.tester.Verdict;\n");
        builder.append("import net.egork.chelper.checkers.Checker;\n");
        builder.append("\n");
		builder.append("public class ").append(name).append(" implements Checker {\n");
        builder.append("\tpublic ").append(name).append("(String parameters) {\n");
        builder.append("\t}\n\n");
		builder.append("\tpublic Verdict check(String input, String expectedOutput, String actualOutput) {\n");
		builder.append("\t\treturn Verdict.UNDECIDED;\n");
		builder.append("\t}\n");
		builder.append("}\n");
		return builder.toString();
	}

	public static String createStub(Task task, String location, String name, Project project) {
		PsiDirectory directory = FileUtilities.getPsiDirectory(project, location);
		String inputClass = task.inputClass;
		String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
		String outputClass = task.outputClass;
		String outputClassShort = outputClass.substring(outputClass.lastIndexOf('.') + 1);
		StringBuilder builder = new StringBuilder();
		String packageName = FileUtilities.getPackage(directory);
		if (packageName != null && packageName.length() != 0)
			builder.append("package ").append(packageName).append(";\n\n");
		builder.append("import ").append(inputClass).append(";\n");
		builder.append("import ").append(outputClass).append(";\n");
		builder.append("\n");
		builder.append("public class ").append(name).append(" {\n");
		builder.append("\tpublic void solve(int testNumber, ").append(inputClassShort).append(" in, ").
			append(outputClassShort).append(" out) {\n");
		builder.append("\t}\n");
		builder.append("}\n");
		return builder.toString();
	}

	public static void createSourceFile(final Project project, final TopCoderTask task) {
		ApplicationManager.getApplication().runWriteAction(new Runnable() {
			public void run() {
				PsiFile originalSource = FileUtilities.getPsiFile(project,
					Utilities.getData(project).defaultDirectory + "/" + task.name + ".java");
				String[] textParts = createInlinedSource(project, Collections.<String>emptySet(),
						originalSource);
				final StringBuilder text = new StringBuilder();
				text.append(textParts[0]);
				text.append("public ");
				text.append(textParts[1]);
				String outputDirectory = Utilities.getData(project).outputDirectory;
				VirtualFile directory = FileUtilities.createDirectoryIfMissing(project, outputDirectory);
				if (directory == null)
					return;
				for (VirtualFile file : directory.getChildren()) {
					if ("java".equals(file.getExtension())) {
						try {
							file.delete(null);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
				final VirtualFile file = FileUtilities.writeTextFile(directory, task.name + ".java", text.toString());
				FileUtilities.synchronizeFile(file);
				removeUnusedCode(project, file, task.name, task.signature.name);
				String source = FileUtilities.readTextFile(file);
				VirtualFile virtualFile = FileUtilities.writeTextFile(LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home")), ".java", source);
				new File(virtualFile.getCanonicalPath()).deleteOnExit();
			}
		});
	}

	public static void createUnitTest(Task task, final Project project) {
		if (!Utilities.getData(project).enableUnitTests)
			return;
		Test[] tests = task.tests;
		for (int i = 0, testsLength = tests.length; i < testsLength; i++)
			tests[i] = tests[i].setActive(true);
		String path = Utilities.getData(project).testDirectory + "/on" + canonize(firstPart(task.date), false)  + "/on" + canonize(task.date, false) + "_" + canonize(task.contestName, false) + "/" +
			canonize(task.name, true);
        task = task.setTests(tests);
		String originalPath = path;
		int index = 0;
		while (FileUtilities.getFile(project, path) != null)
			path = originalPath + (index++);
		final VirtualFile directory = FileUtilities.createDirectoryIfMissing(project, path);
		final String packageName = FileUtilities.getPackage(FileUtilities.getPsiDirectory(project, path));
		if (packageName == null) {
			JOptionPane.showMessageDialog(null, "testDirectory should be under project source");
			return;
		}
        PsiElement main = JavaPsiFacade.getInstance(project).findClass(task.taskClass, GlobalSearchScope.allScope(project));
        VirtualFile mainFile = main == null ? null : main.getContainingFile() == null ? null : main.getContainingFile().getVirtualFile();
        String mainContent = FileUtilities.readTextFile(mainFile);
        mainContent = changePackage(mainContent, packageName);
        String taskClassSimple = getSimpleName(task.taskClass);
        FileUtilities.writeTextFile(directory, taskClassSimple + ".java", mainContent);
        task = task.setTaskClass(packageName + "." + taskClassSimple);
        PsiElement checker = JavaPsiFacade.getInstance(project).findClass(task.checkerClass, GlobalSearchScope.allScope(project));
        VirtualFile checkerFile = checker == null ? null : checker.getContainingFile() == null ? null : checker.getContainingFile().getVirtualFile();
        if (checkerFile != null && mainFile != null && checkerFile.getParent().equals(mainFile.getParent())) {
            String checkerContent = FileUtilities.readTextFile(checkerFile);
            checkerContent = changePackage(checkerContent, packageName);
            String checkerClassSimple = getSimpleName(task.checkerClass);
            FileUtilities.writeTextFile(directory, checkerClassSimple + ".java", checkerContent);
            task = task.setCheckerClass(packageName + "." + checkerClassSimple);
        }
        String[] testClasses = Arrays.copyOf(task.testClasses, task.testClasses.length);
        for (int i = 0; i < testClasses.length; i++) {
            PsiElement test = JavaPsiFacade.getInstance(project).findClass(task.testClasses[i], GlobalSearchScope.allScope(project));
            VirtualFile testFile = test == null ? null : test.getContainingFile() == null ? null : test.getContainingFile().getVirtualFile();
            String testContent = FileUtilities.readTextFile(testFile);
            testContent = changePackage(testContent, packageName);
            String testClassSimple = getSimpleName(testClasses[i]);
            FileUtilities.writeTextFile(directory, testClassSimple + ".java", testContent);
            testClasses[i] = packageName + "." + testClassSimple;
        }
        task = task.setTestClasses(testClasses);
        final Task finalTask = task;
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                String taskFilePath;
                try {
                    VirtualFile taskFile = directory.createChildData(null, ArchiveAction.canonize(finalTask.name) + ".task");
                    OutputStream outputStream = taskFile.getOutputStream(null);
                    finalTask.saveTask(new OutputWriter(outputStream));
                    outputStream.close();
                    taskFilePath = FileUtilities.getRelativePath(project.getBaseDir(), taskFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String tester = generateTester(taskFilePath);
                tester = changePackage(tester, packageName);
                FileUtilities.writeTextFile(directory, "Main.java", tester);
            }
        });
	}

    private static String getSimpleName(String className) {
        int position = className.lastIndexOf('.');
        if (position != -1)
            className = className.substring(position + 1);
        return className;
    }

    private static String canonize(String token, boolean firstIsLetter) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < token.length(); i++) {
			if (firstIsLetter && i == 0 && Character.isDigit(token.charAt(0)))
				result.append('_');
			if (Character.isLetterOrDigit(token.charAt(i)) && token.charAt(i) < 128)
				result.append(token.charAt(i));
			else
				result.append('_');
		}
        return result.toString();
    }

    public static void createUnitTest(TopCoderTask task, final Project project) {
		if (!Utilities.getData(project).enableUnitTests)
			return;
		NewTopCoderTest[] tests = task.tests;
		for (int i = 0, testsLength = tests.length; i < testsLength; i++)
			tests[i] = tests[i].setActive(true);
		String path = Utilities.getData(project).testDirectory + "/on" + canonize(firstPart(task.date), false) + "/on" + canonize(task.date, false) + "_" + canonize(task.contestName, false) + "/" +
			canonize(task.name, true);
        task = task.setTests(tests);
		String originalPath = path;
		int index = 0;
		while (FileUtilities.getFile(project, path) != null)
			path = originalPath + (index++);
		final VirtualFile directory = FileUtilities.createDirectoryIfMissing(project, path);
		final String packageName = FileUtilities.getPackage(FileUtilities.getPsiDirectory(project, path));
		if (packageName == null) {
			JOptionPane.showMessageDialog(null, "testDirectory should be under project source");
			return;
		}
        VirtualFile mainFile = FileUtilities.getFile(project, Utilities.getData(project).defaultDirectory + "/" + task.name + ".java");
        String mainContent = FileUtilities.readTextFile(mainFile);
        mainContent = changePackage(mainContent, packageName);
        String taskClassSimple = task.name;
        FileUtilities.writeTextFile(directory, taskClassSimple + ".java", mainContent);
        task = task.setFQN(packageName + "." + taskClassSimple);
        String[] testClasses = Arrays.copyOf(task.testClasses, task.testClasses.length);
        for (int i = 0; i < testClasses.length; i++) {
            PsiElement test = JavaPsiFacade.getInstance(project).findClass(task.testClasses[i], GlobalSearchScope.allScope(project));
            VirtualFile testFile = test == null ? null : test.getContainingFile() == null ? null : test.getContainingFile().getVirtualFile();
            String testContent = FileUtilities.readTextFile(testFile);
            testContent = changePackage(testContent, packageName);
            String testClassSimple = getSimpleName(testClasses[i]);
            FileUtilities.writeTextFile(directory, testClassSimple + ".java", testContent);
            testClasses[i] = packageName + "." + testClassSimple;
        }
        task = task.setTestClasses(testClasses);
        final TopCoderTask finalTask = task;
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                String taskFilePath;
                try {
                    VirtualFile taskFile = directory.createChildData(null, finalTask.name + ".tctask");
                    OutputStream outputStream = taskFile.getOutputStream(null);
                    finalTask.saveTask(new OutputWriter(outputStream));
                    outputStream.close();
                    taskFilePath = FileUtilities.getRelativePath(project.getBaseDir(), taskFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String tester = generateTopCoderTester(taskFilePath);
                tester = changePackage(tester, packageName);
                FileUtilities.writeTextFile(directory, "Main.java", tester);
            }
        });
	}

	private static String firstPart(String date) {
		int position = date.indexOf('.');
		if (position != -1)
			position = date.indexOf('.', position + 1);
		if (position != -1)
			return date.substring(0, position);
		return date;
	}

	private static String generateTopCoderTester(String taskPath) {
		StringBuilder builder = new StringBuilder();
		builder.append("import net.egork.chelper.tester.NewTopCoderTester;\n\n");
		builder.append("import org.junit.Assert;\n");
		builder.append("import org.junit.Test;\n\n");
		builder.append("public class Main {\n");
		builder.append("\t@Test\n");
		builder.append("\tpublic void test() throws Exception {\n");
		builder.append("\t\tif (!NewTopCoderTester.test(\"").append(taskPath).append("\"))\n");
		builder.append("\t\t\tAssert.fail();\n");
		builder.append("\t}\n");
		builder.append("}\n");
		return builder.toString();
	}

	private static String generateTester(String taskPath) {
		StringBuilder builder = new StringBuilder();
		builder.append("import net.egork.chelper.tester.NewTester;\n\n");
		builder.append("import org.junit.Assert;\n");
		builder.append("import org.junit.Test;\n\n");
		builder.append("public class Main {\n");
		builder.append("\t@Test\n");
		builder.append("\tpublic void test() throws Exception {\n");
		builder.append("\t\tif (!NewTester.test(\"").append(taskPath).append("\"))\n");
		builder.append("\t\t\tAssert.fail();\n");
		builder.append("\t}\n");
		builder.append("}\n");
		return builder.toString();
	}

//	private static String generateTester(TopCoderTask task, String path) {
//		StringBuilder builder = new StringBuilder();
//		builder.append("import net.egork.chelper.tester.TopCoderTester;\n");
//		builder.append("import org.junit.Assert;\n");
//		builder.append("import org.junit.Test;\n\n");
//		builder.append("public class Main {\n");
//		builder.append("\t@Test\n");
//		builder.append("\tpublic void test() throws Exception {\n");
//		builder.append("\t\tif (!TopCoderTester.test(\"").append(task.signature).append("\",\n");
//		builder.append("\t\t\t\"")
//			.append(FileUtilities.getFQN(FileUtilities.getPsiDirectory(task.project, path), task.name))
//			.append("\",\n");
//		builder.append("\t\t\t\"").append(escape(EncodingUtilities.encodeTests(task.tests))).append("\"))\n");
//		builder.append("\t\t{\n");
//		builder.append("\t\t\tAssert.fail();\n");
//		builder.append("\t\t}\n");
//		builder.append("\t}\n");
//		builder.append("}\n");
//		return builder.toString();
//	}

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

	public static String changePackage(String sourceFile, String packageName) {
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

    public static String createTestStub(String location, String name, Project project) {
        PsiDirectory directory = FileUtilities.getPsiDirectory(project, location);
        StringBuilder builder = new StringBuilder();
        String packageName = FileUtilities.getPackage(directory);
        if (packageName != null && packageName.length() != 0)
            builder.append("package ").append(packageName).append(";\n\n");
        builder.append("import net.egork.chelper.task.Test;\n");
        builder.append("import net.egork.chelper.tester.TestProvider;\n");
        builder.append("\n");
        builder.append("import java.util.Collection;\n");
        builder.append("import java.util.Collections;\n");
        builder.append("\n");
        builder.append("public class ").append(name).append(" implements TestProvider {\n");
        builder.append("\tpublic Collection<Test> createTests() {\n");
        builder.append("\t\treturn Collections.emptyList();\n");
        builder.append("\t}\n");
        builder.append("}\n");
        return builder.toString();
    }

    public static String createTopCoderStub(TopCoderTask task) {
        StringBuilder builder = new StringBuilder();
        builder.append("public class ").append(task.name).append(" {\n");
        builder.append("\tpublic ").append(task.signature.result.getSimpleName()).append(" ").append(task.signature.name).append("(");
        for (int i = 0; i < task.signature.arguments.length; i++) {
            if (i != 0)
                builder.append(", ");
            builder.append(task.signature.arguments[i].getSimpleName()).append(' ').append(task.signature.argumentNames[i]);
        }
        builder.append(") {\n");
        builder.append("\t}\n");
        builder.append("}\n");
        return builder.toString();
    }

	public static String createTopCoderTestStub(String location, String name, Project project) {
		PsiDirectory directory = FileUtilities.getPsiDirectory(project, location);
  StringBuilder builder = new StringBuilder();
  String packageName = FileUtilities.getPackage(directory);
  if (packageName != null && packageName.length() != 0)
      builder.append("package ").append(packageName).append(";\n\n");
  builder.append("import net.egork.chelper.task.NewTopCoderTest;\n");
  builder.append("import net.egork.chelper.tester.TopCoderTestProvider;\n");
  builder.append("\n");
  builder.append("import java.util.Collection;\n");
  builder.append("import java.util.Collections;\n");
  builder.append("\n");
  builder.append("public class ").append(name).append(" implements TopCoderTestProvider {\n");
  builder.append("\tpublic Collection<NewTopCoderTest> createTests() {\n");
  builder.append("\t\treturn Collections.emptyList();\n");
  builder.append("\t}\n");
  builder.append("}\n");
  return builder.toString();
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
			} else if (element instanceof PsiJavaCodeReferenceElement) {
				PsiElement resolved = ((PsiJavaCodeReferenceElement) element).resolve();
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
