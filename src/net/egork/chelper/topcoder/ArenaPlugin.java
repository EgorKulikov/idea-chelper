package net.egork.chelper.topcoder;

import com.topcoder.client.contestant.ProblemComponentModel;
import com.topcoder.shared.language.Language;
import com.topcoder.shared.problem.Renderer;

import javax.swing.*;

public interface ArenaPlugin {
    JPanel getEditorPanel();

    String getSource();

    @SuppressWarnings({"UnusedDeclaration"})
        // Required by TopCoder Editor Plugin API
    void setSource(String source);

    void setProblemComponent(ProblemComponentModel componentModel, Language language, Renderer renderer);

    void startUsing();
}
