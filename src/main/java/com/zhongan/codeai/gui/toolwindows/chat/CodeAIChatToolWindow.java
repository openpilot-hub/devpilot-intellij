package com.zhongan.codeai.gui.toolwindows.chat;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import com.zhongan.codeai.gui.toolwindows.components.ChatDisplayPanel;
import com.zhongan.codeai.gui.toolwindows.components.UserChatPanel;
import com.zhongan.codeai.integrations.llms.LlmProviderFactory;
import com.zhongan.codeai.integrations.llms.entity.CodeAIChatCompletionRequest;
import com.zhongan.codeai.integrations.llms.entity.CodeAIMessage;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;

public class CodeAIChatToolWindow {
    private final JPanel codeAIChatToolWindowPanel;

    private final JPanel userChatPanel;

    private final ScrollablePanel chatContentPanel;

    private final Project project;

    private final AsyncProcessIcon loadingSpinner;

    public JPanel getCodeAIChatToolWindowPanel() {
        return codeAIChatToolWindowPanel;
    }

    public CodeAIChatToolWindow(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.loadingSpinner = new AsyncProcessIcon("network-waiting-spinner");
        this.codeAIChatToolWindowPanel = new JPanel(new GridBagLayout());
        this.chatContentPanel = new ScrollablePanel();
        this.userChatPanel = new UserChatPanel(this::syncSendAndDisplay);

        loadingSpinner.setBorder(JBUI.Borders.emptyLeft(8));
        loadingSpinner.setVisible(false);

        var gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;

        chatContentPanel.setLayout(new BoxLayout(chatContentPanel, BoxLayout.Y_AXIS));
        var scrollPane = new JBScrollPane(chatContentPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);
        codeAIChatToolWindowPanel.add(scrollPane, gbc);

        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 1;

        codeAIChatToolWindowPanel.add(userChatPanel, gbc);
    }

    private void showChatContent(String content, int type) {
        var gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        var text = new JTextPane();
        text.setLayout(new BoxLayout(text, BoxLayout.PAGE_AXIS));
        text.setText(content);
        text.setEditable(false);

        // response should show loading
        if (type == 1) {
            var panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            panel.setBorder(JBUI.Borders.empty());
            loadingSpinner.resume();
            loadingSpinner.setVisible(true);
            panel.add(loadingSpinner);
            chatContentPanel.add(panel);
        }

        ChatDisplayPanel chatDisplayPanel = new ChatDisplayPanel().setText(text);
        chatContentPanel.add(chatDisplayPanel, gbc);
        chatContentPanel.revalidate();
        chatContentPanel.repaint();
    }

    private String sendMessage(Project project, String message) {
        var codeAIMessage = new CodeAIMessage();
        // FIXME
        codeAIMessage.setRole("user");
        codeAIMessage.setContent(message);

        var request = new CodeAIChatCompletionRequest();
        request.setMessages(List.of(codeAIMessage));
        return new LlmProviderFactory().getLlmProvider(project).chatCompletion(request);
    }

    private void syncSendAndDisplay(String message) {
        // show prompt
        showChatContent(message, 0);

        // FIXME
        new Thread(() -> {
            String result = sendMessage(this.project, message);
            showChatContent(result, 1);
            loadingSpinner.setVisible(false);
        }).start();
    }
}
