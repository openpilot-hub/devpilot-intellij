package com.zhongan.codeai.gui.toolwindows.chat;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.zhongan.codeai.enums.SessionTypeEnum;
import com.zhongan.codeai.gui.toolwindows.components.ChatDisplayPanel;
import com.zhongan.codeai.gui.toolwindows.components.ContentComponent;
import com.zhongan.codeai.gui.toolwindows.components.UserChatPanel;
import com.zhongan.codeai.integrations.llms.LlmProvider;
import com.zhongan.codeai.integrations.llms.LlmProviderFactory;
import com.zhongan.codeai.integrations.llms.entity.CodeAIChatCompletionRequest;
import com.zhongan.codeai.integrations.llms.entity.CodeAIMessage;
import com.zhongan.codeai.util.CodeAIMessageBundle;
import com.zhongan.codeai.util.MarkdownUtil;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

public class CodeAIChatToolWindow {
    private final JPanel codeAIChatToolWindowPanel;

    private final UserChatPanel userChatPanel;

    private final ScrollablePanel chatContentPanel;

    private final Project project;

    private LlmProvider llmProvider;

    private CodeAIChatCompletionRequest multiSessionRequest = new CodeAIChatCompletionRequest();

    public CodeAIChatToolWindow(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.codeAIChatToolWindowPanel = new JPanel(new GridBagLayout());
        this.chatContentPanel = new ScrollablePanel();
        this.userChatPanel = new UserChatPanel(this::syncSendAndDisplay, this::stopSending);
        this.llmProvider = new LlmProviderFactory().getLlmProvider(project);

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
        chatContentPanel.add(createWelcomePanel());
    }

    public JPanel getCodeAIChatToolWindowPanel() {
        return codeAIChatToolWindowPanel;
    }

    private void showChatContent(String content, int type) {
        var gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        ContentComponent contentPanel = new ContentComponent();

        List<String> blocks = MarkdownUtil.splitBlocks(content);
        for (String block : blocks) {
            if (block.startsWith("```")) {
                contentPanel.add(contentPanel.createCodeComponent(project, block));
            } else {
                contentPanel.add(contentPanel.createTextComponent(block));
            }
        }

        ChatDisplayPanel chatDisplayPanel = new ChatDisplayPanel().setText(contentPanel);

        // 0 - user, 1 - system
        if (type == 0) {
            chatDisplayPanel.setUserLabel();
        } else {
            chatDisplayPanel.setSystemLabel();
        }

        chatContentPanel.add(chatDisplayPanel, gbc);
        chatContentPanel.revalidate();
        chatContentPanel.repaint();

        // scroll to bottom
        chatContentPanel.scrollRectToVisible(chatContentPanel.getVisibleRect());
    }

    private String sendMessage(Integer sessionType, String message) {
        var codeAIMessage = new CodeAIMessage();
        // FIXME
        codeAIMessage.setRole("user");
        codeAIMessage.setContent(message);
        //check session type,default multi session
        CodeAIChatCompletionRequest codeAIChatCompletionRequest;
        SessionTypeEnum sessionTypeEnum = SessionTypeEnum.getEnumByCode(sessionType);
        if (SessionTypeEnum.INDEPENDENT.equals(sessionTypeEnum) && !multiSessionRequest.getMessages().isEmpty()) {
            codeAIChatCompletionRequest = new CodeAIChatCompletionRequest();
        } else {
            codeAIChatCompletionRequest = multiSessionRequest;
        }
        codeAIChatCompletionRequest.getMessages().add(codeAIMessage);

        return llmProvider.chatCompletion(codeAIChatCompletionRequest);
    }

    public void syncSendAndDisplay(String message) {
        //chat窗口支持多轮会话
        syncSendAndDisplay(SessionTypeEnum.MULTI_TURN.getCode(), message, null);
    }

    public void syncSendAndDisplay(Integer sessionType, String message, Consumer<String> callback) {

        // check if sending
        if (userChatPanel.isSending()) {
            return;
        }

        // set status sending
        userChatPanel.setSending(true);
        userChatPanel.setIconStop();

        // show prompt
        showChatContent(message, 0);

        // show thinking
        showChatContent(CodeAIMessageBundle.get("codeai.thinking.content"), 1);

        // FIXME
        new Thread(() -> {
            String result = sendMessage(sessionType, message);
            SwingUtilities.invokeLater(() -> {
                int componentCount = chatContentPanel.getComponentCount();
                if (componentCount > 0) {
                    Component loading = chatContentPanel.getComponent(componentCount - 1);
                    chatContentPanel.remove(loading);
                    showChatContent(result, 1);
                }

                userChatPanel.setIconSend();
                userChatPanel.setSending(false);

                if (callback != null) {
                    callback.accept(result);
                }
            });
        }).start();
    }

    private void stopSending() {
        llmProvider.interruptSend();
        userChatPanel.setIconSend();
        userChatPanel.setSending(false);
    }

    public void clearSession() {
        SwingUtilities.invokeLater(() -> {
            if (userChatPanel.isSending()) {
                stopSending();
            }
            chatContentPanel.setVisible(false);
            chatContentPanel.removeAll();
            chatContentPanel.setVisible(true);
        });
        multiSessionRequest.getMessages().clear();
    }

    private ChatDisplayPanel createWelcomePanel() {
        JTextPane jTextPane = new JTextPane();
        jTextPane.setText("Welcome to OpenPilot: Your AI-Infused Code!");
        ChatDisplayPanel chatDisplayPanel = new ChatDisplayPanel().setText(jTextPane);
        chatDisplayPanel.setSystemLabel();
        return chatDisplayPanel;
    }

}