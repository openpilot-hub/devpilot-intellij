package com.zhongan.devpilot.gui.toolwindows.chat;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.zhongan.devpilot.DevPilotVersion;
import com.zhongan.devpilot.actions.editor.popupmenu.BasicEditorAction;
import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.enums.SessionTypeEnum;
import com.zhongan.devpilot.gui.toolwindows.components.EditorInfo;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.LlmProviderFactory;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotCodePrediction;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotRagRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotRagResponse;
import com.zhongan.devpilot.provider.file.FileAnalyzeProviderFactory;
import com.zhongan.devpilot.util.BalloonAlertUtils;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.EncryptionUtil;
import com.zhongan.devpilot.util.JsonUtils;
import com.zhongan.devpilot.util.MessageUtil;
import com.zhongan.devpilot.util.TokenUtils;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;
import com.zhongan.devpilot.webview.model.EmbeddedModel;
import com.zhongan.devpilot.webview.model.JavaCallModel;
import com.zhongan.devpilot.webview.model.LocaleModel;
import com.zhongan.devpilot.webview.model.LoginModel;
import com.zhongan.devpilot.webview.model.MessageModel;
import com.zhongan.devpilot.webview.model.RecallModel;
import com.zhongan.devpilot.webview.model.ThemeModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import static com.zhongan.devpilot.constant.DefaultConst.CHAT_STEP_ONE;
import static com.zhongan.devpilot.constant.DefaultConst.CHAT_STEP_THREE;
import static com.zhongan.devpilot.constant.DefaultConst.CHAT_STEP_TWO;
import static com.zhongan.devpilot.constant.DefaultConst.CODE_PREDICT_PROMPT_VERSION;
import static com.zhongan.devpilot.constant.DefaultConst.NORMAL_CHAT_TYPE;
import static com.zhongan.devpilot.constant.DefaultConst.SMART_CHAT_TYPE;
import static com.zhongan.devpilot.enums.SessionTypeEnum.MULTI_TURN;

@Service
public final class DevPilotChatToolWindowService {
    private final Project project;

    private final DevPilotChatToolWindow devPilotChatToolWindow;

    private LlmProvider llmProvider;

    private final List<MessageModel> historyMessageList = new ArrayList<>();

    private final List<DevPilotMessage> historyRequestMessageList = new ArrayList<>();

    private final AtomicBoolean cancel = new AtomicBoolean(false);

    private MessageModel lastMessage = new MessageModel();

    private final AtomicInteger nowStep = new AtomicInteger(1);

    private volatile String currentMessageId = null;

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

    public void chat(Integer sessionType, String msgType, Map<String, String> data,
                     String message, Consumer<String> callback, MessageModel messageModel) {
        this.cancel.set(false);
        this.currentMessageId = messageModel.getId();
        this.lastMessage = messageModel;

        callWebView(messageModel);
        addMessage(messageModel);
        callWebView(MessageModel.buildLoadingMessage());

        this.llmProvider = new LlmProviderFactory().getLlmProvider(project);

        // todo normal chat should be used in some case
//        if (messageModel.getCodeRef() == null) {
//            normalChat(sessionType, msgType, data, message, callback, messageModel);
//            return;
//        }

        smartChat(sessionType, msgType, data, message, callback, messageModel);
    }

    public void normalChat(Integer sessionType, String msgType, Map<String, String> data,
                           String message, Consumer<String> callback, MessageModel messageModel) {
        sendMessage(sessionType, msgType, data, message, callback, messageModel, null, null, NORMAL_CHAT_TYPE);
    }

    public void smartChat(Integer sessionType, String msgType, Map<String, String> data,
                          String message, Consumer<String> callback, MessageModel messageModel) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // step1 call model to do code prediction
            if (shouldCancelChat(messageModel)) {
                return;
            }

            this.nowStep.set(CHAT_STEP_ONE);
            var references = codePredict(messageModel.getContent(), messageModel.getCodeRef(), msgType);

            // step2 call rag to analyze code
            if (shouldCancelChat(messageModel)) {
                return;
            }

            this.nowStep.set(CHAT_STEP_TWO);
            var rag = callRag(references, messageModel.getCodeRef(), message);

            // step3 call model to get the final result
            if (shouldCancelChat(messageModel)) {
                return;
            }

            this.nowStep.set(CHAT_STEP_THREE);

            // avoid immutable map
            Map<String, String> newMap;
            if (data != null) {
                newMap = new HashMap<>(data);
            } else {
                newMap = new HashMap<>();
            }
            final List<CodeReferenceModel>[] localRefs = new List[1];
            final List<CodeReferenceModel>[] remoteRefs = new List[1];

            if (rag != null) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    var language = messageModel.getCodeRef() == null ? null : messageModel.getCodeRef().getLanguageId();

                    FileAnalyzeProviderFactory.getProvider(language)
                            .buildRelatedContextDataMap(project, messageModel.getCodeRef(), rag.localRag, rag.remoteRag, data);

                    if (rag.localRag != null) {
                        localRefs[0] = CodeReferenceModel.getCodeRefListFromPsiElement(rag.localRag, EditorActionEnum.getEnumByName(msgType));
                    }

                    if (rag.remoteRag != null) {
                        remoteRefs[0] = CodeReferenceModel.getCodeRefFromString(rag.remoteRag, language);
                    }
                });
            }

            sendMessage(sessionType, msgType, newMap, message, callback, messageModel, remoteRefs[0], localRefs[0], SMART_CHAT_TYPE);
        });
    }

    public void regenerateChat(MessageModel messageModel, Consumer<String> callback) {
        this.cancel.set(false);

        callWebView(MessageModel.buildLoadingMessage());

        this.llmProvider = new LlmProviderFactory().getLlmProvider(project);

        // todo normal chat should be used in some case
//        if (messageModel.getCodeRef() == null) {
//            regenerateNormalChat(messageModel, callback);
//            return;
//        }

        regenerateSmartChat(messageModel, callback);
    }

    public void regenerateNormalChat(MessageModel messageModel, Consumer<String> callback) {
        sendMessage(callback, null, null, null, NORMAL_CHAT_TYPE);
    }

    public void regenerateSmartChat(MessageModel messageModel, Consumer<String> callback) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // step1 call model to do code prediction
            if (shouldCancelChat(messageModel)) {
                return;
            }

            this.nowStep.set(CHAT_STEP_ONE);
            var references = codePredict(messageModel.getContent(), messageModel.getCodeRef(), null);

            // step2 call rag to analyze code
            if (shouldCancelChat(messageModel)) {
                return;
            }

            this.nowStep.set(CHAT_STEP_TWO);
            var rag = callRag(references, messageModel.getCodeRef(), messageModel.getContent());

            // step3 call model to get the final result
            if (shouldCancelChat(messageModel)) {
                return;
            }

            this.nowStep.set(CHAT_STEP_THREE);

            var data = new HashMap<String, String>();
            final List<CodeReferenceModel>[] localRefs = new List[1];
            final List<CodeReferenceModel>[] remoteRefs = new List[1];

            if (rag != null) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    var language = messageModel.getCodeRef() == null ? null : messageModel.getCodeRef().getLanguageId();

                    FileAnalyzeProviderFactory.getProvider(language)
                            .buildRelatedContextDataMap(project, messageModel.getCodeRef(), rag.localRag, rag.remoteRag, data);

                    if (rag.localRag != null) {
                        localRefs[0] = CodeReferenceModel.getCodeRefListFromPsiElement(rag.localRag, messageModel.getCodeRef().getType());
                    }

                    if (rag.remoteRag != null) {
                        remoteRefs[0] = CodeReferenceModel.getCodeRefFromString(rag.remoteRag, language);
                    }
                });
            }

            sendMessage(callback, data, remoteRefs[0], localRefs[0], SMART_CHAT_TYPE);
        });
    }

    private boolean shouldCancelChat(MessageModel messageModel) {
        if (cancel.get()) {
            return true;
        }

        return !StringUtils.equals(currentMessageId, messageModel.getId());
    }

    private DevPilotCodePrediction codePredict(String content, CodeReferenceModel codeReference, String commandType) {
        this.lastMessage = MessageModel
                .buildAssistantMessage(System.currentTimeMillis() + "", System.currentTimeMillis(), "", true, RecallModel.create(1));
        callWebView(this.lastMessage);

        final Map<String, String> dataMap = new HashMap<>();

        if (commandType == null) {
            if (codeReference == null || codeReference.getType() == null) {
                commandType = "PURE_CHAT";
            } else {
                commandType = codeReference.getType().name();
            }
        }

        dataMap.put("commandTypeFor", commandType);

        if (codeReference != null) {
            ApplicationManager.getApplication().runReadAction(() -> {
                FileAnalyzeProviderFactory.getProvider(codeReference.getLanguageId())
                        .buildCodePredictDataMap(project, codeReference, dataMap);
            });
        }

        var devPilotChatCompletionRequest = new DevPilotChatCompletionRequest();
        devPilotChatCompletionRequest.setVersion(CODE_PREDICT_PROMPT_VERSION);
        devPilotChatCompletionRequest.getMessages().addAll(removeRedundantRelatedContext(copyHistoryRequestMessageList(historyRequestMessageList)));
        devPilotChatCompletionRequest.getMessages().add(
                MessageUtil.createPromptMessage(System.currentTimeMillis() + "", "CODE_PREDICTION", content, dataMap));
        devPilotChatCompletionRequest.setStream(Boolean.FALSE);
        var response = this.llmProvider.codePrediction(devPilotChatCompletionRequest);
        if (!response.isSuccessful()) {
            return null;
        }
        return JsonUtils.fromJson(response.getContent(), DevPilotCodePrediction.class);
    }

    private Rag callRag(DevPilotCodePrediction codePredict, CodeReferenceModel codeReference, String message) {
        this.lastMessage = MessageModel
                .buildAssistantMessage(System.currentTimeMillis() + "", System.currentTimeMillis(), "", true, RecallModel.create(2));
        callWebView(this.lastMessage);

        return ApplicationManager.getApplication().runReadAction((Computable<Rag>) () -> {
            List<PsiElement> localRag = null;
            List<String> remoteRag = null;

            var language = codeReference == null ? DevPilotVersion.getDefaultLanguage() : codeReference.getLanguageId();

            // call local rag
            if (codePredict != null) {
                localRag = FileAnalyzeProviderFactory.getProvider(language).callLocalRag(project, codePredict);
            }

            // call remote rag
            var request = new DevPilotRagRequest();
            if (codeReference != null) {
                request.setSelectedCode(codeReference.getSourceCode());
            }
            request.setProjectType(language);
            if (message != null) {
                request.setContent(message);
            }

            // calculate md5 of project path as unique id
            request.setProjectName(getProjectPathString());

            var response = this.llmProvider.ragCompletion(request);

            if (response != null) {
                remoteRag = response.stream().map(DevPilotRagResponse::getCode)
                        .filter(Objects::nonNull).collect(Collectors.toList());
            }

            return new Rag(localRag, remoteRag);
        });
    }

    public String sendMessage(Integer sessionType, String msgType, Map<String, String> data,
                              String message, Consumer<String> callback, MessageModel messageModel,
                              List<CodeReferenceModel> remoteRefs, List<CodeReferenceModel> localRefs, int chatType) {
        DevPilotMessage userMessage;
        if (data == null || data.isEmpty()) {
            userMessage = MessageUtil.createUserMessage(message, msgType, messageModel.getId());
        } else {
            userMessage = MessageUtil.createPromptMessage(messageModel.getId(), msgType, message, data);
        }

        // check session type,default multi session
        var devPilotChatCompletionRequest = new DevPilotChatCompletionRequest();
        var sessionTypeEnum = SessionTypeEnum.getEnumByCode(sessionType);
        if (SessionTypeEnum.INDEPENDENT.equals(sessionTypeEnum)) {
            // independent message can not update, just readonly
            devPilotChatCompletionRequest.setStream(false);
            devPilotChatCompletionRequest.getMessages().add(userMessage);
        } else {
            devPilotChatCompletionRequest.setStream(true);
            historyRequestMessageList.add(userMessage);
            devPilotChatCompletionRequest.getMessages().addAll(copyHistoryRequestMessageList(historyRequestMessageList));
        }

        this.llmProvider = new LlmProviderFactory().getLlmProvider(project);
        var chatCompletion = this.llmProvider.chatCompletion(project, devPilotChatCompletionRequest, callback, remoteRefs, localRefs, chatType);
        if (MULTI_TURN.equals(sessionTypeEnum) &&
                devPilotChatCompletionRequest.getMessages().size() > historyRequestMessageList.size()) {
            // update multi session request
            historyRequestMessageList.add(
                    devPilotChatCompletionRequest.getMessages().get(devPilotChatCompletionRequest.getMessages().size() - 1));
        }

        return chatCompletion;
    }

    public String sendMessage(Consumer<String> callback, Map<String, String> data,
                              List<CodeReferenceModel> remoteRefs, List<CodeReferenceModel> localRefs, int chatType) {
        // if data is not empty, the data should add into last history request message
        if (data != null && !data.isEmpty() && !historyMessageList.isEmpty()) {
            var lastHistoryRequestMessage = historyRequestMessageList.get(historyRequestMessageList.size() - 1);
            if (lastHistoryRequestMessage.getPromptData() == null) {
                lastHistoryRequestMessage.setPromptData(new HashMap<>());
            }
            lastHistoryRequestMessage.getPromptData().putAll(data);
        }

        var devPilotChatCompletionRequest = new DevPilotChatCompletionRequest();
        devPilotChatCompletionRequest.setStream(true);
        devPilotChatCompletionRequest.getMessages().addAll(copyHistoryRequestMessageList(historyRequestMessageList));

        this.llmProvider = new LlmProviderFactory().getLlmProvider(project);

        var chatCompletion = this.llmProvider.chatCompletion(project, devPilotChatCompletionRequest, callback, remoteRefs, localRefs, chatType);
        if (devPilotChatCompletionRequest.getMessages().size() > historyRequestMessageList.size()) {
            // update multi session request
            historyRequestMessageList.add(
                    devPilotChatCompletionRequest.getMessages().get(devPilotChatCompletionRequest.getMessages().size() - 1));
        }

        return chatCompletion;
    }

    public void interruptSend() {
        this.cancel.set(true);
        if (this.lastMessage.getRecall() == null || this.nowStep.get() >= 3) {
            this.llmProvider.interruptSend();
        } else {
            if (this.lastMessage != null) {
                this.lastMessage.setStreaming(false);
                this.lastMessage.setRecall(RecallModel.createTerminated(this.nowStep.get()));
                addMessage(this.lastMessage);
                callWebView();
                this.lastMessage = null;
            }
        }
    }

    /**
     * Only used in CODE_PREDICTION for minimizing request data size.
     * @param devPilotMessages
     */
    private List<DevPilotMessage> removeRedundantRelatedContext(List<DevPilotMessage> devPilotMessages) {
        if (CollectionUtils.isEmpty(devPilotMessages)) {
            return Collections.emptyList();
        }
        ArrayList<DevPilotMessage> copy = new ArrayList<>(devPilotMessages);
        copy.forEach(
                msg -> {
                    if (msg.getPromptData() != null) {
                        msg.getPromptData().remove("relatedContext");
                    }
                }
        );
        return copy;
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
        lastMessage = historyMessageList.get(historyMessageList.size() - 1);

        if (!lastMessage.getRole().equals("user")) {
            return;
        }

        regenerateChat(lastMessage, null);
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
        if (messageModel.getCodeRef() != null) {
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

    private String getProjectPathString() {
        var path = project.getBasePath();

        if (path == null) {
            return null;
        }

        return EncryptionUtil.getMD5Hash(path);
    }

    static class Rag {
        private List<PsiElement> localRag;
        private List<String> remoteRag;

        public Rag(List<PsiElement> localRag, List<String> remoteRag) {
            this.localRag = localRag;
            this.remoteRag = remoteRag;
        }

        public List<PsiElement> getLocalRag() {
            return localRag;
        }

        public void setLocalRag(List<PsiElement> localRag) {
            this.localRag = localRag;
        }

        public List<String> getRemoteRag() {
            return remoteRag;
        }

        public void setRemoteRag(List<String> remoteRag) {
            this.remoteRag = remoteRag;
        }
    }
}
