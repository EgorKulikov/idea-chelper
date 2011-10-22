package net.egork.chelper.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.search.searches.ReferencesSearch;
import net.egork.chelper.Utilities;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Task {
	private static final String[] classStarts = {"class ", "abstract class", "interface ", "enum "};
	public static final String SEPARATOR = "::";
	public final String name;
	public final String location;
	public final TestType testType;
	public final StreamConfiguration input;
	public final StreamConfiguration output;
	public final Test[] tests;
	public final String heapMemory;
	public final String stackMemory;
	public final Project project;

	public Task(String name, String location, TestType testType, StreamConfiguration input,
		StreamConfiguration output, String heapMemory, String stackMemory, Project project)
	{
		this(name, location, testType, input, output, heapMemory, stackMemory, project, new Test[0]);
	}

	public Task(String name, String location, TestType testType, StreamConfiguration input,
		StreamConfiguration output, String heapMemory, String stackMemory, Project project, Test[] tests)
	{
		this.name = name;
		this.location = location;
		this.testType = testType;
		this.input = input;
		this.output = output;
		this.tests = tests;
		this.heapMemory = heapMemory;
		this.stackMemory = stackMemory;
		this.project = project;
	}

	public Task setName(String name) {
		return new Task(name, location, testType, input, output, heapMemory, stackMemory, project, tests);
	}

	public Task setDirectory(String location) {
		return new Task(name, location, testType, input, output, heapMemory, stackMemory, project, tests);
	}

	public Task setTestType(TestType testType) {
		return new Task(name, location, testType, input, output, heapMemory, stackMemory, project, tests);
	}

	public Task setInput(StreamConfiguration input) {
		return new Task(name, location, testType, input, output, heapMemory, stackMemory, project, tests);
	}

	public Task setOutput(StreamConfiguration output) {
		return new Task(name, location, testType, input, output, heapMemory, stackMemory, project, tests);
	}

	public Task setTests(Test[] tests) {
		return new Task(name, location, testType, input, output, heapMemory, stackMemory, project, tests);
	}

	public Task setHeapMemory(String heapMemory) {
		return new Task(name, location, testType, input, output, heapMemory, stackMemory, project, tests);
	}

	public Task setStackMemory(String stackMemory) {
		return new Task(name, location, testType, input, output, heapMemory, stackMemory, project, tests);
	}

	public Task setProject(Project project) {
		return new Task(name, location, testType, input, output, heapMemory, stackMemory, project, tests);
	}

	public PsiElement initialize() {
		if (location == null)
			return null;
		PsiDirectory directory = Utilities.getPsiDirectory(project, location);
		if (directory == null)
			return null;
		PsiClass[] psiClasses = JavaDirectoryService.getInstance().getClasses(directory);
		Map<String, PsiClass> classes = new HashMap<String, PsiClass>();
		for (PsiClass psiClass : psiClasses)
			classes.put(psiClass.getName(), psiClass);
		PsiElement main;
		if (!classes.containsKey(name))
			main = createMainClass();
		else
			main = classes.get(name);
		if (!classes.containsKey(name + "Checker"))
			createCheckerClass();
		return main;
	}

	private PsiElement createMainClass() {
		String mainFileContent = buildMainFile();
		VirtualFile file = Utilities.writeTextFile(Utilities.getFile(project, location), name + ".java", mainFileContent);
		if (file == null)
			return null;
		return PsiManager.getInstance(project).findFile(file);
	}

	private String buildMainFile() {
		PsiDirectory directory = Utilities.getPsiDirectory(project, location);
		String inputClass = Utilities.getData(project).inputClass;
		String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
		StringBuilder builder = new StringBuilder();
		String packageName = Utilities.getPackage(directory);
		if (packageName != null && packageName.length() != 0)
			builder.append("package ").append(packageName).append(";\n\n");
		builder.append("import ").append(inputClass).append(";\n");
		builder.append("import java.io.PrintWriter;\n");
		builder.append("\n");
		builder.append("public class ").append(name).append(" {\n");
		builder.append("\tpublic void solve(int testNumber, ").append(inputClassShort).append(" in, PrintWriter out) {\n");
		builder.append("\t}\n");
		builder.append("}\n");
		return builder.toString();
	}

	private PsiElement createCheckerClass() {
		String checkerFileContent = buildCheckerFile();
		VirtualFile file = Utilities.writeTextFile(Utilities.getFile(project, location), name +
			"Checker.java", checkerFileContent);
		if (file == null)
			return null;
		return PsiManager.getInstance(project).findFile(file);
	}

	private String buildCheckerFile() {
		PsiDirectory directory = Utilities.getPsiDirectory(project, location);
		String inputClass = Utilities.getData(project).inputClass;
		String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
		StringBuilder builder = new StringBuilder();
		String packageName = Utilities.getPackage(directory);
		if (packageName != null && packageName.length() != 0)
			builder.append("package ").append(packageName).append(";\n\n");
		builder.append("import ").append(inputClass).append(";\n");
		builder.append("import net.egork.chelper.task.Test;\n");
		builder.append("\n");
		builder.append("import java.util.Collection;\n");
		builder.append("import java.util.Collections;\n");
		builder.append("\n");
		builder.append("public class ").append(name).append("Checker {\n");
		builder.append("\tpublic String check(").append(inputClassShort).append(" input, ").append(inputClassShort)
			.append(" expected, ").append(inputClassShort).append(" actual) {\n");
		builder.append("\t\treturn \"\";\n");
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

	public String getFQN() {
		return Utilities.getFile(project, location).getPath().replace('/', '.') + name;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(name).append(SEPARATOR).append(location).append(SEPARATOR).append(testType).append(SEPARATOR).
			append(input.type).append(SEPARATOR).
			append(input.type == StreamConfiguration.StreamType.CUSTOM ? input.fileName : "").append(SEPARATOR).
			append(output.type).append(SEPARATOR).
			append(output.type == StreamConfiguration.StreamType.CUSTOM ? output.fileName : "").append(SEPARATOR).
			append(heapMemory).append(SEPARATOR).append(stackMemory).append(SEPARATOR);
		builder.append(encodeTests());
		return builder.toString();
	}

	public static Task read(String text, Project project) {
		String[] tokens = text.split(SEPARATOR, -1);
		String name = tokens[0];
		String location = tokens[1];
		TestType testType;
		try {
			testType = TestType.valueOf(tokens[2]);
		} catch (IllegalArgumentException e) {
			testType = null;
		}
		StreamConfiguration.StreamType inputType;
		try {
			inputType = StreamConfiguration.StreamType.valueOf(tokens[3]);
		} catch (IllegalArgumentException e) {
			inputType = null;
		}
		String inputFileName = null;
		if (inputType == StreamConfiguration.StreamType.CUSTOM)
			inputFileName = tokens[4];
		StreamConfiguration.StreamType outputType;
		try {
			outputType = StreamConfiguration.StreamType.valueOf(tokens[5]);
		} catch (IllegalArgumentException e) {
			outputType = null;
		}
		String outputFileName = null;
		if (outputType == StreamConfiguration.StreamType.CUSTOM)
			outputFileName = tokens[6];
		String heapMemory = tokens[7];
		String stackMemory = tokens[8];
		if ("empty".equals(tokens[9])) {
			return new Task(name, location, testType, new StreamConfiguration(inputType, inputFileName),
				new StreamConfiguration(outputType, outputFileName), heapMemory, stackMemory, project);
		}
		Test[] tests = new Test[tokens.length - 9];
		for (int i = 0; i < tests.length; i++)
			tests[i] = Test.decode(i, tokens[9 + i]);
		return new Task(name, location, testType, new StreamConfiguration(inputType, inputFileName),
			new StreamConfiguration(outputType, outputFileName), heapMemory, stackMemory, project, tests);
	}

	public String encodeTests() {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (Test test : tests) {
			if (first)
				first = false;
			else
				builder.append(SEPARATOR);
			builder.append(test.encode());
		}
		if (builder.length() == 0)
			return "empty";
		return builder.toString();
	}

	public void createSourceFile() {
		ApplicationManager.getApplication().runReadAction(new Runnable() {
			public void run() {
				Set<String> toImport = new HashSet<String>();
				toImport.add("import java.io.InputStream;");
				toImport.add("import java.io.OutputStream;");
				toImport.add("import java.io.IOException;");
				if (input.type != StreamConfiguration.StreamType.STANDARD)
					toImport.add("import java.io.FileInputStream;");
				if (output.type != StreamConfiguration.StreamType.STANDARD)
					toImport.add("import java.io.FileOutputStream;");
				PsiFile originalSource = Utilities.getPsiFile(project, location + "/" + name + ".java");
				final String[] textParts = generateInlinedSource(project, toImport, originalSource);
				final StringBuilder text = new StringBuilder();
				text.append(textParts[0]);
				text.append(generateMainClass());
				text.append(textParts[1]);
				ApplicationManager.getApplication().invokeLater(new Runnable() {
					public void run() {
						String outputDirectory = Utilities.getData(project).outputDirectory;
						VirtualFile directory = Utilities.createDirectoryIfMissing(project, outputDirectory);
						if (directory == null)
							return;
						final VirtualFile file = Utilities.writeTextFile(directory, "Main.java", text.toString());
						Utilities.synchronizeFile(file);
						Utilities.getData(project).queue.run(new com.intellij.openapi.progress.Task.Backgroundable(
							project, "Creating source file") {
							public void run(@NotNull ProgressIndicator indicator) {
								indicator.setText("Removing unused code");
								removeUnusedCode(project, file, "Main", "main");
							}
						});
					}
				});
			}
		});
	}

	public static String[] generateInlinedSource(Project project, Set<String> mandatoryImports,
		PsiFile originalSource)
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
		ApplicationManager.getApplication().invokeLater(new Runnable() {
			public void run() {
				ApplicationManager.getApplication().runWriteAction(new Runnable() {
					public void run() {
						PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
						if (file == null)
							return;
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
						Utilities.synchronizeFile(virtualFile);
					}
				});

			}
		});
	}

	private String generateMainClass() {
		StringBuilder builder = new StringBuilder();
		builder.append("public class Main {\n");
		builder.append("\tpublic static void main(String[] args) {\n");
		if (input.type == StreamConfiguration.StreamType.STANDARD)
			builder.append("\t\tInputStream inputStream = System.in;\n");
		else {
			builder.append("\t\tInputStream inputStream;\n");
			builder.append("\t\ttry {\n");
			builder.append("\t\t\tinputStream = new FileInputStream(\"").append(input.getFileName(name, ".in")).append("\");\n");
			builder.append("\t\t} catch (IOException e) {\n");
			builder.append("\t\t\tthrow new RuntimeException(e);\n");
			builder.append("\t\t}\n");
		}
		if (output.type == StreamConfiguration.StreamType.STANDARD)
			builder.append("\t\tOutputStream outputStream = System.out;\n");
		else {
			builder.append("\t\tOutputStream outputStream;\n");
			builder.append("\t\ttry {\n");
			builder.append("\t\t\toutputStream = new FileOutputStream(\"").append(output.getFileName(name,
				".out")).append("\");\n");
			builder.append("\t\t} catch (IOException e) {\n");
			builder.append("\t\t\tthrow new RuntimeException(e);\n");
			builder.append("\t\t}\n");
		}
		String inputClass = Utilities.getData(project).inputClass;
		String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
		builder.append("\t\t").append(inputClassShort).append(" in = new ").append(inputClassShort).append("(inputStream);\n");
		builder.append("\t\tPrintWriter out = new PrintWriter(outputStream);\n");
		builder.append("\t\t").append(name).append(" solver = new ").append(name).append("();\n");
		if (testType == TestType.SINGLE) {
			builder.append("\t\tsolver.solve(1, in, out);\n");
			builder.append("\t\tout.close();\n");
		} else if (testType == TestType.MULTI_EOF) {
			builder.append("\t\ttry {\n");
			builder.append("\t\t\tint testNumber = 1;\n");
			builder.append("\t\t\twhile (true)\n");
			builder.append("\t\t\t\tsolver.solve(testNumber++, in, out);\n");
			builder.append("\t\t} catch (UnknownError e) {\n");
			builder.append("\t\t\tout.close();\n");
			builder.append("\t\t}\n");
		} else {
			builder.append("\t\tint testCount = Integer.parseInt(in.next());\n");
			builder.append("\t\tfor (int i = 1; i <= testCount; i++)\n");
			builder.append("\t\t\tsolver.solve(i, in, out);\n");
			builder.append("\t\tout.close();\n");
		}
		builder.append("\t}\n");
		builder.append("}\n\n");
		return builder.toString();
	}

	private static class InlineVisitor extends PsiElementVisitor {
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
