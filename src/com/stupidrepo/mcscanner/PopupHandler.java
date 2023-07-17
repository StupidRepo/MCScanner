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
        JFrame threadFrame = new JFrame("MCScanner");
        threadFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        threadFrame.setSize(500, 125);
        threadFrame.setLayout(new BorderLayout());

        JLabel threadLabel = new JLabel(this.message);
        threadLabel.setHorizontalAlignment(0);

        threadFrame.add(threadLabel, "North");

        JTextField threadField = new JTextField(this.defaultText);
        threadField.setHorizontalAlignment(0);

        threadFrame.add(threadField, "Center");

        JButton threadButton = new JButton(this.buttonText);
        threadFrame.add(threadButton, "East");

        threadButton.addActionListener(e -> {
            this.responseText = threadField.getText();
            threadFrame.setVisible(false);
            threadFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            threadFrame.dispatchEvent(new WindowEvent(threadFrame, 201));
        });

        threadFrame.setVisible(true);

        while(threadFrame.isVisible()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        threadFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }
}
