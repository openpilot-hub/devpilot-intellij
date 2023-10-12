package com.zhongan.codeai.gui.toolwindows.components;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.zhongan.codeai.CodeAIIcons;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;

public class UserChatPanel extends JPanel {
    private final JBTextArea textArea;

    private final JPanel iconsPanel;

    private final Consumer<String> bindEvent;

    private final JButton button;

    public UserChatPanel(Consumer<String> event) {
        this.bindEvent = event;

        textArea = new JBTextArea();
        textArea.setOpaque(false);
        // FIXME
        textArea.setBackground(JBColor.namedColor("Editor.SearchField.background", UIUtil.getTextFieldBackground()));
        textArea.setLineWrap(true);
        // FIXME
        textArea.getEmptyText().setText("Ask me anything");
        textArea.setBorder(JBUI.Borders.empty(8, 4));
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleEvent();
                }
            }
        });

        this.setOpaque(false);
        this.setLayout(new BorderLayout());
        this.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0),
                JBUI.Borders.empty(10)));
        this.add(textArea, BorderLayout.CENTER);

        var flowLayout = new FlowLayout(FlowLayout.RIGHT);
        flowLayout.setHgap(10);
        button = createIconButton(CodeAIIcons.SEND_ICON);
        iconsPanel = new JPanel(flowLayout);
        iconsPanel.add(button);
        this.add(JBUI.Panels.simplePanel().addToBottom(iconsPanel), BorderLayout.EAST);
    }

    public void setIconStop() {
        button.setIcon(CodeAIIcons.STOP_ICON);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(
                CodeAIIcons.STOP_ICON.getIconWidth(), CodeAIIcons.STOP_ICON.getIconHeight()));
    }

    public void setIconSend() {
        button.setIcon(CodeAIIcons.SEND_ICON);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(
                CodeAIIcons.SEND_ICON.getIconWidth(), CodeAIIcons.SEND_ICON.getIconHeight()));
    }

    private void handleEvent() {
        String message = textArea.getText();

        if (StringUtils.isBlank(message)) {
            return;
        }

        bindEvent.accept(message);
        textArea.setText(null);
    }

    private JButton createIconButton(Icon icon) {
        var button = new JButton(icon);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
        button.addActionListener(e -> handleEvent());
        return button;
    }
}
