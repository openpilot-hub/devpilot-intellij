package com.zhongan.codeai.gui.toolwindows.components;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jetbrains.annotations.Nullable;

public class ChatDisplayPanel extends JPanel {
    private final ResponseInfo responseInfo;

    public ChatDisplayPanel() {
        super(new BorderLayout());
        this.responseInfo = new ResponseInfo();
        this.add(responseInfo, BorderLayout.CENTER);
    }

    public ChatDisplayPanel setText(JComponent text) {
        responseInfo.setText(text);
        return this;
    }

    static class ResponseInfo extends JPanel {
        private @Nullable JComponent text;

        ResponseInfo() {
            super(new BorderLayout());
            setBorder(JBUI.Borders.compound(
                    JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
                    JBUI.Borders.empty(5, 10, 10, 10)));
        }

        public void setText(JComponent text) {
            this.text = text;
            this.add(text);
        }

        public JComponent getText() {
            return text;
        }
    }
}
