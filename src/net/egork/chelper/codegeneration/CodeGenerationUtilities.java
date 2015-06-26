package net.egork.chelper.codegeneration;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import net.egork.chelper.actions.ArchiveAction;
import net.egork.chelper.task.*;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.OutputWriter;
import net.egork.chelper.util.Utilities;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CodeGenerationUtilities {
	public static String createInlinedSource(Project project, final Set<String> excludedPackages, MainFileTemplate template,
		final Set<String> classesToImport, boolean allToInnerClasses, PsiClass firstToDisplay)
	{
		final Queue<PsiElement> queue = new ArrayDeque<PsiElement>();
		final Set<PsiElement> toInline = new HashSet<PsiElement>();
		for (PsiElement element : template.entryPoints) {
			processElement(excludedPackages, queue, toInline, element, classesToImport);
		}
		final PsiElementVisitor visitor = new PsiElementVisitor() {
			@Override
			public void visitElement(PsiElement element) {
				if (element instanceof PsiReference) {
					PsiReference reference = (PsiReference) element;
					PsiElement referenced = reference.resolve();
					if (referenced instanceof PsiField || referenced instanceof PsiMethod || referenced instanceof PsiClass) {
						processElement(excludedPackages, queue, toInline, referenced, classesToImport);
					}
				} else if (element instanceof PsiMethod) {
					//We should get here only if we find anonymous class method. We need to get its super method if present
					for (PsiMethod method : ((PsiMethod) element).findSuperMethods()) {
						processElement(excludedPackages, queue, toInline, method, classesToImport);
					}
				} else if (element instanceof PsiConstructorCall) {
					PsiMethod constructor = ((PsiConstructorCall) element).resolveConstructor();
					if (constructor != null) {
						processElement(excludedPackages, queue, toInline, constructor, classesToImport);
					}
				}
				element.acceptChildren(this);
			}
		};
		while (!queue.isEmpty()) {
			while (!queue.isEmpty()) {
				PsiElement element = queue.poll();
				if (element instanceof PsiField) {
					PsiField field = (PsiField) element;
					processType(excludedPackages, queue, toInline, field.getType().getDeepComponentType(), classesToImport);
					PsiExpression initializer = field.getInitializer();
					if (initializer != null) {
						initializer.accept(visitor);
					}
					processElement(excludedPackages, queue, toInline, field.getContainingClass(), classesToImport);
					if (field instanceof PsiEnumConstant) {
						if (((PsiEnumConstant) field).resolveConstructor() != null) {
							processElement(excludedPackages, queue, toInline, ((PsiEnumConstant) field).resolveConstructor(), classesToImport);
						}
					}
				} else if (element instanceof PsiMethod) {
					PsiMethod method = (PsiMethod) element;
					for (PsiParameter parameter : method.getParameterList().getParameters()) {
						PsiType type = parameter.getType().getDeepComponentType();
						processType(excludedPackages, queue, toInline, type, classesToImport);
					}
					for (PsiMethod superMethods : method.findSuperMethods()) {
						processElement(excludedPackages, queue, toInline, superMethods, classesToImport);
					}
					PsiCodeBlock body = method.getBody();
					if (body != null) {
						body.accept(visitor);
					}
					processElement(excludedPackages, queue, toInline, method.getContainingClass(), classesToImport);
				} else if (element instanceof PsiClass) {
					PsiClass parent = ((PsiClass) element).getContainingClass();
					if (parent != null) {
						processElement(excludedPackages, queue, toInline, parent, classesToImport);
					}
					for (PsiClass superClass : ((PsiClass) element).getSupers()) {
						processElement(excludedPackages, queue, toInline, superClass, classesToImport);
					}
				}
			}
			Set<PsiElement> addOnStep = new HashSet<PsiElement>();
			for (PsiElement element : toInline) {
				if (element instanceof PsiClass) {
					for (PsiMethod method : ((PsiClass) element).getMethods()) {
						if (!toInline.contains(method)) {
							for (PsiMethod parent : method.findSuperMethods()) {
								if (toInline.contains(parent) || !shouldAddElement(parent, excludedPackages)) {
									processElement(excludedPackages, queue, addOnStep, method, classesToImport);
									break;
								}
							}
						}
					}
				}
			}
			toInline.addAll(addOnStep);
			queue.addAll(addOnStep);
		}
		Set<String> single = new HashSet<String>();
		final Set<String> multiple = new HashSet<String>();
		single.add(firstToDisplay.getName());
		for (PsiElement element : toInline) {
			if (element instanceof PsiClass) {
				String name = ((PsiClass) element).getName();
				if (single.contains(name)) {
					multiple.add(name);
				} else {
					single.add(name);
				}
			}
		}
		final StringBuilder inlinedSource = new StringBuilder();
		PsiElementVisitor inlineVisitor = new PsiElementVisitor() {
			@Override
			public void visitElement(PsiElement element) {
				if (element instanceof PsiIdentifier &&
					element.getParent() instanceof PsiReference &&
					((PsiReference)element.getParent()).resolve() instanceof PsiClass) {
					PsiClass aClass = (PsiClass) ((PsiReference) element.getParent()).resolve();
					inlinedSource.append(convertNameFull(aClass, toInline, multiple));
				} else {
					if (element.getFirstChild() == null) {
						inlinedSource.append(element.getText());
					} else {
						element.acceptChildren(this);
					}
				}
			}
		};
		toInline.remove(firstToDisplay);
		addSource(inlinedSource, firstToDisplay, toInline, allToInnerClasses, true, inlineVisitor, multiple);
		for (PsiElement element : toInline) {
			if (element instanceof PsiClass && ((PsiClass) element).getContainingClass() == null && !element.equals(firstToDisplay)) {
				addSource(inlinedSource, (PsiClass) element, toInline, allToInnerClasses, true, inlineVisitor, multiple);
			}
		}
		StringBuilder imports = new StringBuilder();
		for (String aImport : classesToImport) {
			if (!aImport.startsWith("java.lang.")) {
				imports.append("import ").append(aImport).append(";\n");
			}
		}
		Map<String, String> map = new HashMap<String, String>();
		map.put("IMPORTS", imports.toString());
		map.put("INLINED_SOURCE", inlinedSource.toString());
		return template.apply(map);
	}

	private static void addSource(StringBuilder source, PsiClass aClass, Set<PsiElement> toInline,
			boolean convertToStaticInner, boolean removePublic, PsiElementVisitor visitor, Set<String> resolveToFull) {
		PsiModifierList list = aClass.getModifierList();
		String modifierList = list == null ? "" : list.getText();
		if (removePublic) {
			modifierList = modifierList.replace("public ", "").replace(" public", "").replace("public", "");
		}
		if (convertToStaticInner) {
			if (modifierList.isEmpty()) {
				modifierList = "static";
			} else {
				modifierList = "static " + modifierList;
			}
		}
		source.append(modifierList);
		if (!modifierList.isEmpty()) {
			source.append(" ");
		}
		source.append(aClass.isEnum() ? "enum" : aClass.isInterface() ? "interface" : "class").append(' ');
		String className = convertName(aClass, toInline, resolveToFull);
		source.append(className);
		if (aClass.getExtendsList() != null) {
			source.append(' ');
			aClass.getExtendsList().accept(visitor);
		}
		if (aClass.getImplementsList() != null) {
			source.append(' ');
			aClass.getImplementsList().accept(visitor);
		}
		source.append(" {\n");
		boolean fieldAdded = false;
		boolean enumAdded = false;
		for (PsiField field : aClass.getFields()) {
			if (!toInline.contains(field)) {
				continue;
			}
			if (field instanceof PsiEnumConstant) {
				field.accept(visitor);
				source.append(",\n");
				enumAdded = true;
			}
		}
		if (enumAdded) {
			source.append(";\n");
		}
		for (PsiField field : aClass.getFields()) {
			if (!toInline.contains(field)) {
				continue;
			}
			if (!(field instanceof PsiEnumConstant)) {
				PsiModifierList fieldModifierList = field.getModifierList();
				modifierList = fieldModifierList == null ? "" : fieldModifierList.getText();
				source.append(modifierList);
				if (!modifierList.isEmpty()) {
					source.append(" ");
				}
				field.getTypeElement().accept(visitor);
				source.append(' ');
				source.append(field.getName());
				PsiExpression initializer = field.getInitializer();
				if (initializer != null) {
					source.append(" = ");
					initializer.accept(visitor);
				}
				source.append(";\n");
				fieldAdded = true;
			}
		}
		if (fieldAdded) {
			source.append("\n");
		}
		for (PsiMethod method : aClass.getMethods()) {
			if (!toInline.contains(method)) {
				continue;
			}
			PsiModifierList fieldModifierList = method.getModifierList();
			modifierList = fieldModifierList.getText();
			source.append(modifierList);
			if (!modifierList.isEmpty()) {
				source.append(" ");
			}
			if (method.getReturnType() != null) {
				method.getReturnTypeElement().accept(visitor);
				source.append(' ');
				source.append(method.getName());
			} else {
				source.append(className);
			}
			method.getParameterList().accept(visitor);
			if (method.getBody() != null) {
				source.append(' ');
				method.getBody().accept(visitor);
			} else {
				source.append(";");
			}
			source.append("\n\n");
		}
		for (PsiClass innerClass : aClass.getInnerClasses()) {
			if (!toInline.contains(innerClass)) {
				continue;
			}
			addSource(source, innerClass, toInline, false, false, visitor, resolveToFull);
			source.append("\n");
		}
		source.append("}\n");
	}

	private static String convert(PsiType type, Set<PsiElement> toInline, Set<String> resolveToFull) {
		if (type.getArrayDimensions() != 0) {
			StringBuilder result = new StringBuilder();
			result.append(convert(type.getDeepComponentType(), toInline, resolveToFull));
			for (int i = 0; i < type.getArrayDimensions(); i++) {
				result.append("[]");
			}
			return result.toString();
		}
		if (type instanceof PsiClassType) {
			return convertNameFull(((PsiClassType) type).resolve(), toInline, resolveToFull);
		}
		return type.getCanonicalText();
	}

	private static String convertName(PsiClass aClass, Set<PsiElement> toInline, Set<String> resolveToFull) {
		if (aClass.getContainingClass() == null) {
			return convertNameFull(aClass, toInline, resolveToFull);
		} else {
			return aClass.getName();
		}
	}

	private static String convertNameFull(PsiClass aClass, Set<PsiElement> toInline, Set<String> resolveToFull) {
		List<String> inner = new ArrayList<String>();
		while (aClass.getContainingClass() != null) {
			inner.add(aClass.getName());
			aClass = aClass.getContainingClass();
		}
		StringBuilder result = new StringBuilder();
		if (toInline.contains(aClass) && resolveToFull.contains(aClass.getName())) {
			result.append(aClass.getQualifiedName().replace('.', '_'));
		} else {
			result.append(aClass.getName());
		}
		for (String className : inner) {
			result.append('.').append(className);
		}
		return result.toString();
	}

	private static void processElement(Set<String> excludedPackages, Queue<PsiElement> queue, Set<PsiElement> toInline, PsiElement element, Set<String> classesToImport) {
		boolean shouldAdd = shouldAddElement(element, excludedPackages);
		if (element instanceof PsiClass && !shouldAdd) {
			classesToImport.add(((PsiClass) element).getQualifiedName());
		} else if (!toInline.contains(element) && shouldAdd) {
			queue.add(element);
			toInline.add(element);
		}
	}

	private static void processType(Set<String> excludedPackages, Queue<PsiElement> queue, Set<PsiElement> toInline, PsiType type, Set<String> classesToImport) {
		if (type instanceof PsiClassType) {
			PsiClass aClass = ((PsiClassType) type).resolve();
			if (aClass == null) {
				//TODO
				return;
			}
			processElement(excludedPackages, queue, toInline, aClass, classesToImport);
		}
	}

	private static boolean shouldAddElement(PsiElement element, Set<String> excludedPackages) {
		PsiClass containingClass = element instanceof PsiClass ? (PsiClass) element : ((PsiMember)element).getContainingClass();
		if (containingClass == null) {
			//TODO
			return false;
		}
		String qualifiedName = containingClass.getQualifiedName();
		if (qualifiedName == null || qualifiedName.startsWith("_")) {
			//TODO
			return false;
		}
		for (String aPackage : excludedPackages) {
			if (qualifiedName.startsWith(aPackage)) {
				return false;
			}
		}
		return true;
	}

	public static MainFileTemplate createMainClassTemplate(Task task, Project project) {
		StringBuilder builder = new StringBuilder();
		builder.append("%IMPORTS%\n");
		builder.append("/**\n" +
			" * Built using CHelper plug-in\n" +
			" * Actual solution is at the top\n");
		String author = Utilities.getData(project).author;
		if (!author.isEmpty()) {
			builder.append(" * @author ").append(author).append("\n");
		}
		builder.append("*/");
		builder.append("public class ").append(task.mainClass).append(" {\n");
		builder.append("\tpublic static void main(String[] args) {\n");
		if (task.includeLocale)
			builder.append("\t\tLocale.setDefault(Locale.US);\n");
		if (task.input.type == StreamConfiguration.StreamType.STANDARD)
			builder.append("\t\tInputStream inputStream = System.in;\n");
		else if (task.input.type != StreamConfiguration.StreamType.LOCAL_REGEXP) {
			builder.append("\t\tInputStream inputStream;\n");
			builder.append("\t\ttry {\n");
			builder.append("\t\t\tinputStream = new FileInputStream(\"").append(task.input.
				getFileName(task.name, ".in")).append("\");\n");
			builder.append("\t\t} catch (IOException e) {\n");
			builder.append("\t\t\tthrow new RuntimeException(e);\n");
			builder.append("\t\t}\n");
		} else {
			builder.append("\t\tInputStream inputStream;\n");
			builder.append("\t\ttry {\n");
			builder.append("\t\t\tfinal String regex = \"").append(task.input.fileName).append("\";\n");
			builder.append("\t\t\tFile directory = new File(\".\");\n" +
				"\t\t\tFile[] candidates = directory.listFiles(new FilenameFilter() {\n" +
				"\t\t\t\tpublic boolean accept(File dir, String name) {\n" +
				"\t\t\t\t\treturn name.matches(regex);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t});\n" +
				"\t\t\tFile toRun = null;\n" +
				"\t\t\tfor (File candidate : candidates) {\n" +
				"\t\t\t\tif (toRun == null || candidate.lastModified() > toRun.lastModified())\n" +
				"\t\t\t\t\ttoRun = candidate;\n" +
				"\t\t\t}\n" +
				"\t\t\tinputStream = new FileInputStream(toRun);\n");
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
		String inputClass = getSimpleName(task.inputClass);
		builder.append("\t\t").append(inputClass).append(" in = new ").append(inputClass).
			append("(inputStream);\n");
		String outputClass = getSimpleName(task.outputClass);
		builder.append("\t\t").append(outputClass).append(" out = new ").append(outputClass).
			append("(outputStream);\n");
		String className = getSimpleName(task.taskClass);
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
		builder.append("%INLINED_SOURCE%");
		builder.append("}\n\n");
		return new MainFileTemplate(Arrays.asList(MainFileTemplate.getInputConstructor(project),
			MainFileTemplate.getOutputConstructor(project),
			MainFileTemplate.getMethod(project, task.taskClass, "solve", "void", "int", Utilities.getData(project).inputClass, Utilities.getData(project).outputClass),
			MainFileTemplate.getMethod(project, Utilities.getData(project).outputClass, "close", "void")),
			builder.toString());
	}

	public static void createSourceFile(final Task task, final Project project) {
		ApplicationManager.getApplication().runWriteAction(new Runnable() {
			public void run() {
				Set<String> toImport = new HashSet<String>();
				toImport.add("java.io.InputStream");
				toImport.add("java.io.OutputStream");
				toImport.add("java.io.IOException");
				if (task.input.type != StreamConfiguration.StreamType.STANDARD)
					toImport.add("java.io.FileInputStream");
				if (task.output.type != StreamConfiguration.StreamType.STANDARD)
					toImport.add("java.io.FileOutputStream");
				if (task.input.type == StreamConfiguration.StreamType.LOCAL_REGEXP) {
					toImport.add("java.io.File");
					toImport.add("java.io.FilenameFilter");
				}
				if (task.includeLocale)
					toImport.add("java.util.Locale");
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
				String source = createInlinedSource(project, new HashSet<String>(Arrays.asList(Utilities.getData(project).excludedPackages)),
					createMainClassTemplate(task, project), toImport, true, MainFileTemplate.getClass(project, task.taskClass));
				final VirtualFile file = FileUtilities.writeTextFile(directory, task.mainClass + ".java", source);
				FileUtilities.synchronizeFile(file);
				ReformatCodeProcessor processor = new ReformatCodeProcessor(PsiManager.getInstance(project).findFile(file), false);
				processor.run();
				FileUtilities.synchronizeFile(file);
			}
		});
	}

	public static String createCheckerStub(String location, String name, Project project, Task task) {
        PsiDirectory directory = FileUtilities.getPsiDirectory(project, location);
        String inputClass = task.inputClass;
        String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
        String outputClass = task.outputClass;
        String outputClassShort = outputClass.substring(outputClass.lastIndexOf('.') + 1);
        String packageName = FileUtilities.getPackage(directory);
        String template = createCheckerClassTemplateIfNeeded(project);
        return template.replace("%package%", packageName).replace("%InputClass%", inputClassShort).
                replace("%InputClassFQN%", inputClass).replace("%OutputClass%", outputClassShort).
                replace("%OutputClassFQN%", outputClass).replace("%CheckerClass%", name);
	}

	public static String createStub(Task task, String location, String name, Project project) {
		PsiDirectory directory = FileUtilities.getPsiDirectory(project, location);
		String inputClass = task.inputClass;
		String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
		String outputClass = task.outputClass;
		String outputClassShort = outputClass.substring(outputClass.lastIndexOf('.') + 1);
        String packageName = FileUtilities.getPackage(directory);
        String template = createTaskClassTemplateIfNeeded(project, task.template);
        return template.replace("%package%", packageName).replace("%InputClass%", inputClassShort).
            replace("%InputClassFQN%", inputClass).replace("%OutputClass%", outputClassShort).
            replace("%OutputClassFQN%", outputClass).replace("%TaskClass%", name);
	}

    public static String createTaskClassTemplateIfNeeded(Project project, String templateName) {
        VirtualFile file = FileUtilities.getFile(project, templateName == null ? "TaskClass.template" : templateName);
		String result = null;
        if (file != null) {
			result = FileUtilities.readTextFile(file);
		}
		if (result == null && templateName != null) {
			file = FileUtilities.getFile(project, "TaskClass.template");
			if (file != null) {
				result = FileUtilities.readTextFile(file);
			}
		}
		if (result != null) {
			return result;
		}
        String template = "package %package%;\n" +
                "\n" +
                "import %InputClassFQN%;\n" +
                "import %OutputClassFQN%;\n" +
                "\n" +
                "public class %TaskClass% {\n" +
                "    public void solve(int testNumber, %InputClass% in, %OutputClass% out) {\n" +
                "    }\n" +
                "}\n";
        FileUtilities.writeTextFile(project.getBaseDir(), "TaskClass.template", template);
        return template;
    }

    public static String createCheckerClassTemplateIfNeeded(Project project) {
        VirtualFile file = FileUtilities.getFile(project, "CheckerClass.template");
        if (file != null)
            return FileUtilities.readTextFile(file);
        String template = "package %package%;\n" +
                "\n" +
                "import net.egork.chelper.tester.Verdict;\n" +
                "import net.egork.chelper.checkers.Checker;\n" +
                "\n" +
                "public class %CheckerClass% implements Checker {\n" +
                "    public %CheckerClass%(String parameters) {\n" +
                "    }\n" +
                "\n" +
                "    public Verdict check(String input, String expectedOutput, String actualOutput) {\n" +
                "        return Verdict.UNDECIDED;\n" +
                "    }\n" +
                "}\n";
        FileUtilities.writeTextFile(project.getBaseDir(), "CheckerClass.template", template);
        return template;
    }

    public static String createTestCaseClassTemplateIfNeeded(Project project) {
        VirtualFile file = FileUtilities.getFile(project, "TestCaseClass.template");
        if (file != null)
            return FileUtilities.readTextFile(file);
        String template = "package %package%;\n" +
                "\n" +
                "import net.egork.chelper.task.Test;\n" +
                "import net.egork.chelper.tester.TestCase;\n" +
                "\n" +
                "import java.util.Collection;\n" +
                "import java.util.Collections;\n" +
                "\n" +
                "public class %TestCaseClass% {\n" +
                "    @TestCase\n" +
                "    public Collection<Test> createTests() {\n" +
                "        return Collections.emptyList();\n" +
                "    }\n" +
                "}\n";
        FileUtilities.writeTextFile(project.getBaseDir(), "TestCaseClass.template", template);
        return template;
    }

    public static String createTopCoderTestCaseClassTemplateIfNeeded(Project project) {
        VirtualFile file = FileUtilities.getFile(project, "TopCoderTestCaseClass.template");
        if (file != null)
            return FileUtilities.readTextFile(file);
        String template = "package %package%;\n" +
                "\n" +
                "import net.egork.chelper.task.NewTopCoderTest;\n" +
                "import net.egork.chelper.tester.TestCase;\n" +
                "\n" +
                "import java.util.Collection;\n" +
                "import java.util.Collections;\n" +
                "\n" +
                "public class %TestCaseClass% {\n" +
                "    @TestCase\n" +
                "    public Collection<NewTopCoderTest> createTests() {\n" +
                "        return Collections.emptyList();\n" +
                "    }\n" +
                "}\n";
        FileUtilities.writeTextFile(project.getBaseDir(), "TopCoderTestCaseClass.template", template);
        return template;
    }

    public static String createTopCoderTaskTemplateIfNeeded(Project project) {
        VirtualFile file = FileUtilities.getFile(project, "TopCoderTaskClass.template");
        if (file != null)
            return FileUtilities.readTextFile(file);
        String template = "package %package%;\n" +
                "\n" +
                "public class %TaskClass% {\n" +
                "    public %Signature% {\n" +
				"        return %DefaultValue%;\n" +
                "    }\n" +
                "}\n";
        FileUtilities.writeTextFile(project.getBaseDir(), "TopCoderTaskClass.template", template);
        return template;
    }

    public static void createSourceFile(final Project project, final TopCoderTask task) {
		ApplicationManager.getApplication().runWriteAction(new Runnable() {
			public void run() {
				String text = createInlinedSource(project,
					new HashSet<String>(Arrays.asList(Utilities.getData(project).excludedPackages)),
					new MainFileTemplate(Collections.<PsiElement>singleton(task.getMethod(project)),
					"%IMPORTS%\npublic %INLINED_SOURCE%"), new HashSet<String>(), false,
					task.getMethod(project).getContainingClass());
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
				final VirtualFile file = FileUtilities.writeTextFile(directory, task.name + ".java", text);
				FileUtilities.synchronizeFile(file);
				ReformatCodeProcessor processor = new ReformatCodeProcessor(PsiManager.getInstance(project).findFile(file), false);
				processor.run();
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
                    VirtualFile taskFile = directory.findOrCreateChildData(null, ArchiveAction.canonize(finalTask.name) + ".task");
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
                    VirtualFile taskFile = directory.findOrCreateChildData(null, finalTask.name + ".tctask");
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

    public static String createTestStub(String location, String name, Project project, Task task) {
        PsiDirectory directory = FileUtilities.getPsiDirectory(project, location);
        String inputClass = task.inputClass;
        String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
        String outputClass = task.outputClass;
        String outputClassShort = outputClass.substring(outputClass.lastIndexOf('.') + 1);
        String packageName = FileUtilities.getPackage(directory);
        String template = createTestCaseClassTemplateIfNeeded(project);
        return template.replace("%package%", packageName).replace("%InputClass%", inputClassShort).
                replace("%InputClassFQN%", inputClass).replace("%OutputClass%", outputClassShort).
                replace("%OutputClassFQN%", outputClass).replace("%TestCaseClass%", name);
    }

    public static String createTopCoderStub(TopCoderTask task, Project project, String packageName) {
        String template = createTopCoderTaskTemplateIfNeeded(project);
        StringBuilder signature = new StringBuilder();
        signature.append(task.signature.result.getSimpleName()).append(" ").append(task.signature.name).append("(");
        for (int i = 0; i < task.signature.arguments.length; i++) {
            if (i != 0)
                signature.append(", ");
            signature.append(task.signature.arguments[i].getSimpleName()).append(' ').append(task.signature.argumentNames[i]);
        }
		signature.append(')');
        return template.replace("%package%", packageName).replace("%TaskClass%", task.name).
			replace("%Signature%", signature.toString()).replace("%DefaultValue%", task.defaultValue());
    }

	public static String createTopCoderTestStub(Project project, String aPackage, String name) {
		String template = createTopCoderTestCaseClassTemplateIfNeeded(project);
		return template.replace("%package%", aPackage).replace("%TestCaseClass%", name);
	}
}
