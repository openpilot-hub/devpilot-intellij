package com.zhongan.devpilot.gui.toolwindows.chat;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.zhongan.devpilot.DevPilotVersion;
import com.zhongan.devpilot.actions.editor.popupmenu.BasicEditorAction;
import com.zhongan.devpilot.agents.BinaryManager;
import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.embedding.entity.request.EmbeddingQueryRequest;
import com.zhongan.devpilot.embedding.entity.request.EmbeddingQueryResponse;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.gui.toolwindows.components.EditorInfo;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.LlmProviderFactory;
import com.zhongan.devpilot.integrations.llms.entity.CompletionRelatedCodeInfo;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotCodePrediction;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotCompletionPredictRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotRagRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotRagResponse;
import com.zhongan.devpilot.mcp.McpConfigurationHandler;
import com.zhongan.devpilot.mcp.McpConnections;
import com.zhongan.devpilot.mcp.McpServer;
import com.zhongan.devpilot.provider.file.FileAnalyzeProviderFactory;
import com.zhongan.devpilot.session.ChatSessionManager;
import com.zhongan.devpilot.session.ChatSessionManagerService;
import com.zhongan.devpilot.session.model.ChatSession;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.util.BalloonAlertUtils;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.EncryptionUtil;
import com.zhongan.devpilot.util.JsonUtils;
import com.zhongan.devpilot.util.MessageUtil;
import com.zhongan.devpilot.util.PromptDataMapUtils;
import com.zhongan.devpilot.util.PsiElementUtils;
import com.zhongan.devpilot.webview.model.AgentDecisionModel;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;
import com.zhongan.devpilot.webview.model.EmbeddedModel;
import com.zhongan.devpilot.webview.model.JavaCallModel;
import com.zhongan.devpilot.webview.model.LocaleModel;
import com.zhongan.devpilot.webview.model.LoginModel;
import com.zhongan.devpilot.webview.model.MessageModel;
import com.zhongan.devpilot.webview.model.RecallModel;
import com.zhongan.devpilot.webview.model.ThemeModel;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.JFrame;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import static com.zhongan.devpilot.constant.DefaultConst.AGENT_CHAT_TYPE;
import static com.zhongan.devpilot.constant.DefaultConst.CHAT_STEP_ONE;
import static com.zhongan.devpilot.constant.DefaultConst.CHAT_STEP_THREE;
import static com.zhongan.devpilot.constant.DefaultConst.CHAT_STEP_TWO;
import static com.zhongan.devpilot.constant.DefaultConst.CODE_PREDICT_PROMPT_VERSION;
import static com.zhongan.devpilot.constant.DefaultConst.D2C_PROMPT_VERSION;
import static com.zhongan.devpilot.constant.DefaultConst.NORMAL_CHAT_TYPE;
import static com.zhongan.devpilot.constant.DefaultConst.SMART_CHAT_TYPE;
import static com.zhongan.devpilot.constant.PlaceholderConst.ANSWER_LANGUAGE;
import static com.zhongan.devpilot.constant.PlaceholderConst.LANGUAGE;

@Service
public final class DevPilotChatToolWindowService {
    private static final Logger LOG = Logger.getInstance(DevPilotChatToolWindowService.class);

    private final Project project;

    private final DevPilotChatToolWindow devPilotChatToolWindow;

    private final ChatSessionManager sessionManager;

    private LlmProvider llmProvider;

    private final AtomicBoolean cancel = new AtomicBoolean(false);

    private MessageModel lastMessage = new MessageModel();

    private final AtomicInteger nowStep = new AtomicInteger(1);

    private volatile String currentMessageId = null;

    public DevPilotChatToolWindowService(Project project) {
        this.project = project;
        this.sessionManager = project.getService(ChatSessionManagerService.class).getSessionManager();
        this.devPilotChatToolWindow = new DevPilotChatToolWindow(project);
        subscribeToFocusEvents();
    }

    public DevPilotChatToolWindow getDevPilotChatToolWindow() {
        return this.devPilotChatToolWindow;
    }

    private void subscribeToFocusEvents() {
        JFrame frame = WindowManager.getInstance().getFrame(project);
        if (frame != null) {
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowActivated(WindowEvent e) {
                    if (sessionManager.isSessionUpdated()) {
                        sessionManager.setSessionUpdated(Boolean.FALSE);
                        callWebView(Boolean.TRUE);
                    }
                }
            });
        }
    }

    public Project getProject() {
        return this.project;
    }

    public void chat(String msgType, Map<String, String> data,
                     String message, Consumer<String> callback, MessageModel messageModel) {
        this.cancel.set(false);
        this.currentMessageId = messageModel.getId();
        this.lastMessage = messageModel;

        callWebView(messageModel);
        addMessage(messageModel);
        callWebView(MessageModel.buildLoadingMessage());

        this.llmProvider = LlmProviderFactory.INSTANCE.getLlmProvider(project);

        if (DefaultConst.NORMAL_CHAT_TYPE == messageModel.getChatMode()) {
            normalChat(msgType, data, message, callback, messageModel);
            return;
        }

        smartChat(msgType, data, message, callback, messageModel);
    }

    public void deepThinking(Map<String, String> data,
                             String message, MessageModel messageModel) {
        this.cancel.set(false);
        this.currentMessageId = messageModel.getId();
        this.lastMessage = messageModel;

        // 显示初始消息
        callWebView(messageModel);
        addMessage(messageModel);
        callWebView(MessageModel.buildLoadingMessage());

        this.llmProvider = LlmProviderFactory.INSTANCE.getLlmProvider(project);

        DevPilotMessage userMessage;
        if (data == null || data.isEmpty()) {
            userMessage = MessageUtil.createUserMessage(message, "PURE_CHAT", messageModel.getId());
        } else {
            userMessage = MessageUtil.createPromptMessage(messageModel.getId(), "PURE_CHAT", message, data);
        }

        // 处理会话
        ChatSession chatSession = sessionManager.getCurrentSession();
        chatSession.setChatMode(DefaultConst.AGENT_CHAT_TYPE);
        List<DevPilotMessage> historyRequestMessageList = chatSession.getHistoryRequestMessageList();
        historyRequestMessageList.add(userMessage);
        chatSession.setAbort(Boolean.FALSE);
        sessionManager.saveSession(chatSession);

        // 启动深度思考
        this.llmProvider.deepThinking(project, sessionManager.getSessionsDir(), chatSession);
    }

    public void normalChat(String msgType, Map<String, String> data,
                           String message, Consumer<String> callback, MessageModel messageModel) {
        sendMessage(msgType, data, message, callback, messageModel, null, null, NORMAL_CHAT_TYPE);
    }

    public void smartChat(String msgType, Map<String, String> data,
                          String message, Consumer<String> callback, MessageModel messageModel) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // step1 call model to do code prediction
            if (shouldCancelChat(messageModel)) {
                return;
            }

            this.nowStep.set(CHAT_STEP_ONE);
            var references = codePredict(messageModel.getContent(), messageModel.getCodeRefs(), msgType);

            // step2 call rag to analyze code
            if (shouldCancelChat(messageModel)) {
                return;
            }

            this.nowStep.set(CHAT_STEP_TWO);
            var rag = callRag(references, messageModel.getCodeRefs(), message);

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
                    var language = CodeReferenceModel.getLanguage(messageModel.getCodeRefs());

                    FileAnalyzeProviderFactory.getProvider(language)
                            .buildRelatedContextDataMap(project, messageModel.getCodeRefs(), rag.localRag, rag.remoteRag, rag.localEmbeddingRag, newMap);

                    if (rag.localRag != null) {
                        localRefs[0] = CodeReferenceModel.getCodeRefListFromPsiElement(rag.localRag, EditorActionEnum.getEnumByName(msgType));
                    }

                    if (rag.remoteRag != null) {
                        remoteRefs[0] = CodeReferenceModel.getCodeRefFromString(rag.remoteRag, language);
                    }

                    if (rag.localEmbeddingRag != null) {
                        if (localRefs[0] == null) {
                            localRefs[0] = CodeReferenceModel.getCodeRefFromRag(project, rag.localEmbeddingRag, language);
                        } else {
                            localRefs[0].addAll(CodeReferenceModel.getCodeRefFromRag(project, rag.localEmbeddingRag, language));
                        }
                    }
                });
            }

            sendMessage(msgType, newMap, message, callback, messageModel, remoteRefs[0], localRefs[0], SMART_CHAT_TYPE);
        });
    }

    public void regenerateChat(MessageModel messageModel, Consumer<String> callback) {
        this.cancel.set(false);

        callWebView(MessageModel.buildLoadingMessage());

        this.llmProvider = LlmProviderFactory.INSTANCE.getLlmProvider(project);

        if (DefaultConst.NORMAL_CHAT_TYPE == messageModel.getChatMode()) {
            regenerateNormalChat(messageModel, callback);
            return;
        }

        regenerateSmartChat(messageModel, callback);
    }

    public void regenerateNormalChat(MessageModel messageModel, Consumer<String> callback) {
        regenerateMessage(callback, null, null, null, NORMAL_CHAT_TYPE, messageModel);
    }

    public void regenerateSmartChat(MessageModel messageModel, Consumer<String> callback) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // step1 call model to do code prediction
            if (shouldCancelChat(messageModel)) {
                return;
            }

            this.nowStep.set(CHAT_STEP_ONE);
            var references = codePredict(messageModel.getContent(), messageModel.getCodeRefs(), null);

            // step2 call rag to analyze code
            if (shouldCancelChat(messageModel)) {
                return;
            }

            this.nowStep.set(CHAT_STEP_TWO);
            var rag = callRag(references, messageModel.getCodeRefs(), messageModel.getContent());

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
                    var language = CodeReferenceModel.getLanguage(messageModel.getCodeRefs());

                    FileAnalyzeProviderFactory.getProvider(language)
                            .buildRelatedContextDataMap(project, messageModel.getCodeRefs(), rag.localRag, rag.remoteRag, rag.localEmbeddingRag, data);

                    EditorActionEnum type = null;
                    if (messageModel.getCodeRefs() != null) {
                        type = CodeReferenceModel.getLastType(messageModel.getCodeRefs());
                    }

                    if (rag.localRag != null) {
                        localRefs[0] = CodeReferenceModel.getCodeRefListFromPsiElement(rag.localRag, type);
                    }

                    if (rag.remoteRag != null) {
                        remoteRefs[0] = CodeReferenceModel.getCodeRefFromString(rag.remoteRag, language);
                    }

                    if (rag.localEmbeddingRag != null) {
                        if (localRefs[0] == null) {
                            localRefs[0] = CodeReferenceModel.getCodeRefFromRag(project, rag.localEmbeddingRag, language);
                        } else {
                            localRefs[0].addAll(CodeReferenceModel.getCodeRefFromRag(project, rag.localEmbeddingRag, language));
                        }
                    }
                });
            }

            regenerateMessage(callback, data, remoteRefs[0], localRefs[0], SMART_CHAT_TYPE, messageModel);
        });
    }

    private boolean shouldCancelChat(MessageModel messageModel) {
        if (cancel.get()) {
            return true;
        }

        return !StringUtils.equals(currentMessageId, messageModel.getId());
    }

    public List<CompletionRelatedCodeInfo> buildCompletionRelatedFile(String filePath, String document, int position, String language) {
        var predict = completionCodePredict(filePath, document, position, language);
        if (predict != null) {
            var result = new CopyOnWriteArrayList<CompletionRelatedCodeInfo>();

            // 本地索引召回
            ApplicationManager.getApplication().runReadAction(() -> {
                var list = FileAnalyzeProviderFactory.getProvider(language).callLocalRag(project, predict);
                for (var element : list) {
                    var info = new CompletionRelatedCodeInfo();
                    info.setScore(1.0d);
                    info.setFilePath(element.getContainingFile().getName());
                    info.setCode(element.getText());
                    result.add(info);
                }
            });

            // 本地向量库召回
            if (!StringUtils.isEmpty(predict.getComments())) {
                this.llmProvider = LlmProviderFactory.INSTANCE.getLlmProvider(project);
                var embeddingRequest = new EmbeddingQueryRequest();
                embeddingRequest.setProjectName(project.getBasePath());
                embeddingRequest.setHomeDir(BinaryManager.INSTANCE.getHomeDir().getAbsolutePath());
                embeddingRequest.setContent(predict.getComments());

                var embeddingResponse = this.llmProvider.embeddingQuery(embeddingRequest);
                if (embeddingResponse != null) {
                    var hitDataList = embeddingResponse.getHitsData();
                    for (EmbeddingQueryResponse.HitData hitData : hitDataList) {
                        var code = PsiElementUtils.getCodeBlock(project,
                                hitData.getFilePath(), hitData.getStartOffset(), hitData.getEndOffset());
                        if (code == null) {
                            continue;
                        }
                        var info = new CompletionRelatedCodeInfo();
                        info.setScore(Double.parseDouble(hitData.getScore()));
                        info.setFilePath(hitData.getFilePath());
                        info.setCode(code);
                        result.add(info);
                    }
                }
            }

            // 本地索引和本地向量库一起返回
            return result;
        }

        return null;
    }

    public DevPilotCodePrediction completionCodePredict(String filePath, String document, int position, String language) {
        this.llmProvider = LlmProviderFactory.INSTANCE.getLlmProvider(project);

        var completionPredictRequest = new DevPilotCompletionPredictRequest();
        completionPredictRequest.setFilePath(filePath);
        completionPredictRequest.setDocument(document);
        completionPredictRequest.setPosition(position);
        completionPredictRequest.setLanguage(language);

        var response = this.llmProvider.completionCodePrediction(completionPredictRequest);
        if (!response.isSuccessful() || response.getContent() == null) {
            return null;
        }
        return JsonUtils.fromJson(JsonUtils.fixJson(response.getContent()), DevPilotCodePrediction.class);
    }

    private DevPilotCodePrediction codePredict(String content, List<CodeReferenceModel> codeReference, String commandType) {
        this.lastMessage = MessageModel
                .buildAssistantMessage(System.currentTimeMillis() + "", System.currentTimeMillis(), "", true, RecallModel.create(1));
        callWebView(this.lastMessage);

        final Map<String, String> dataMap = new HashMap<>();

        var type = CodeReferenceModel.getLastType(codeReference);

        if (commandType == null) {
            if (codeReference == null || type == null) {
                commandType = "PURE_CHAT";
            } else {
                commandType = type.name();
            }
        }

        dataMap.put("commandTypeFor", commandType);

        if (codeReference != null) {
            ApplicationManager.getApplication().runReadAction(() -> {
                PromptDataMapUtils.buildCodePredictDataMap(project, codeReference, dataMap);
            });
        }

        var devPilotChatCompletionRequest = new DevPilotChatCompletionRequest();
        devPilotChatCompletionRequest.setVersion(CODE_PREDICT_PROMPT_VERSION);
        devPilotChatCompletionRequest.getMessages().addAll(removeRedundantRelatedContext(copyHistoryRequestMessageList(sessionManager.getCurrentSession().getHistoryRequestMessageList())));
        devPilotChatCompletionRequest.getMessages().add(
                MessageUtil.createPromptMessage(System.currentTimeMillis() + "", "CODE_PREDICTION", content, dataMap));
        devPilotChatCompletionRequest.setStream(Boolean.FALSE);
        var response = this.llmProvider.codePrediction(devPilotChatCompletionRequest);
        if (!response.isSuccessful() || response.getContent() == null) {
            return null;
        }
        return JsonUtils.fromJson(JsonUtils.fixJson(response.getContent()), DevPilotCodePrediction.class);
    }

    private Rag callRag(DevPilotCodePrediction codePredict, List<CodeReferenceModel> codeReference, String message) {
        this.lastMessage = MessageModel
                .buildAssistantMessage(System.currentTimeMillis() + "", System.currentTimeMillis(), "", true, RecallModel.create(2));
        callWebView(this.lastMessage);

        return ApplicationManager.getApplication().runReadAction((Computable<Rag>) () -> {
            final List<PsiElement>[] localRag = new List[1];
            final List<String>[] remoteRag = new List[1];
            final List<EmbeddingQueryResponse.HitData>[] localEmbedding = new List[1];

            var language = CodeReferenceModel.getLanguage(codeReference);

            CountDownLatch latch = new CountDownLatch(3);

            ApplicationManager.getApplication().runReadAction(() -> {
                try {
                    // call local rag
                    if (codePredict != null) {
                        localRag[0] = FileAnalyzeProviderFactory
                                .getProvider(language).callLocalRag(project, codePredict);
                    }
                } finally {
                    latch.countDown();
                }
            });

            ApplicationManager.getApplication().runReadAction(() -> {
                try {
                    // call local embedding
                    var embeddingRequest = new EmbeddingQueryRequest();
                    embeddingRequest.setProjectName(project.getBasePath());
                    embeddingRequest.setHomeDir(BinaryManager.INSTANCE.getHomeDir().getAbsolutePath());
                    embeddingRequest.setContent(message);
                    if (codeReference != null) {
                        embeddingRequest.setSelectedCode(CodeReferenceModel.getLastSourceCode(codeReference));
                    }

                    var embeddingResponse = llmProvider.embeddingQuery(embeddingRequest);
                    if (embeddingResponse != null) {
                        localEmbedding[0] = embeddingResponse.getHitsData();
                    }
                } finally {
                    latch.countDown();
                }
            });

            ApplicationManager.getApplication().runReadAction(() -> {
                try {
                    // menu action will not call remote rag
                    if (codeReference == null || CodeReferenceModel.getLastType(codeReference) == null) {
                        remoteRag[0] = null;
                    }

                    // call remote rag
                    var request = new DevPilotRagRequest();
                    if (codeReference != null) {
                        request.setSelectedCode(CodeReferenceModel.getLastSourceCode(codeReference));
                    }
                    request.setProjectType(language);
                    if (message != null) {
                        request.setContent(message);
                    }

                    // calculate md5 of project path as unique id
                    request.setProjectName(getProjectPathString());

                    if (codePredict != null) {
                        request.setPredictionComments(codePredict.getComments());
                    }

                    var response = this.llmProvider.ragCompletion(request);

                    if (response != null) {
                        remoteRag[0] = response.stream().map(DevPilotRagResponse::getCode)
                                .filter(Objects::nonNull).collect(Collectors.toList());
                    }
                } finally {
                    latch.countDown();
                }
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                return new Rag(localRag[0], remoteRag[0], localEmbedding[0]);
            }

            return new Rag(localRag[0], remoteRag[0], localEmbedding[0]);
        });
    }

    public String sendMessage(String msgType, Map<String, String> data,
                              String message, Consumer<String> callback, MessageModel messageModel,
                              List<CodeReferenceModel> remoteRefs, List<CodeReferenceModel> localRefs, int chatType) {
        DevPilotMessage userMessage;
        if (data == null || data.isEmpty()) {
            userMessage = MessageUtil.createUserMessage(message, msgType, messageModel.getId());
        } else {
            userMessage = MessageUtil.createPromptMessage(messageModel.getId(), msgType, message, data);
        }

        // check session type,default multi session
        List<DevPilotMessage> historyRequestMessageList = sessionManager.getCurrentSession().getHistoryRequestMessageList();
        historyRequestMessageList.add(userMessage);

        return sendMessage(historyRequestMessageList, msgType, remoteRefs, localRefs, chatType, callback);
    }

    private String sendMessage(List<DevPilotMessage> historyRequestMessageList,
                               String msgType,
                               List<CodeReferenceModel> remoteRefs, List<CodeReferenceModel> localRefs, int chatType,
                               Consumer<String> callback) {
        var devPilotChatCompletionRequest = new DevPilotChatCompletionRequest();
        devPilotChatCompletionRequest.setStream(true);
        devPilotChatCompletionRequest.getMessages().addAll(copyHistoryRequestMessageList(historyRequestMessageList));

        if ("EXTERNAL_AGENTS".equals(msgType)) {
            devPilotChatCompletionRequest.setVersion(D2C_PROMPT_VERSION);
        }

        this.llmProvider = LlmProviderFactory.INSTANCE.getLlmProvider(project);

        sessionManager.saveSession(sessionManager.getCurrentSession());
        return this.llmProvider.chatCompletion(project, devPilotChatCompletionRequest, callback, remoteRefs, localRefs, chatType);
    }

    public void regenerateMessage(Consumer<String> callback, Map<String, String> data,
                                  List<CodeReferenceModel> remoteRefs, List<CodeReferenceModel> localRefs, int chatType, MessageModel messageModel) {
        // if data is not empty, the data should add into last history request message
        List<MessageModel> historyMessageList = sessionManager.getCurrentSession().getHistoryMessageList();
        List<DevPilotMessage> historyRequestMessageList = sessionManager.getCurrentSession().getHistoryRequestMessageList();

        if (data != null && !data.isEmpty() && !historyMessageList.isEmpty()) {
            var lastHistoryRequestMessage = historyRequestMessageList.get(historyRequestMessageList.size() - 1);
            if (lastHistoryRequestMessage.getPromptData() == null) {
                lastHistoryRequestMessage.setPromptData(new HashMap<>());
            }
            lastHistoryRequestMessage.getPromptData().putAll(data);
        }
        sendMessage(historyRequestMessageList, messageModel.getMsgType(), remoteRefs, localRefs, chatType, callback);
    }

    public void interruptSend() {
        // 判断当前会话的chatMode是否是Agent
        ChatSession chatSession = sessionManager.getCurrentSession();
        LOG.info("Interrupt event received for chat: " + chatSession.getId() + " with chatMode:" + chatSession.getChatMode() + ".");
        if (AGENT_CHAT_TYPE == chatSession.getChatMode()) {
            // 发请求推送通知Node去处理Cancel事件
            this.llmProvider = LlmProviderFactory.INSTANCE.getLlmProvider(project);
            this.llmProvider.cancel(project, sessionManager.getSessionsDir(), sessionManager.getCurrentSession());
        } else {
            this.cancel.set(true);
            if (this.lastMessage.getRecall() == null || this.nowStep.get() >= 3) {
                this.llmProvider.interruptSend();
            } else {
                if (this.lastMessage != null) {
                    this.lastMessage.setStreaming(false);
                    this.lastMessage.setRecall(RecallModel.createTerminated(this.nowStep.get()));
                    addMessage(this.lastMessage);
                    callWebView(Boolean.FALSE);
                    this.lastMessage = null;
                }
            }
        }
    }

    /**
     * Only used in CODE_PREDICTION for minimizing request data size.
     *
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

    public void handleCreateNewSession() {
        sessionManager.createNewSession();
        callWebView(Boolean.FALSE);
    }

    public void handleSwitchSession(String sessionId) {
        sessionManager.switchSession(sessionId);
        callWebView(Boolean.TRUE);
    }

    public void handleDeleteSession(String sessionId) {
        sessionManager.deleteSession(sessionId, Boolean.FALSE);
        renderHistorySession();
    }

    public List<MessageModel> getHistoryMessageList(boolean forceUpdate) {
        if (forceUpdate) {
            ChatSession currentSession = sessionManager.getSessions().stream()
                    .filter(session -> session.getId().equals(sessionManager.getCurrentSession().getId()))
                    .findFirst()
                    .orElse(null);
            if (null == currentSession) {
                currentSession = sessionManager.createNewSession();
            }
            return currentSession.getHistoryMessageList();
        } else {
            return sessionManager.getCurrentSession().getHistoryMessageList();
        }
    }

    public void addMessage(MessageModel messageModel) {
        ChatSession currentSession = sessionManager.getCurrentSession();
        currentSession.getHistoryMessageList().add(messageModel);
        sessionManager.saveSession(currentSession);
    }

    public void addRequestMessage(DevPilotMessage message) {
        ChatSession currentSession = sessionManager.getCurrentSession();
        currentSession.getHistoryRequestMessageList().add(message);
        sessionManager.saveSession(currentSession);
    }

    public void clearSession() {
        sessionManager.clearSession();
        callWebView(Boolean.FALSE);
    }

    // Do not clear message show session
    public void clearRequestSession() {
        sessionManager.getCurrentSession().getHistoryRequestMessageList().clear();

        if (sessionManager.getCurrentSession().getHistoryMessageList().isEmpty()) {
            return;
        }

        var dividerModel = MessageModel.buildDividerMessage();
        callWebView(dividerModel);
        sessionManager.getCurrentSession().getHistoryMessageList().add(dividerModel);
    }

    public void clearRequestSessionAndChangeChatMode(int chatMode) {
        clearRequestSession();
        sessionManager.getCurrentSession().setChatMode(chatMode);
    }

    public void deleteMessage(String id) {
        List<MessageModel> historyMessageList = sessionManager.getCurrentSession().getHistoryMessageList();
        List<DevPilotMessage> historyRequestMessageList = sessionManager.getCurrentSession().getHistoryRequestMessageList();

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

        historyRequestMessageList.removeIf(item -> StringUtils.equals(id, item.getId()));
        if (assistantMessageId != null) {
            var finalAssistantMessageId = assistantMessageId;
            historyRequestMessageList.removeIf(item -> StringUtils.equals(finalAssistantMessageId, item.getId()));
        }
        sessionManager.deleteMessage(id);
        callWebView(Boolean.FALSE);
    }

    public void regenerateMessage() {
        List<MessageModel> historyMessageList = sessionManager.getCurrentSession().getHistoryMessageList();
        var lastMessage = historyMessageList.get(historyMessageList.size() - 1);

        if (!lastMessage.getRole().equals("assistant")) {
            return;
        }

        var id = lastMessage.getId();
        historyMessageList.removeIf(item -> StringUtils.equals(id, item.getId()));
        sessionManager.getCurrentSession().getHistoryRequestMessageList().removeIf(item -> StringUtils.equals(id, item.getId()));

        // todo handle real callback
        lastMessage = historyMessageList.get(historyMessageList.size() - 1);

        if (!lastMessage.getRole().equals("user")) {
            return;
        }

        regenerateChat(lastMessage, null);
    }

    // called by ide
    public void handleActions(EditorActionEnum actionEnum, PsiElement psiElement, int mode) {
        ActionManager actionManager = ActionManager.getInstance();
        BasicEditorAction myAction = (BasicEditorAction) actionManager
                .getAction(DevPilotMessageBundle.get(actionEnum.getLabel()));
        ApplicationManager.getApplication().invokeLater(() -> {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor == null || !editor.getSelectionModel().hasSelection()) {
                BalloonAlertUtils.showWarningAlert(DevPilotMessageBundle.get("devpilot.alter.code.not.selected"), 0, -10, Balloon.Position.above);
                return;
            }
            myAction.fastAction(project, editor, editor.getSelectionModel().getSelectedText(), psiElement, null, mode);
        });
    }

    // called by web view
    public void handleActions(CodeReferenceModel codeReferenceModel, EditorActionEnum actionEnum, PsiElement psiElement, int mode) {
        if (codeReferenceModel == null || StringUtils.isEmpty(codeReferenceModel.getSourceCode())) {
            handleActions(actionEnum, psiElement, mode);
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
            myAction.fastAction(project, editor, codeReferenceModel.getSourceCode(), psiElement, codeReferenceModel, mode);
        });
    }

    public MessageModel getUserContentCode(MessageModel messageModel) {
        List<CodeReferenceModel> codeRefs = new ArrayList<>();

        if (messageModel.getCodeRefs() != null) {
            codeRefs = messageModel.getCodeRefs();
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

        // 检查sourceCode是否在codeRefs重复，重复就删除旧的
        var code = editorInfo[0].getSourceCode();
        // 不管存不存在都删除，反正最后都会追加
        codeRefs.removeIf(codeRef -> StringUtils.equals(codeRef.getSourceCode(), code));

        var codeReference = CodeReferenceModel.getCodeRefFromEditor(editorInfo[0], null);
        codeRefs.add(codeReference);

        messageModel.setCodeRefs(codeRefs);

        return messageModel;
    }

    // get user message by assistant message id
    public MessageModel getUserMessage(String id) {
        var index = getMessageIndex(id);

        if (index == -1) {
            return null;
        }

        var userMessage = sessionManager.getCurrentSession().getHistoryMessageList().get(index - 1);

        if (!userMessage.getRole().equals("user")) {
            return null;
        }

        return userMessage;
    }

    private int getMessageIndex(String id) {
        List<MessageModel> historyMessageList = sessionManager.getCurrentSession().getHistoryMessageList();
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
        var messageList = getHistoryMessageList(Boolean.FALSE);

        var tmpList = new ArrayList<>(messageList);
        tmpList.add(messageModel);

        var javaCallModel = new JavaCallModel();
        javaCallModel.setCommand("RenderChatConversation");
        javaCallModel.setPayload(tmpList);

        callWebView(javaCallModel);
    }

    public void callWebView(boolean forceUpdate) {
        var messageList = getHistoryMessageList(forceUpdate);

        var javaCallModel = new JavaCallModel();
        javaCallModel.setCommand("RenderChatConversation");
        javaCallModel.setPayload(messageList);

        callWebView(javaCallModel);
    }

    public void renderHistorySession() {
        var javaCallModel = new JavaCallModel();
        javaCallModel.setCommand("ShowHistory");
        javaCallModel.setPayload(sessionManager.getSessions().stream()
                .filter(t -> CollectionUtils.isNotEmpty(t.getHistoryRequestMessageList()))
                .collect(Collectors.toList()));
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

    public void switchChatMode(Object payload) {
        ChatSession currentSession = sessionManager.getCurrentSession();
        if (currentSession == null) {
            return;
        }

        ChatSession newSession = JsonUtils.fromJson(JsonUtils.toJson(payload), ChatSession.class);
        if (newSession == null || newSession.getChatMode() == 0) {
            return;
        }

        int currentChatMode = currentSession.getChatMode();
        int newChatMode = newSession.getChatMode();

        if (!StringUtils.equalsIgnoreCase(newSession.getId(), currentSession.getId()) || (AGENT_CHAT_TYPE == currentChatMode && AGENT_CHAT_TYPE != newChatMode)) {
            sessionManager.createNewSessionWithChatMode(newChatMode);
        } else {
            currentSession.setChatMode(newChatMode);
        }

        callWebView(Boolean.FALSE);
    }

    public void listMcpServers() {
        McpConnections result = McpConfigurationHandler.INSTANCE.loadMcpServersWithConnectionStatus(Boolean.TRUE);
        var javaCallModel = new JavaCallModel();
        javaCallModel.setCommand("ShowMcpServers");
        javaCallModel.setPayload(result);
        callWebView(javaCallModel);
    }

    public void openMcpServerConfigurationFile() {
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(McpConfigurationHandler.INSTANCE.mcpConfigurationPath()));
        if (virtualFile != null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true);
        } else {
            LOG.warn("Error occurred while opening mcp_configuration.json");
            Messages.showWarningDialog(project, DevPilotMessageBundle.get("devpilot.warning.mcpConfiguration.opening"), DevPilotMessageBundle.get("devpilot.warning"));
        }
    }

    public void mcpServerChanged(Object payload) {
        LOG.warn("-------McpServerChanged---------");
        Map<String, Object> eventMap = JsonUtils.fromJson(JsonUtils.toJson(payload), Map.class);
        String operatorType = (String) eventMap.get("operatorType");
        McpServer server = JsonUtils.fromJson(JsonUtils.toJson(eventMap.get("server")), McpServer.class);
        McpConnections result = McpConfigurationHandler.INSTANCE.handleMcpServerChanged(operatorType, server);

//        McpConnections result = McpConfigurationHandler.INSTANCE.loadMcpServersWithConnectionStatus(Boolean.TRUE);
        var javaCallModel = new JavaCallModel();
        javaCallModel.setCommand("ShowMcpServers");
        javaCallModel.setPayload(result);
        callWebView(javaCallModel);
    }

    public void agentExecutionApprovedOrNot(Object payload) {
        Map<String, Boolean> eventMap = JsonUtils.fromJson(JsonUtils.toJson(payload), Map.class);
        boolean approvedOrNot = eventMap.getOrDefault("approved", Boolean.FALSE);
        var currentSession = sessionManager.getCurrentSession();
        var historyMessageList = currentSession.getHistoryMessageList();
        MessageModel lastMessage = historyMessageList.isEmpty() ? null : historyMessageList.get(historyMessageList.size() - 1);
        if (null != lastMessage && "assistant".equals(lastMessage.getRole())) {
            var decisions = lastMessage.getDecisions();
            if (decisions.isEmpty()) {
                return;
            }
            AgentDecisionModel decision = decisions.get(decisions.size() - 1);
            if (null == decision || !StringUtils.equalsIgnoreCase(decision.getActionType(), "RequireApprove")) {
                return;
            }

            decision.setResult(String.valueOf(approvedOrNot));

            if (approvedOrNot) {
                lastMessage.setStreaming(Boolean.TRUE);
                this.llmProvider = LlmProviderFactory.INSTANCE.getLlmProvider(project);
                Map<String, String> data = new HashMap<>();
                String answerLanguage = LanguageSettingsState.getInstance().getLanguageIndex() == 1 ? "zh_CN" : "en_US";
                data.put(ANSWER_LANGUAGE, answerLanguage);
                data.put(LANGUAGE, DevPilotVersion.getDefaultLanguage());

                DevPilotMessage userMessage = MessageUtil.createPromptMessage(UUID.randomUUID().toString(), "PURE_CHAT", "OK", data);
                ChatSession chatSession = sessionManager.getCurrentSession();
                List<DevPilotMessage> historyRequestMessageList = chatSession.getHistoryRequestMessageList();
                historyRequestMessageList.add(userMessage);
                chatSession.setAbort(Boolean.FALSE);
                chatSession.setChatMode(DefaultConst.AGENT_CHAT_TYPE);
                sessionManager.saveSession(chatSession);
                this.llmProvider.deepThinking(project, sessionManager.getSessionsDir(), chatSession);
            } else {
                lastMessage.setStreaming(Boolean.FALSE);
                sessionManager.sessionUIRefreshed();
            }
            callWebView(false);
        }
    }

    static class Rag {
        private List<PsiElement> localRag;

        private List<String> remoteRag;

        private List<EmbeddingQueryResponse.HitData> localEmbeddingRag;

        Rag(List<PsiElement> localRag, List<String> remoteRag, List<EmbeddingQueryResponse.HitData> localEmbeddingRag) {
            this.localRag = localRag;
            this.remoteRag = remoteRag;
            this.localEmbeddingRag = localEmbeddingRag;
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

        public List<EmbeddingQueryResponse.HitData> getLocalEmbeddingRag() {
            return localEmbeddingRag;
        }

        public void setLocalEmbeddingRag(List<EmbeddingQueryResponse.HitData> localEmbeddingRag) {
            this.localEmbeddingRag = localEmbeddingRag;
        }
    }
}
