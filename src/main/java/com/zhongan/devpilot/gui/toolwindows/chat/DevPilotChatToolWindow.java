package com.zhongan.devpilot.gui.toolwindows.chat;

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
import com.zhongan.devpilot.actions.editor.popupmenu.BasicEditorAction;
import com.zhongan.devpilot.constant.PromptConst;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.enums.SessionTypeEnum;
import com.zhongan.devpilot.gui.toolwindows.components.ChatDisplayPanel;
import com.zhongan.devpilot.gui.toolwindows.components.ContentComponent;
import com.zhongan.devpilot.gui.toolwindows.components.EditorInfo;
import com.zhongan.devpilot.gui.toolwindows.components.UserChatPanel;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.LlmProviderFactory;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.util.BalloonAlertUtils;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.MarkdownUtil;
import com.zhongan.devpilot.util.MessageUtil;

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

public class DevPilotChatToolWindow {
    private final JPanel devPilotChatToolWindowPanel;

    private final UserChatPanel userChatPanel;

    private final ScrollablePanel chatContentPanel;

    private final Project project;

    private LlmProvider llmProvider;

    private final DevPilotChatCompletionRequest multiSessionRequest = new DevPilotChatCompletionRequest();

    public DevPilotChatToolWindow(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.devPilotChatToolWindowPanel = new JPanel(new GridBagLayout());
        this.chatContentPanel = new ScrollablePanel();
        this.userChatPanel = new UserChatPanel(this::syncSendAndDisplay, this::stopSending);

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
        devPilotChatToolWindowPanel.add(scrollPane, gbc);

        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 1;

        devPilotChatToolWindowPanel.add(userChatPanel, gbc);
        chatContentPanel.add(createWelcomePanel());
    }

    public JPanel getDevPilotChatToolWindowPanel() {
        return devPilotChatToolWindowPanel;
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
            contentPanel.add(contentPanel.createTextComponent(DevPilotMessageBundle.get("devpilot.chatWindow.diff")));
        } else if (type == 0 && actionType != null) {
            contentPanel.add(contentPanel.createRightActionComponent(DevPilotMessageBundle.get(actionType.getLabel()), project, editorInfo));
        } else {
            List<String> blocks = MarkdownUtil.divideMarkdown(content);
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
        DevPilotMessage userMessage = MessageUtil.createUserMessage(message);
        // check session type,default multi session
        DevPilotChatCompletionRequest devPilotChatCompletionRequest = new DevPilotChatCompletionRequest();
        SessionTypeEnum sessionTypeEnum = SessionTypeEnum.getEnumByCode(sessionType);
        if (SessionTypeEnum.INDEPENDENT.equals(sessionTypeEnum)) {
            // independent message can not update, just readonly
            devPilotChatCompletionRequest.getMessages().add(userMessage);
            devPilotChatCompletionRequest.getMessages().add(MessageUtil.createSystemMessage(PromptConst.RESPONSE_FORMAT));
        } else {
            if (multiSessionRequest.getMessages().isEmpty()) {
                multiSessionRequest.getMessages().add(MessageUtil.createSystemMessage(PromptConst.RESPONSE_FORMAT));
            }
            devPilotChatCompletionRequest.setStream(multiSessionRequest.isStream());
            devPilotChatCompletionRequest.setModel(multiSessionRequest.getModel());
            multiSessionRequest.getMessages().add(userMessage);
            devPilotChatCompletionRequest.getMessages().addAll(multiSessionRequest.getMessages());
        }

        var llmProvider = new LlmProviderFactory().getLlmProvider(project);
        this.llmProvider = llmProvider;

        String chatCompletion = llmProvider.chatCompletion(devPilotChatCompletionRequest);
        if (SessionTypeEnum.MULTI_TURN.equals(sessionTypeEnum) &&
                devPilotChatCompletionRequest.getMessages().size() > multiSessionRequest.getMessages().size()) {
            // update multi session request
            multiSessionRequest.getMessages().add(
                    devPilotChatCompletionRequest.getMessages().get(devPilotChatCompletionRequest.getMessages().size() - 1));
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
        showChatContent(DevPilotMessageBundle.get("devpilot.thinking.content"), 1);

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
        if (llmProvider == null) {
            return;
        }
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
        clearSessionTip.setText(DevPilotMessageBundle.get("devpilot.clear.session.tip"));
        chatContentPanel.add(new ChatDisplayPanel().setText(clearSessionTip).setSystemLabel());
    }

    private ChatDisplayPanel createWelcomePanel() {
        JTextPane welcomePanel = new JTextPane();
        welcomePanel.setContentType("text/html");
        welcomePanel.setEditable(false);
        welcomePanel.setOpaque(true);
        welcomePanel.setBackground(new JBColor(Gray._248, Gray._54));
        welcomePanel.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true);
        welcomePanel.setText(String.format(DevPilotMessageBundle.get("devpilot.welcome.words"), DevPilotLlmSettingsState.getInstance().getFullName()));
        welcomePanel.setBorder(JBUI.Borders.emptyLeft(5));

        welcomePanel.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String description = e.getDescription();
                ActionManager actionManager = ActionManager.getInstance();
                BasicEditorAction myAction = (BasicEditorAction) actionManager.getAction(DevPilotMessageBundle.get(description));
                Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                if (editor == null || !editor.getSelectionModel().hasSelection()) {
                    String msg = editor == null ? DevPilotMessageBundle.get("devpilot.alter.welcome.openFile") : DevPilotMessageBundle.get("devpilot.alter.welcome.selectCode");
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
        userPromptPanel.setText(String.format(DevPilotMessageBundle.get("devpilot.welcome.assist"), DevPilotLlmSettingsState.getInstance().getFullName()));
        ChatDisplayPanel chatDisplayPanel = new ChatDisplayPanel().setText(userPromptPanel);
        chatDisplayPanel.setSystemLabel();
        return chatDisplayPanel;
    }

}