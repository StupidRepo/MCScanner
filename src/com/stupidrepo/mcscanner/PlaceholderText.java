package com.stupidrepo.mcscanner;

import javax.swing.*;
import java.awt.event.FocusAdapter;

public class PlaceholderText {
    private final String text;
    private String lastText = "";
    private final JTextField textField;

    public PlaceholderText(String text, JTextField textField) {
        this.text = text;
        this.textField = textField;
    }

    public FocusAdapter getFocusAdapter() {
        return new FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textField.setText(lastText);
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                lastText = textField.getText();
                if (textField.getText().isEmpty()) {
                    textField.setText(text);
                }
            }
        };
    }
}
