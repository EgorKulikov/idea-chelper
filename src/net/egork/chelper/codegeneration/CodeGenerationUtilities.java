package net.egork.chelper.codegeneration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import net.egork.chelper.task.NewTopCoderTest;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.TaskUtilities;
import net.egork.chelper.util.Utilities;

import javax.swing.*;
import java.util.Arrays;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CodeGenerationUtilities {
    public static String createCheckerStub(String location, String name, Project project, Task task) {
        PsiDirectory directory = FileUtilities.getPsiDirectory(project, location);
        String inputClass = task.inputClass;
        String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
        String outputClass = task.outputClass;
        String outputClassShort = outputClass.substring(outputClass.lastIndexOf('.') + 1);
        String packageName = FileUtilities.getPackage(directory);
        String template = createCheckerClassTemplateIfNeeded(project);
        return new Template(template).apply("package", packageName, "InputClass", inputClassShort, "InputClassFQN",
                inputClass, "OutputClass", outputClassShort, "OutputClassFQN", outputClass, "CheckerClass", name);
    }

    public static String createInteractorStub(String location, String name, Project project, Task task) {
        PsiDirectory directory = FileUtilities.getPsiDirectory(project, location);
        String inputClass = task.inputClass;
        String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
        String outputClass = task.outputClass;
        String outputClassShort = outputClass.substring(outputClass.lastIndexOf('.') + 1);
        String packageName = FileUtilities.getPackage(directory);
        String template = createInteractorClassTemplateIfNeeded(project);
        return new Template(template).apply("package", packageName, "InputClass", inputClassShort, "InputClassFQN",
                inputClass, "OutputClass", outputClassShort, "OutputClassFQN", outputClass, "InteractorClass", name);
    }

    public static String createStub(Task task, String location, String name, Project project) {
        PsiDirectory directory = FileUtilities.getPsiDirectory(project, location);
        String inputClass = task.inputClass;
        String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
        String outputClass = task.outputClass;
        String outputClassShort = outputClass.substring(outputClass.lastIndexOf('.') + 1);
        String packageName = FileUtilities.getPackage(directory);
        String template = createTaskClassTemplateIfNeeded(project, task.template);
        return new Template(template).apply("package", packageName, "InputClass", inputClassShort, "InputClassFQN",
                inputClass, "OutputClass", outputClassShort, "OutputClassFQN", outputClass, "TaskClass", name);
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
        if (file != null) {
            return FileUtilities.readTextFile(file);
        }
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

    public static String createInteractorClassTemplateIfNeeded(Project project) {
        VirtualFile file = FileUtilities.getFile(project, "InteractorClass.template");
        if (file != null) {
            return FileUtilities.readTextFile(file);
        }
        String template = "package %package%;\n" +
                "\n" +
                "import net.egork.chelper.tester.Verdict;\n" +
                "import net.egork.chelper.tester.State;\n" +
                "import java.io.InputStream;\n" +
                "import java.io.OutputStream;\n" +
                "\n" +
                "public class %InteractorClass% {\n" +
                "    public Verdict interact(InputStream input, InputStream solutionOutput, OutputStream solutionInput, State<Boolean> state) {\n" +
                "        return Verdict.UNDECIDED;\n" +
                "    }\n" +
                "}\n";
        FileUtilities.writeTextFile(project.getBaseDir(), "InteractorClass.template", template);
        return template;
    }

    public static String createTestCaseClassTemplateIfNeeded(Project project) {
        VirtualFile file = FileUtilities.getFile(project, "TestCaseClass.template");
        if (file != null) {
            return FileUtilities.readTextFile(file);
        }
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
        if (file != null) {
            return FileUtilities.readTextFile(file);
        }
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
        if (file != null) {
            return FileUtilities.readTextFile(file);
        }
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

    public static void createUnitTest(Task task, final Project project) {
        if (!Utilities.getData(project).enableUnitTests) {
            return;
        }
        Test[] tests = task.tests;
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
        PsiElement interactor = task.interactor == null ? null : JavaPsiFacade.getInstance(project).findClass(task.interactor, GlobalSearchScope.allScope(project));
        VirtualFile interactorFile = interactor == null ? null : interactor.getContainingFile() == null ? null : interactor.getContainingFile().getVirtualFile();
        if (interactorFile != null && mainFile != null && interactorFile.getParent().equals(mainFile.getParent())) {
            String interactorContent = FileUtilities.readTextFile(interactorFile);
            interactorContent = changePackage(interactorContent, packageName);
            String interactorClassSimple = getSimpleName(task.interactor);
            FileUtilities.writeTextFile(directory, interactorClassSimple + ".java", interactorContent);
            task = task.setInteractor(packageName + "." + interactorClassSimple);
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
                String taskFileName = TaskUtilities.getTaskFileName(finalTask.name);
                FileUtilities.saveConfiguration(taskFileName, finalTask, directory);
                VirtualFile taskFile = directory.findChild(taskFileName);
                String taskFilePath = FileUtilities.getRelativePath(project.getBaseDir(), taskFile);
                String tester = generateTester(taskFilePath);
                tester = changePackage(tester, packageName);
                FileUtilities.writeTextFile(directory, "Main.java", tester);
            }
        });
    }

    private static String canonize(String token, boolean firstIsLetter) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            if (firstIsLetter && i == 0 && Character.isDigit(token.charAt(0))) {
                result.append('_');
            }
            if (Character.isLetterOrDigit(token.charAt(i)) && token.charAt(i) < 128) {
                result.append(token.charAt(i));
            } else {
                result.append('_');
            }
        }
        return result.toString();
    }

    public static void createUnitTest(TopCoderTask task, final Project project) {
        if (!Utilities.getData(project).enableUnitTests) {
            return;
        }
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
                String taskFileName = TaskUtilities.getTaskFileName(finalTask.name);
                FileUtilities.saveConfiguration(taskFileName, finalTask, directory);
                VirtualFile taskFile = directory.findChild(taskFileName);
                String taskFilePath = FileUtilities.getRelativePath(project.getBaseDir(), taskFile);
                String tester = generateTopCoderTester(taskFilePath);
                tester = changePackage(tester, packageName);
                FileUtilities.writeTextFile(directory, "Main.java", tester);
            }
        });
    }

    private static String firstPart(String date) {
        int position = date.indexOf('.');
        if (position != -1) {
            position = date.indexOf('.', position + 1);
        }
        if (position != -1) {
            return date.substring(0, position);
        }
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
            if (index == -1) {
                return sourceFile;
            }
            sourceFile = sourceFile.substring(index + 1);
        }
        if (packageName.length() == 0) {
            return sourceFile;
        }
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
        return new Template(template).apply("package", packageName, "InputClass", inputClassShort, "InputClassFQN",
                inputClass, "OutputClass", outputClassShort, "OutputClassFQN", outputClass, "TestCaseClass", name);
    }

    public static String createTopCoderStub(TopCoderTask task, Project project, String packageName) {
        String template = createTopCoderTaskTemplateIfNeeded(project);
        StringBuilder signature = new StringBuilder();
        signature.append(task.signature.result).append(" ").append(task.signature.name).append("(");
        for (int i = 0; i < task.signature.arguments.length; i++) {
            if (i != 0) {
                signature.append(", ");
            }
            signature.append(task.signature.arguments[i]).append(' ').append(task.signature.argumentNames[i]);
        }
        signature.append(')');
        return new Template(template).apply("package", packageName, "TaskClass", task.name, "Signature",
                signature.toString(), "DefaultValue", task.defaultValue());
    }

    public static String createTopCoderTestStub(Project project, String aPackage, String name) {
        String template = createTopCoderTestCaseClassTemplateIfNeeded(project);
        return new Template(template).apply("package", aPackage, "TestCaseClass", name);
    }

    static String getSimpleName(String className) {
        int position = className.lastIndexOf('.');
        if (position != -1) {
            className = className.substring(position + 1);
        }
        return className;
    }
}
