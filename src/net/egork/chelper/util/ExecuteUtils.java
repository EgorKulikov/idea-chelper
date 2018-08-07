package net.egork.chelper.util;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;

/**
 * Created by Scruel on 2017/9/8.
 * Github : https://github.com/scruel
 */
public class ExecuteUtils {
    private ExecuteUtils() {
    }

    public static void executeStrictWriteAction(final Runnable action) {
        final Application application = ApplicationManager.getApplication();

        if (application.isDispatchThread() && application.isWriteAccessAllowed()) {
            action.run();
        } else {
            application.invokeLater(
                    new Runnable() {
                        @Override
                        public void run() {
                            application.runWriteAction(action);
                        }
                    }
            );
        }
    }

    public static void executeStrictWriteActionAndWait(final Runnable action) {
        final Application application = ApplicationManager.getApplication();

        application.invokeAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        application.runWriteAction(action);
                    }
                }
        );
    }
}
