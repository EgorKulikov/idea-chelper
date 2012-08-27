package net.egork.chelper.topcoder;

import javax.swing.*;
import java.awt.*;

/**
 * @author Egor Kulikov
 */
public class MessagePanel extends JPanel {
    public static final Color INFO_COLOR = Color.WHITE;
    public static final Color ERROR_COLOR = Color.RED;

    private JLabel messageLabel;

    public MessagePanel() {
        super(new BorderLayout());
        messageLabel = new JLabel();
        add(messageLabel, BorderLayout.CENTER);
    }

    public void showErrorMessage(final String message) {
        setMessage(message, ERROR_COLOR);
    }

    public void showInfoMessage(final String message) {
        setMessage(message, INFO_COLOR);
    }

    private void setMessage(final String message, final Color color) {
        messageLabel.setForeground(color);
        messageLabel.setText(message);
    }
}
