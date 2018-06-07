package net.egork.chelper.topcoder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.topcoder.client.contestant.ProblemComponentModel;
import com.topcoder.shared.language.JavaLanguage;
import com.topcoder.shared.language.Language;
import com.topcoder.shared.problem.DataType;
import com.topcoder.shared.problem.Renderer;
import com.topcoder.shared.problem.TestCase;
import net.egork.chelper.task.MethodSignature;
import net.egork.chelper.task.NewTopCoderTest;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TopCoderTask;

import javax.swing.*;
import java.io.*;
import java.util.Arrays;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class CHelperArenaPlugin implements ArenaPlugin {
    private static ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private MessagePanel messagePanel;
    public static final int PORT = 4242;
    private ProblemComponentModel last = null;

    public JPanel getEditorPanel() {
        return messagePanel;
    }

    public String getSource() {
        if (last == null) {
            return "";
        }
        try {
            Message message = new Message(PORT);
            message.out.printString(Message.GET_SOURCE);
            message.out.printString(last.getClassName());
            String response = message.in.readString();
            if (!Message.OK.equals(response)) {
                messagePanel.showErrorMessage("Something went wrong :(");
                return "";
            }
            return message.in.readString();
        } catch (IOException e) {
            messagePanel.showInfoMessage("Probably socket was not opened, trying file method");
            StringBuilder source = new StringBuilder();
            try {
                File file = new File(System.getProperty("user.home") + File.separator + ".java");
                Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                int next;
                while ((next = reader.read()) != -1)
                    source.append((char) next);
                reader.close();
                return source.toString();
            } catch (IOException e1) {
                messagePanel.showErrorMessage("Both socket and file methods failed to retrieve source");
                return "";
            }
        }
    }

    public void setSource(String source) {
    }

    public void setProblemComponent(ProblemComponentModel componentModel, Language language, Renderer renderer) {
        System.out.println("Set problem component");
        last = componentModel;
        if (!(language instanceof JavaLanguage)) {
            messagePanel.showErrorMessage("Only Java language is supported");
            return;
        }
        String name = componentModel.getClassName();
        String methodName = componentModel.getMethodName();
        Class result = getClass(componentModel.getReturnType());
        if (result == null) {
            return;
        }
        Class[] arguments = new Class[componentModel.getParamTypes().length];
        for (int i = 0; i < arguments.length; i++) {
            if ((arguments[i] = getClass(componentModel.getParamTypes()[i])) == null) {
                return;
            }
        }
        MethodSignature signature = new MethodSignature(methodName, result, arguments, componentModel.getParamNames());
        String date = Task.getDateString();
        String contestName = getFullContestName(componentModel.getProblem().getRound().getContestName());
        NewTopCoderTest[] tests = new NewTopCoderTest[componentModel.getTestCases().length];
        for (int i = 0; i < tests.length; i++) {
            TestCase testCase = componentModel.getTestCases()[i];
            Object testResult = NewTopCoderTest.parse(testCase.getOutput(), result);
            Object[] testArguments = new Object[testCase.getInput().length];
            System.out.println(testCase.getInput().length + " " + arguments.length);
            System.out.println(Arrays.toString(testCase.getInput()));
            for (int j = 0; j < testArguments.length; j++)
                testArguments[j] = NewTopCoderTest.parse(testCase.getInput()[j], arguments[j]);
            tests[i] = new NewTopCoderTest(testArguments, testResult, i, true);
        }
        // NOTE: we set failOnOverflow to false here, but it will be overridden on the receiving end.
        TopCoderTask task = new TopCoderTask(name, signature, tests, date, contestName, new String[0], null, false,
                componentModel.getComponent().getMemLimitMB() + "M");
        /*try {
            Message message = new Message(PORT);
            message.out.printString(Message.NEW_TASK);
            message.out.flush();

            mapper.writeValue(message.out.out, task);
            message.out.flush();
            String response = message.in.readString();
            if (Message.OK.equals(response)) {
                messagePanel.showInfoMessage("Task created");
            } else if (Message.OTHER_ERROR.equals(response)) {
                messagePanel.showErrorMessage("Something went wrong :(");
            }
        } catch (IOException e) {
            messagePanel.showInfoMessage("Probably socket was not opened, trying file method");*/
        try {
            String path = new BufferedReader(new InputStreamReader(new FileInputStream(System.getProperty("user.home") + File.separator + ".chelper"))).readLine();
            File file = new File(path + File.separator + ".tcjson");
            mapper.writeValue(file, task);
            file.deleteOnExit();
//                    FileOutputStream outputStream = new FileOutputStream(file);
//                    task.saveTask(new OutputWriter(outputStream));
//                    outputStream.close();
        } catch (IOException e1) {
            messagePanel.showErrorMessage("File method also failed");
        }
//        }
    }

    private String getFullContestName(String contestName) {
        if (contestName.startsWith("SRM") && contestName.split(" ").length >= 2) {
            return "TopCoder SRM #" + contestName.split(" ")[1];
        }
        if (contestName.startsWith("TCO") && contestName.split(" ").length >= 4) {
            return "TopCoder Open Round #" + contestName.split(" ")[3];
        }
        return contestName.replace("TCO", "TopCoder Open");
    }

    private Class getClass(DataType type) {
        String description = type.getDescriptor(JavaLanguage.JAVA_LANGUAGE);
        if ("int".equals(description)) {
            return int.class;
        }
        if ("long".equals(description)) {
            return long.class;
        }
        if ("double".equals(description)) {
            return double.class;
        }
        if ("String".equals(description)) {
            return String.class;
        }
        if ("int[]".equals(description)) {
            return int[].class;
        }
        if ("long[]".equals(description)) {
            return long[].class;
        }
        if ("double[]".equals(description)) {
            return double[].class;
        }
        if ("String[]".equals(description)) {
            return String[].class;
        }
        messagePanel.showErrorMessage("Unknown type " + description);
        return null;
    }

    public void startUsing() {
        System.out.println("Start using");
        messagePanel = new MessagePanel();
    }
}
