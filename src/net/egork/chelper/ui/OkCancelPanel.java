package net.egork.chelper.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public abstract class OkCancelPanel extends JPanel {
    protected final JButton okButton;
    protected final JButton cancelButton;
    private final Action okAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            onOk();
        }
    };
    private final Action cancelAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            onCancel();
        }
    };

    public OkCancelPanel(LayoutManager layout) {
        super(layout);
        okButton = new JButton();
        cancelButton = new JButton();
        okButton.setAction(okAction);
        cancelButton.setAction(cancelAction);
        okButton.setText("Ok");
        cancelButton.setText("Cancel");
    }

    public OkCancelPanel() {
        this(new BorderLayout());
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
        initialize(comp);
    }

    private void initialize(Component comp) {
        if (comp instanceof JComponent) {
            JComponent component = (JComponent) comp;
            component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "ok");
            component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
            component.getActionMap().put("ok", okAction);
            component.getActionMap().put("cancel", cancelAction);
            for (int i = component.getComponentCount() - 1; i >= 0; i--)
                initialize(component.getComponent(i));
        }
    }

    public abstract void onOk();

    public abstract void onCancel();

    public JButton getOkButton() {
        return okButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }
}
