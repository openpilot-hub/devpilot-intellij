package com.zhongan.codeai.gui.toolwindows.chat;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.zhongan.codeai.gui.toolwindows.components.ChatDisplayPanel;
import com.zhongan.codeai.integrations.llms.LlmProviderFactory;
import com.zhongan.codeai.integrations.llms.entity.CodeAIChatCompletionRequest;
import com.zhongan.codeai.integrations.llms.entity.CodeAIMessage;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;

import org.apache.commons.lang3.StringUtils;

public class CodeAIChatToolWindow {
    private final JPanel codeAIChatToolWindowPanel;

    private final JPanel userChatPanel;

    private final JBTextArea textArea;

    private final ScrollablePanel chatContentPanel;

    public JPanel getCodeAIChatToolWindowPanel() {
        return codeAIChatToolWindowPanel;
    }

    public CodeAIChatToolWindow(Project project, ToolWindow toolWindow) {
        this.codeAIChatToolWindowPanel = new JPanel(new GridBagLayout());
        this.chatContentPanel = new ScrollablePanel();
        this.userChatPanel = new JPanel();

        textArea = new JBTextArea();
        textArea.setOpaque(false);
        textArea.setBackground(JBColor.namedColor("Editor.SearchField.background", UIUtil.getTextFieldBackground()));
        textArea.setLineWrap(true);
        textArea.getEmptyText().setText("Ask me anything");
        textArea.setBorder(JBUI.Borders.empty(8, 4));
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // send message
                    String message = textArea.getText();

                    if (StringUtils.isBlank(message)) {
                        return;
                    }

                    showChatContent(message);
                    syncSendAndDisplay(project, message);
                    textArea.setText(null);
                }
            }
        });

        userChatPanel.setOpaque(false);
        userChatPanel.setLayout(new BorderLayout());
        userChatPanel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0),
                JBUI.Borders.empty(10)));
        userChatPanel.add(textArea, BorderLayout.CENTER);

        var gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;

        chatContentPanel.setLayout(new BoxLayout(chatContentPanel, BoxLayout.Y_AXIS));
        JBScrollPane scrollPane = new JBScrollPane(chatContentPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);
        codeAIChatToolWindowPanel.add(scrollPane, gbc);

        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 1;

        codeAIChatToolWindowPanel.add(userChatPanel, gbc);
    }

    private void showChatContent(String content) {
        var gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JTextPane text = new JTextPane();
        text.setLayout(new BoxLayout(text, BoxLayout.PAGE_AXIS));
        text.setText(content);
        text.setEditable(false);

        ChatDisplayPanel chatDisplayPanel = new ChatDisplayPanel().setText(text);
        chatContentPanel.add(chatDisplayPanel, gbc);
        chatContentPanel.revalidate();
        chatContentPanel.repaint();
    }

    private String sendMessage(Project project, String message) {
        CodeAIMessage codeAIMessage = new CodeAIMessage();
        // FIXME
        codeAIMessage.setRole("user");
        codeAIMessage.setContent(message);

        CodeAIChatCompletionRequest request = new CodeAIChatCompletionRequest();
        request.setMessages(List.of(codeAIMessage));
        return new LlmProviderFactory().getLlmProvider(project).chatCompletion(request);
    }

    private void syncSendAndDisplay(Project project, String message) {
        // FIXME
        new Thread(() -> {
            String result = sendMessage(project, message);
            showChatContent(result);
        }).start();
    }
}
