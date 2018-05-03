package net.egork.chelper;

import com.intellij.openapi.components.ApplicationComponent;
import net.egork.chelper.util.SSLUtils;
import net.egork.chelper.util.Utilities;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CHelperMain implements ApplicationComponent {
    public CHelperMain() {
    }

    public void initComponent() {
        Utilities.addListeners();
        SSLUtils.trustAllHostnames();
        SSLUtils.trustAllHttpsCertificates();
    }

    public void disposeComponent() {
    }

    @NotNull
    public String getComponentName() {
        return "CHelperMain";
    }
}
