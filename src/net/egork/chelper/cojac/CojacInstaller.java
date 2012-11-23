package net.egork.chelper.cojac;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CojacInstaller {
    public static void install(final Project project) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
            if (project == null)
                return;
            VirtualFile root = project.getBaseDir();
            if (root == null)
                return;
            try {
                VirtualFile cojacJar = root.findChild("cojac.jar");
                if (cojacJar == null) {
                    cojacJar = root.createChildData(null, "cojac.jar");
                    OutputStream outputStream = cojacJar.getOutputStream(null);
                    InputStream inputStream = getClass().getResourceAsStream("/cojac/cojac.jar");
                    StreamUtil.copyStreamContent(inputStream, outputStream);
                    outputStream.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            }
        });
    }
}
