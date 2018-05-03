package net.egork.chelper.topcoder;

import javax.swing.*;
import java.awt.*;

/**
 * @author Egor Kulikov
 */
public class MessagePanel extends JPanel {
    private JLabel message = new JLabel();

    public MessagePanel() {
        super(new BorderLayout());
        message = new JLabel();
        message.setBackground(Color.BLACK);
        add(message, BorderLayout.NORTH);
    }

    public void showErrorMessage(final String message) {
        addMessage(message, Color.RED);
    }

    public void showInfoMessage(final String message) {
        addMessage(message, Color.WHITE);
    }

    private void addMessage(String message, Color color) {
        this.message.setText(message);
        this.message.setForeground(color);
    }
}
