package com.zhongan.codeai.gui.toolwindows.chat;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.zhongan.codeai.actions.editor.popupmenu.BasicEditorAction;
import com.zhongan.codeai.enums.EditorActionEnum;
import com.zhongan.codeai.enums.SessionTypeEnum;
import com.zhongan.codeai.gui.toolwindows.components.ChatDisplayPanel;
import com.zhongan.codeai.gui.toolwindows.components.ContentComponent;
import com.zhongan.codeai.gui.toolwindows.components.EditorInfo;
import com.zhongan.codeai.gui.toolwindows.components.UserChatPanel;
import com.zhongan.codeai.integrations.llms.LlmProvider;
import com.zhongan.codeai.integrations.llms.LlmProviderFactory;
import com.zhongan.codeai.integrations.llms.entity.CodeAIChatCompletionRequest;
import com.zhongan.codeai.integrations.llms.entity.CodeAIMessage;
import com.zhongan.codeai.settings.state.CodeAILlmSettingsState;
import com.zhongan.codeai.util.BalloonAlertUtils;
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
import javax.swing.event.HyperlinkEvent;

public class CodeAIChatToolWindow {
    private final JPanel codeAIChatToolWindowPanel;

    private final UserChatPanel userChatPanel;

    private final ScrollablePanel chatContentPanel;

    private final Project project;

    private final LlmProvider llmProvider;

    private final CodeAIChatCompletionRequest multiSessionRequest = new CodeAIChatCompletionRequest();

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
        showChatContent(content, type, null, null);
    }

    private void showChatContent(String content, int type, EditorActionEnum actionType, EditorInfo editorInfo) {
        var gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        ContentComponent contentPanel = new ContentComponent();

        if (type == 1 && actionType == EditorActionEnum.GENERATE_COMMENTS) {
            contentPanel.add(contentPanel.createTextComponent(CodeAIMessageBundle.get("codeai.chatWindow.diff")));
        } else if (type == 0 && actionType != null) {
            contentPanel.add(contentPanel.createRightActionComponent(CodeAIMessageBundle.get(actionType.getLabel()), project, editorInfo));
        } else {
            List<String> blocks = MarkdownUtil.splitBlocks(content);
            for (String block : blocks) {
                if (block.startsWith("```")) {
                    contentPanel.add(contentPanel.createCodeComponent(project, block, actionType,
                                                            editorInfo == null ? null : editorInfo.getChosenEditor()));
                } else {
                    contentPanel.add(contentPanel.createTextComponent(block));
                }
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
        codeAIMessage.setRole("user");
        codeAIMessage.setContent(message);
        // check session type,default multi session
        CodeAIChatCompletionRequest codeAIChatCompletionRequest = new CodeAIChatCompletionRequest();
        SessionTypeEnum sessionTypeEnum = SessionTypeEnum.getEnumByCode(sessionType);
        if (SessionTypeEnum.INDEPENDENT.equals(sessionTypeEnum)) {
            // independent message can not update, just readonly
            codeAIChatCompletionRequest.getMessages().add(codeAIMessage);
        } else {
            codeAIChatCompletionRequest.setStream(multiSessionRequest.isStream());
            codeAIChatCompletionRequest.setModel(multiSessionRequest.getModel());
            multiSessionRequest.getMessages().add(codeAIMessage);
            codeAIChatCompletionRequest.getMessages().addAll(multiSessionRequest.getMessages());
        }
        String chatCompletion = llmProvider.chatCompletion(codeAIChatCompletionRequest);
        if (SessionTypeEnum.MULTI_TURN.equals(sessionTypeEnum) &&
                codeAIChatCompletionRequest.getMessages().size() > multiSessionRequest.getMessages().size()) {
            // update multi session request
            multiSessionRequest.getMessages().add(
                    codeAIChatCompletionRequest.getMessages().get(codeAIChatCompletionRequest.getMessages().size() - 1));
        }
        return chatCompletion;
    }

    public void syncSendAndDisplay(String message) {
        // support multi session
        syncSendAndDisplay(SessionTypeEnum.MULTI_TURN.getCode(), null, message, null, null);
    }

    public void syncSendAndDisplay(Integer sessionType, EditorActionEnum editorActionEnum, String message, Consumer<String> callback, EditorInfo editorInfo) {

        // check if sending
        if (userChatPanel.isSending()) {
            return;
        }

        // set status sending
        userChatPanel.setSending(true);
        userChatPanel.setIconStop();

        // show prompt
        showChatContent(message, 0, editorActionEnum, editorInfo);

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
                    showChatContent(result, 1, editorActionEnum, editorInfo);
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
            chatContentPanel.add(createUserPromptPanel());
        });
        multiSessionRequest.getMessages().clear();
    }

    public void addClearSessionInfo() {
        if (multiSessionRequest.getMessages().isEmpty()) {
            return;
        }
        multiSessionRequest.getMessages().clear();
        JTextPane clearSessionTip = new JTextPane();
        clearSessionTip.setText(CodeAIMessageBundle.get("codeai.clear.session.tip"));
        chatContentPanel.add(new ChatDisplayPanel().setText(clearSessionTip).setSystemLabel());
    }

    private ChatDisplayPanel createWelcomePanel() {
        JTextPane welcomePanel = new JTextPane();
        welcomePanel.setContentType("text/html");
        welcomePanel.setEditable(false);
        welcomePanel.setOpaque(true);
        welcomePanel.setBackground(new JBColor(Gray._248, Gray._54));
        welcomePanel.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true);
        welcomePanel.setText(String.format(CodeAIMessageBundle.get("codeai.welcome.words"), CodeAILlmSettingsState.getInstance().getFullName()));
        welcomePanel.setBorder(JBUI.Borders.emptyLeft(5));

        welcomePanel.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String description = e.getDescription();
                ActionManager actionManager = ActionManager.getInstance();
                BasicEditorAction myAction = (BasicEditorAction) actionManager.getAction(CodeAIMessageBundle.get(description));
                Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                if (editor == null || !editor.getSelectionModel().hasSelection()) {
                    String msg = editor == null ? CodeAIMessageBundle.get("codeai.alter.welcome.openFile") : CodeAIMessageBundle.get("codeai.alter.welcome.selectCode");
                    BalloonAlertUtils.showWarningAlert(msg, 14, 12, Balloon.Position.atRight);
                    return;
                }
                myAction.fastAction(project, editor, editor.getSelectionModel().getSelectedText());
            }
        });
        ChatDisplayPanel chatDisplayPanel = new ChatDisplayPanel().setText(welcomePanel);
        chatDisplayPanel.setSystemLabel();
        return chatDisplayPanel;
    }

    private ChatDisplayPanel createUserPromptPanel() {
        JTextPane userPromptPanel = new JTextPane();
        userPromptPanel.setContentType("text/html");
        userPromptPanel.setEditable(false);
        userPromptPanel.setOpaque(true);
        userPromptPanel.setBackground(new JBColor(Gray._248, Gray._54));
        userPromptPanel.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true);
        userPromptPanel.setBorder(JBUI.Borders.emptyLeft(5));
        userPromptPanel.setText(String.format(CodeAIMessageBundle.get("codeai.welcome.assist"), CodeAILlmSettingsState.getInstance().getFullName()));
        ChatDisplayPanel chatDisplayPanel = new ChatDisplayPanel().setText(userPromptPanel);
        chatDisplayPanel.setSystemLabel();
        return chatDisplayPanel;
    }

}