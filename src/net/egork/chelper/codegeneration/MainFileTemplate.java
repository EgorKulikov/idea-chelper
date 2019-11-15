package net.egork.chelper.codegeneration;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.FileContentUtil;
import net.egork.chelper.util.Utilities;

import java.util.Collection;
import java.util.Collections;

/**
 * @author egor@egork.net
 */
public class MainFileTemplate extends Template {
    final Collection<PsiElement> entryPoints;
    final Collection<String> imports;

    public MainFileTemplate(String template, Collection<PsiElement> entryPoint, Collection<String> imports) {
        super(template);
        this.entryPoints = entryPoint;
        this.imports = imports;
    }

    public String resolve(String source, String className, Collection<String> additionalImports) {
        StringBuilder imports = new StringBuilder();
        for (String aImport : this.imports) {
            if (!aImport.matches("java[.]lang[.][^.]*")) {
                imports.append("import ").append(aImport).append(";\n");
            }
        }
        for (String aImport : additionalImports) {
            if (!aImport.matches("java[.]lang[.][^.]*")) {
                imports.append("import ").append(aImport).append(";\n");
            }
        }
        return apply("IMPORTS", imports.toString(), "INLINED_SOURCE", source, "CLASS_NAME", className);
    }

    public static PsiClass getClass(Project project, String fqn) {
        FileContentUtil.reparseFiles(project, Collections.singleton(project.getBaseDir()), true);
        return JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
    }

    public static PsiMethod getMethod(Project project, String classFQN, String name, String returnType, String... parameters) {
        PsiClass aClass = getClass(project, classFQN);
        PsiType returns = returnType == null ? null : getTypeByName(project, returnType);
        PsiType[] arguments = new PsiType[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            arguments[i] = getTypeByName(project, parameters[i]);
        }
        for (PsiMethod method : aClass.findMethodsByName(name, false)) {
            if (returns == null && method.getReturnType() == null || returns != null && returns.getCanonicalText().equals(method.getReturnType().getCanonicalText())) {
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

    private static PsiElement getConstructor(Project project, String aClass, String... arguments) {
        String methodName = aClass.substring(aClass.lastIndexOf(".") + 1);
        return getMethod(project, aClass, methodName, null, arguments);
    }
}
