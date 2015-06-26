package net.egork.chelper.codegeneration;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import net.egork.chelper.util.Utilities;

import java.util.Collection;

/**
 * @author egor@egork.net
 */
public class MainFileTemplate extends Template {
	final Collection<PsiElement> entryPoints;

	public MainFileTemplate(Collection<PsiElement> entryPoint, String template) {
		super(template);
		this.entryPoints = entryPoint;
	}

	public static PsiClass getClass(Project project, String fqn) {
		return JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
	}

	public static PsiMethod getMethod(Project project, String classFQN, String name, String returnType, String...parameters) {
		PsiClass aClass = getClass(project, classFQN);
		PsiType returns = returnType == null ? null : getTypeByName(project, returnType);
		PsiType[] arguments = new PsiType[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			arguments[i] = getTypeByName(project, parameters[i]);
		}
		for (PsiMethod method : aClass.findMethodsByName(name, false)) {
			if (returns == null && method.getReturnType() == null || returns != null && returns.getCanonicalText().equals(method.getReturnType().getCanonicalText()))
			{
				boolean good = true;
				PsiParameter[] par = method.getParameterList().getParameters();
				if (par.length != arguments.length) {
					continue;
				}
				for (int i = 0; i < par.length; i++) {
					if (!par[i].getType().getCanonicalText().equals(arguments[i].getCanonicalText())) {
						good = false;
					}
				}
				if (good) {
					return method;
				}
			}
		}
		return null;
	}

	private static PsiClassType getTypeByName(Project project, String type) {
		return PsiType.getTypeByName(type, project, GlobalSearchScope.allScope(project));
	}

	public static PsiElement getInputConstructor(Project project) {
		return getConstructor(project, Utilities.getData(project).inputClass, "java.io.InputStream");
	}

	public static PsiElement getOutputConstructor(Project project) {
		return getConstructor(project, Utilities.getData(project).outputClass, "java.io.OutputStream");
	}

	private static PsiElement getConstructor(Project project, String aClass, String...arguments) {
		String methodName = aClass.substring(aClass.lastIndexOf(".") + 1);
		return getMethod(project, aClass, methodName, null, arguments);
	}
}
