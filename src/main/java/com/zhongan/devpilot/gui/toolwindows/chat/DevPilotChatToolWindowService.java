package com.zhongan.devpilot.gui.toolwindows.chat;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.psi.PsiElement;
import com.zhongan.devpilot.actions.editor.popupmenu.BasicEditorAction;
import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.enums.SessionTypeEnum;
import com.zhongan.devpilot.gui.toolwindows.components.EditorInfo;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.LlmProviderFactory;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.util.BalloonAlertUtils;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.JsonUtils;
import com.zhongan.devpilot.util.MessageUtil;
import com.zhongan.devpilot.util.TokenUtils;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;
import com.zhongan.devpilot.webview.model.EmbeddedModel;
import com.zhongan.devpilot.webview.model.JavaCallModel;
import com.zhongan.devpilot.webview.model.LocaleModel;
import com.zhongan.devpilot.webview.model.LoginModel;
import com.zhongan.devpilot.webview.model.MessageModel;
import com.zhongan.devpilot.webview.model.ThemeModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import static com.zhongan.devpilot.enums.SessionTypeEnum.MULTI_TURN;

@Service
public final class DevPilotChatToolWindowService {
    private final Project project;

    private final DevPilotChatToolWindow devPilotChatToolWindow;

    private LlmProvider llmProvider;

    private final List<MessageModel> historyMessageList = new ArrayList<>();

    private final List<DevPilotMessage> historyRequestMessageList = new ArrayList<>();

    public DevPilotChatToolWindowService(Project project) {
        this.project = project;
        this.devPilotChatToolWindow = new DevPilotChatToolWindow(project);
    }

    public DevPilotChatToolWindow getDevPilotChatToolWindow() {
        return this.devPilotChatToolWindow;
    }

    public Project getProject() {
        return this.project;
    }

    public String sendMessage(Integer sessionType, String msgType, Map<String, String> data, String message, Consumer<String> callback, MessageModel messageModel) {
        DevPilotMessage userMessage;
        if (data == null || data.isEmpty()) {
            userMessage = MessageUtil.createUserMessage(message, msgType, messageModel.getId());
        } else {
            userMessage = MessageUtil.createPromptMessage(messageModel.getId(), msgType, data);
        }

        // check session type,default multi session
        var devPilotChatCompletionRequest = new DevPilotChatCompletionRequest();
        var sessionTypeEnum = SessionTypeEnum.getEnumByCode(sessionType);
        if (SessionTypeEnum.INDEPENDENT.equals(sessionTypeEnum)) {
            // independent message can not update, just readonly
            devPilotChatCompletionRequest.setStream(false);
            devPilotChatCompletionRequest.getMessages().add(userMessage);
        } else {
            if (message != null && message.startsWith("@repo")) {
                clearRequestSession();
            }
            devPilotChatCompletionRequest.setStream(true);
            historyRequestMessageList.add(userMessage);
            devPilotChatCompletionRequest.getMessages().addAll(copyHistoryRequestMessageList(historyRequestMessageList));
        }

        callWebView(messageModel);
        addMessage(messageModel);
        callWebView(MessageModel.buildLoadingMessage());

        this.llmProvider = new LlmProviderFactory().getLlmProvider(project);
        var chatCompletion = this.llmProvider.chatCompletion(project, devPilotChatCompletionRequest, callback);
        if (MULTI_TURN.equals(sessionTypeEnum) &&
                devPilotChatCompletionRequest.getMessages().size() > historyRequestMessageList.size()) {
            // update multi session request
            historyRequestMessageList.add(
                    devPilotChatCompletionRequest.getMessages().get(devPilotChatCompletionRequest.getMessages().size() - 1));
        }

        return chatCompletion;
    }

    public String sendMessage(Consumer<String> callback) {
        // check session type,default multi session
        var devPilotChatCompletionRequest = new DevPilotChatCompletionRequest();
        devPilotChatCompletionRequest.setStream(true);
        devPilotChatCompletionRequest.getMessages().addAll(copyHistoryRequestMessageList(historyRequestMessageList));

        callWebView(MessageModel.buildLoadingMessage());

        this.llmProvider = new LlmProviderFactory().getLlmProvider(project);

        var chatCompletion = this.llmProvider.chatCompletion(project, devPilotChatCompletionRequest, callback);
        if (devPilotChatCompletionRequest.getMessages().size() > historyRequestMessageList.size()) {
            // update multi session request
            historyRequestMessageList.add(
                    devPilotChatCompletionRequest.getMessages().get(devPilotChatCompletionRequest.getMessages().size() - 1));
        }

        return chatCompletion;
    }

    public void interruptSend() {
        if (this.llmProvider != null) {
            this.llmProvider.interruptSend();
        }
    }

    public List<MessageModel> getHistoryMessageList() {
        return historyMessageList;
    }

    public void addMessage(MessageModel messageModel) {
        historyMessageList.add(messageModel);
    }

    public void addRequestMessage(DevPilotMessage message) {
        historyRequestMessageList.add(message);
    }

    public void clearSession() {
        historyMessageList.clear();
        historyRequestMessageList.clear();
        callWebView();
    }

    // Do not clear message show session
    public void clearRequestSession() {
        historyRequestMessageList.clear();

        if (historyMessageList.isEmpty()) {
            return;
        }

        var dividerModel = MessageModel.buildDividerMessage();
        callWebView(dividerModel);
        historyMessageList.add(dividerModel);
    }

    public void deleteMessage(String id) {
        // get user message id then delete itself and its next item(assistant message)
        String assistantMessageId = null;

        var index = getMessageIndex(id);

        if (index == -1) {
            return;
        }

        var nextIndex = index + 1;
        if (nextIndex < historyMessageList.size()) {
            var nextMessage = historyMessageList.get(nextIndex);
            if (nextMessage.getRole().equals("assistant")) {
                assistantMessageId = nextMessage.getId();
                historyMessageList.remove(nextIndex);
            }
        }

        historyMessageList.remove(index);

        historyRequestMessageList.removeIf(item -> item.getId().equals(id));
        if (assistantMessageId != null) {
            var finalAssistantMessageId = assistantMessageId;
            historyRequestMessageList.removeIf(item -> item.getId().equals(finalAssistantMessageId));
        }

        callWebView();
    }

    public void regenerateMessage() {
        var lastMessage = historyMessageList.get(historyMessageList.size() - 1);

        if (!lastMessage.getRole().equals("assistant")) {
            return;
        }

        var id = lastMessage.getId();
        historyMessageList.removeIf(item -> item.getId().equals(id));
        historyRequestMessageList.removeIf(item -> item.getId().equals(id));
        // todo handle real callback
        sendMessage(null);
    }

    public void handleActions(EditorActionEnum actionEnum, PsiElement psiElement) {
        ActionManager actionManager = ActionManager.getInstance();
        BasicEditorAction myAction = (BasicEditorAction) actionManager
                .getAction(DevPilotMessageBundle.get(actionEnum.getLabel()));
        ApplicationManager.getApplication().invokeLater(() -> {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor == null || !editor.getSelectionModel().hasSelection()) {
                BalloonAlertUtils.showWarningAlert(DevPilotMessageBundle.get("devpilot.alter.code.not.selected"), 0, -10, Balloon.Position.above);
                return;
            }
            myAction.fastAction(project, editor, editor.getSelectionModel().getSelectedText(), psiElement, null);
        });
    }

    public void handleActions(CodeReferenceModel codeReferenceModel, EditorActionEnum actionEnum, PsiElement psiElement) {
        if (codeReferenceModel == null || StringUtils.isEmpty(codeReferenceModel.getSourceCode())) {
            handleActions(actionEnum, psiElement);
            return;
        }

        ActionManager actionManager = ActionManager.getInstance();
        BasicEditorAction myAction = (BasicEditorAction) actionManager
                .getAction(DevPilotMessageBundle.get(actionEnum.getLabel()));
        ApplicationManager.getApplication().invokeLater(() -> {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor == null) {
                BalloonAlertUtils.showWarningAlert(DevPilotMessageBundle.get("devpilot.alter.code.not.selected"), 0, -10, Balloon.Position.above);
                return;
            }
            myAction.fastAction(project, editor, codeReferenceModel.getSourceCode(), psiElement, codeReferenceModel);
        });
    }

    public MessageModel getUserContentCode(MessageModel messageModel) {
        var message = messageModel.getContent();

        if (messageModel.getCodeRef() != null) {
            var codeRef = messageModel.getCodeRef();
            var sourceCode = codeRef.getSourceCode();
            var language = codeRef.getLanguageId();
            if (language == null) {
                language = "";
            }
            var codeFormat = String.format("```%s\n%s\n```\n", language, sourceCode);
            codeRef.setVisible(false);
            messageModel.setContent(message + "\n" + codeFormat);
            return messageModel;
        }

        final Editor[] editor = new Editor[1];
        final EditorInfo[] editorInfo = new EditorInfo[1];

        ApplicationManager.getApplication().invokeAndWait(() -> {
            editor[0] = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor[0] == null || !editor[0].getSelectionModel().hasSelection()) {
                editorInfo[0] = null;
                return;
            }
            editorInfo[0] = new EditorInfo(editor[0]);
        });

        if (editorInfo[0] == null || editorInfo[0].getSourceCode() == null) {
            return messageModel;
        }

        var codeReference = CodeReferenceModel.getCodeRefFromEditor(editorInfo[0], null);
        codeReference.setVisible(false);
        var codeFormat = String.format("```%s\n%s\n```\n", codeReference.getLanguageId(), codeReference.getSourceCode());

        messageModel.setContent(message + "\n" + codeFormat);
        messageModel.setCodeRef(codeReference);

        return messageModel;
    }

    // get user message by assistant message id
    public MessageModel getUserMessage(String id) {
        var index = getMessageIndex(id);

        if (index == -1) {
            return null;
        }

        var userMessage = historyMessageList.get(index - 1);

        if (!userMessage.getRole().equals("user")) {
            return null;
        }

        return userMessage;
    }

    private void buildConversationWindowMemory() {
        // 先确保内容不超过 输入token限制
        List<Integer> tokenCounts = TokenUtils.ComputeTokensFromMessagesUsingGPT35Enc(historyRequestMessageList);
        int totalTokenCount = tokenCounts.get(0);
        Collections.reverse(tokenCounts);
        int keepCount = 1;
        for (int i = 0; i < tokenCounts.size() - 1; i++) {
            Integer tokenCount = tokenCounts.get(i);
            if (totalTokenCount + tokenCount > DefaultConst.GPT_35_TOKEN_MAX_LENGTH) {
                break;
            }
            totalTokenCount += tokenCount;
            keepCount++;
        }
        int removeCount = historyRequestMessageList.size() - keepCount;
        for (int i = 0; i < removeCount; i++) {
            historyRequestMessageList.remove(1);
        }

        // 再检查window size
        removeCount = historyRequestMessageList.size() - DefaultConst.CONVERSATION_WINDOW_LENGTH;
        for (int i = 0; i < removeCount; i++) {
            historyRequestMessageList.remove(1);
        }
    }

    private int getMessageIndex(String id) {
        var index = -1;
        for (int i = 0; i < historyMessageList.size(); i++) {
            var message = historyMessageList.get(i);
            if (message.getId().equals(id)) {
                index = i;
                break;
            }
        }
        return index;
    }

    private List<DevPilotMessage> copyHistoryRequestMessageList(List<DevPilotMessage> historyRequestMessageList) {
        List<DevPilotMessage> copiedList = new ArrayList<>();
        for (DevPilotMessage message : historyRequestMessageList) {
            DevPilotMessage copiedMessage = new DevPilotMessage();
            copiedMessage.setRole(message.getRole());
            copiedMessage.setPromptData(message.getPromptData());
            copiedMessage.setContent(message.getContent());
            copiedMessage.setCommandType(message.getCommandType());
            copiedMessage.setId(message.getId());
            copiedList.add(copiedMessage);
        }
        return copiedList;
    }

    public void callWebView(JavaCallModel javaCallModel) {
        var browser = getDevPilotChatToolWindow().jbCefBrowser().getCefBrowser();
        var json = JsonUtils.toJson(javaCallModel);

        if (json == null) {
            return;
        }

        var jsCode = "window.receiveFromIntelliJ(" + json + ")";
        browser.executeJavaScript(jsCode, browser.getURL(), 0);
    }

    public void callErrorInfo(String content) {
        var messageModel = MessageModel.buildInfoMessage(content);
        callWebView(messageModel);
        addMessage(messageModel);
    }

    public void callWebView(MessageModel messageModel) {
        var messageList = getHistoryMessageList();

        var tmpList = new ArrayList<>(messageList);
        tmpList.add(messageModel);

        var javaCallModel = new JavaCallModel();
        javaCallModel.setCommand("RenderChatConversation");
        javaCallModel.setPayload(tmpList);

        callWebView(javaCallModel);
    }

    public void callWebView() {
        var messageList = getHistoryMessageList();

        var javaCallModel = new JavaCallModel();
        javaCallModel.setCommand("RenderChatConversation");
        javaCallModel.setPayload(messageList);

        callWebView(javaCallModel);
    }

    public void changeTheme(String theme) {
        var javaCallModel = new JavaCallModel();
        javaCallModel.setCommand("ThemeChanged");
        javaCallModel.setPayload(new ThemeModel(theme));

        callWebView(javaCallModel);
    }

    public void changeLocale(String locale) {
        var javaCallModel = new JavaCallModel();
        javaCallModel.setCommand("LocaleChanged");
        javaCallModel.setPayload(new LocaleModel(locale));

        callWebView(javaCallModel);
    }

    public void changeLoginStatus(boolean isLoggedIn) {
        var javaCallModel = new JavaCallModel();
        javaCallModel.setCommand("ConfigurationChanged");
        javaCallModel.setPayload(new LoginModel(isLoggedIn));

        callWebView(javaCallModel);
    }

    public void presentRepoCodeEmbeddedState(boolean isEmbedded, String repoName) {
        JavaCallModel javaCallModel = new JavaCallModel();
        javaCallModel.setCommand("PresentCodeEmbeddedState");
        javaCallModel.setPayload(new EmbeddedModel(isEmbedded, repoName));

        callWebView(javaCallModel);
    }

    public void referenceCode(CodeReferenceModel referenceModel) {
        var javaCallModel = new JavaCallModel();
        javaCallModel.setCommand("ReferenceCode");
        javaCallModel.setPayload(referenceModel);

        callWebView(javaCallModel);
    }
}
