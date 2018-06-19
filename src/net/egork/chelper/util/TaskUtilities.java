package net.egork.chelper.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.egork.chelper.codegeneration.SolutionGenerator;
import net.egork.chelper.task.Task;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class TaskUtilities {
    public static ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void createSourceFile(Task task, Project project) {
        SolutionGenerator.createSourceFile(task, project);
    }

    public static VirtualFile getFile(String location, String name, Project project) {
        return FileUtilities.getFile(project, location + "/" + name + ".java");
    }

    public static String getTaskFileLocation(String location, String name) {
        if (location != null && name != null) {
            return location + "/" + getTaskFileName(name);
        }
        return null;
    }

    public static String getTaskFileName(String name) {
        if (name == null) {
            return null;
        }
        return canonize(name) + ".json";
    }

    public static String replaceCyrillics(String filename) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < filename.length(); i++) {
            char c = filename.charAt(i);
            switch (c) {
                case 'а':
                    builder.append('a');
                    break;
                case 'б':
                    builder.append('b');
                    break;
                case 'в':
                    builder.append('v');
                    break;
                case 'г':
                    builder.append('g');
                    break;
                case 'д':
                    builder.append('d');
                    break;
                case 'е':
                    builder.append('e');
                    break;
                case 'ё':
                    builder.append("jo");
                    break;
                case 'ж':
                    builder.append('j');
                    break;
                case 'з':
                    builder.append('z');
                    break;
                case 'и':
                    builder.append('i');
                    break;
                case 'й':
                    builder.append('j');
                    break;
                case 'к':
                    builder.append('k');
                    break;
                case 'л':
                    builder.append('l');
                    break;
                case 'м':
                    builder.append('m');
                    break;
                case 'н':
                    builder.append('n');
                    break;
                case 'о':
                    builder.append('o');
                    break;
                case 'п':
                    builder.append('p');
                    break;
                case 'р':
                    builder.append('r');
                    break;
                case 'с':
                    builder.append('s');
                    break;
                case 'т':
                    builder.append('t');
                    break;
                case 'у':
                    builder.append('u');
                    break;
                case 'ф':
                    builder.append('f');
                    break;
                case 'х':
                    builder.append('h');
                    break;
                case 'ц':
                    builder.append('c');
                    break;
                case 'ч':
                    builder.append("ch");
                    break;
                case 'ш':
                    builder.append("sh");
                    break;
                case 'щ':
                    builder.append("sch");
                    break;
                case 'ъ':
                    break;
                case 'ы':
                    builder.append('y');
                    break;
                case 'ь':
                    break;
                case 'э':
                    builder.append('e');
                    break;
                case 'ю':
                    builder.append("ju");
                    break;
                case 'я':
                    builder.append("ja");
                    break;
                case 'А':
                    builder.append('A');
                    break;
                case 'Б':
                    builder.append('B');
                    break;
                case 'В':
                    builder.append('V');
                    break;
                case 'Г':
                    builder.append('G');
                    break;
                case 'Д':
                    builder.append('D');
                    break;
                case 'Е':
                    builder.append('E');
                    break;
                case 'Ё':
                    builder.append("Jo");
                    break;
                case 'Ж':
                    builder.append('J');
                    break;
                case 'З':
                    builder.append('Z');
                    break;
                case 'И':
                    builder.append('I');
                    break;
                case 'Й':
                    builder.append('J');
                    break;
                case 'К':
                    builder.append('K');
                    break;
                case 'Л':
                    builder.append('L');
                    break;
                case 'М':
                    builder.append('M');
                    break;
                case 'Н':
                    builder.append('N');
                    break;
                case 'О':
                    builder.append('O');
                    break;
                case 'П':
                    builder.append('P');
                    break;
                case 'Р':
                    builder.append('R');
                    break;
                case 'С':
                    builder.append('S');
                    break;
                case 'Т':
                    builder.append('T');
                    break;
                case 'У':
                    builder.append('U');
                    break;
                case 'Ф':
                    builder.append('F');
                    break;
                case 'Х':
                    builder.append('H');
                    break;
                case 'Ц':
                    builder.append('C');
                    break;
                case 'Ч':
                    builder.append("Ch");
                    break;
                case 'Ш':
                    builder.append("Sh");
                    break;
                case 'Щ':
                    builder.append("Sch");
                    break;
                case 'Ъ':
                    break;
                case 'Ы':
                    builder.append('Y');
                    break;
                case 'Ь':
                    break;
                case 'Э':
                    builder.append('E');
                    break;
                case 'Ю':
                    builder.append("Ju");
                    break;
                case 'Я':
                    builder.append("Ja");
                    break;
                default:
                    builder.append(c);
            }
        }
        return builder.toString();
    }

    public static String canonize(String filename) {
        filename = filename.replaceAll("[\\\\?%*:|\"<>/]", "-");
        while (filename.endsWith("."))
            filename = filename.substring(0, filename.length() - 1);
        filename = replaceCyrillics(filename);
        return filename;
    }

    public static String createClassName(String name) {
        return canonize(name).replace(" ", "");
    }
}
