package com.stupidrepo.mcscanner;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.event.WindowEvent;

public class PopupHandler {
    private final String message;
    private final String defaultText;
    private final String buttonText;

    public String responseText;

    public PopupHandler(String message, String defaultText, String buttonText) {
        this.message = message;
        this.defaultText = defaultText;
        this.buttonText = buttonText;
    }

    public void showAndWait() {
        JFrame frame = new JFrame("MCScanner");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(500, 125);
        frame.setLayout(new BorderLayout());

        JLabel messageLabel = new JLabel(this.message);
        messageLabel.setHorizontalAlignment(0);

        frame.add(messageLabel, "North");

        JTextField textField = new JTextField(this.defaultText);
        textField.setHorizontalAlignment(0);

        frame.add(textField, "Center");

        JButton threadButton = new JButton(this.buttonText);
        frame.add(threadButton, "South");

        threadButton.addActionListener(e -> {
            this.responseText = textField.getText();
            frame.setVisible(false);
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.dispatchEvent(new WindowEvent(frame, 201));
        });

        frame.setVisible(true);

        while(frame.isVisible()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }
}
