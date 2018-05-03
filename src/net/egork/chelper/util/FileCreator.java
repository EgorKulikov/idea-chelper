package net.egork.chelper.util;

import com.intellij.openapi.project.Project;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public interface FileCreator {
    public String createFile(Project project, String path, String name);

    public boolean isValid(String name);
}
