package net.egork.chelper.task;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import net.egork.chelper.util.CodeGenerationUtilities;
import net.egork.chelper.util.FileUtilities;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Task {
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

	public Task setTests(Test[] tests) {
		return new Task(name, location, testType, input, output, heapMemory, stackMemory, project, tests);
	}

	public Task setProject(Project project) {
		return new Task(name, location, testType, input, output, heapMemory, stackMemory, project, tests);
	}

	public PsiElement initialize() {
		if (location == null)
			return null;
		FileUtilities.createDirectoryIfMissing(project, location);
		PsiDirectory directory = FileUtilities.getPsiDirectory(project, location);
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
		String mainFileContent = CodeGenerationUtilities.createStub(this);
		VirtualFile file = FileUtilities
			.writeTextFile(FileUtilities.getFile(project, location), name + ".java", mainFileContent);
		if (file == null)
			return null;
		return PsiManager.getInstance(project).findFile(file);
	}

	private PsiElement createCheckerClass() {
		String checkerFileContent = CodeGenerationUtilities.createCheckerStub(this);
		VirtualFile file = FileUtilities.writeTextFile(FileUtilities.getFile(project, location), name +
			"Checker.java", checkerFileContent);
		if (file == null)
			return null;
		return PsiManager.getInstance(project).findFile(file);
	}

	public String getFQN() {
		return FileUtilities.getFile(project, location).getPath().replace('/', '.') + name;
	}

	public void createSourceFile() {
		CodeGenerationUtilities.createSourceFile(this);
	}
}
