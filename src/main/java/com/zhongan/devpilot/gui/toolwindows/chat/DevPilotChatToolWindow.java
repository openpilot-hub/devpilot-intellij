package com.zhongan.devpilot.gui.toolwindows.chat;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.zhongan.devpilot.enums.ChatActionTypeEnum;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.enums.SessionTypeEnum;
import com.zhongan.devpilot.provider.file.FileAnalyzeProviderFactory;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.util.ConfigChangeUtils;
import com.zhongan.devpilot.util.EditorUtils;
import com.zhongan.devpilot.util.JetbrainsVersionUtils;
import com.zhongan.devpilot.util.JsonUtils;
import com.zhongan.devpilot.util.LoginUtils;
import com.zhongan.devpilot.util.NewFileUtils;
import com.zhongan.devpilot.util.TelemetryUtils;
import com.zhongan.devpilot.webview.DevPilotCustomHandlerFactory;
import com.zhongan.devpilot.webview.model.CodeActionModel;
import com.zhongan.devpilot.webview.model.CodeReferenceModel;
import com.zhongan.devpilot.webview.model.JsCallModel;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JComponent;

import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.network.CefRequest;

import static com.zhongan.devpilot.constant.PlaceholderConst.SELECTED_CODE;

public class DevPilotChatToolWindow {
    private JBCefBrowser jbCefBrowser;

    private final Project project;

    private static final Map<String, EditorActionEnum> codeActionMap = new ConcurrentHashMap<>();

    static {
        codeActionMap.put("FixCode", EditorActionEnum.FIX_CODE);
        codeActionMap.put("CommentCode", EditorActionEnum.GENERATE_COMMENTS);
        codeActionMap.put("ExplainCode", EditorActionEnum.EXPLAIN_CODE);
        codeActionMap.put("TestCode", EditorActionEnum.GENERATE_TESTS);
    }

    public DevPilotChatToolWindow(Project project) {
        super();
        this.project = project;
        load();
    }

    public synchronized JBCefBrowser jbCefBrowser() {
        return this.jbCefBrowser;
    }

    private void load() {
        JBCefBrowser browser;
        try {
            boolean isOffScreenRendering = true;
            if (SystemInfo.isMac) {
                isOffScreenRendering = false;
            } else if (!SystemInfo.isLinux && !SystemInfo.isUnix) {
                if (SystemInfo.isWindows) {
                    isOffScreenRendering = true;
                }
            } else {
                isOffScreenRendering = JetbrainsVersionUtils.isVersionLaterThan233();
            }

            browser = JBCefBrowser.createBuilder().setOffScreenRendering(isOffScreenRendering).createBrowser();

        } catch (Throwable e) {
            browser = new JBCefBrowser();
        }

        registerJsCallJavaHandler(browser);
        registerLifeSpanHandler(browser);
        browser.loadURL("http://devpilot/index.html");
        this.jbCefBrowser = browser;
    }

    private void registerJsCallJavaHandler(JBCefBrowser browser) {
        var myQuery = JBCefJSQuery.create((JBCefBrowserBase) browser);

        myQuery.addHandler((query) -> {
            var jsCallModel = JsonUtils.fromJson(query, JsCallModel.class);
            if (jsCallModel == null) {
                return new JBCefJSQuery.Response("error");
            }

            var command = jsCallModel.getCommand();
            var service = project.getService(DevPilotChatToolWindowService.class);

            switch (command) {
                case "AppendToConversation": {
                    var payload = jsCallModel.getPayload();
                    var messageModel = JsonUtils.fromJson(JsonUtils.toJson(payload), MessageModel.class);
                    if (messageModel == null) {
                        return new JBCefJSQuery.Response("error");
                    }
                    var time = System.currentTimeMillis();
                    var username = DevPilotLlmSettingsState.getInstance().getFullName();
                    var uuid = UUID.randomUUID().toString();

                    var message = service.getUserContentCode(messageModel);
                    var userMessageModel = MessageModel.buildCodeMessage(uuid, time, message.getContent(), username, message.getCodeRef());

                    var data = new HashMap<String, String>();

                    if (message.getCodeRef() != null) {
                        data.put(SELECTED_CODE, message.getCodeRef().getSourceCode());

                        ApplicationManager.getApplication().invokeAndWait(() -> {
                            FileAnalyzeProviderFactory.getProvider(message.getCodeRef().getLanguageId())
                                    .buildChatDataMap(project, null, message.getCodeRef(), data);
                        });
                    }

                    service.chat(SessionTypeEnum.MULTI_TURN.getCode(), "PURE_CHAT", data, message.getContent(), null, userMessageModel);

                    return new JBCefJSQuery.Response("success");
                }
                case "InterruptChatStream": {
                    service.interruptSend();
                    return new JBCefJSQuery.Response("success");
                }
                case "ClearChatHistory": {
                    service.clearSession();
                    return new JBCefJSQuery.Response("success");
                }
                case "InsertCodeAtCaret": {
                    var payload = jsCallModel.getPayload();
                    var codeActionModel = JsonUtils.fromJson(JsonUtils.toJson(payload), CodeActionModel.class);

                    if (codeActionModel == null) {
                        return new JBCefJSQuery.Response("error");
                    }

                    insertAtCaret(codeActionModel.getContent());

                    TelemetryUtils.chatAccept(codeActionModel, ChatActionTypeEnum.INSERT);

                    return new JBCefJSQuery.Response("success");
                }
                case "ReplaceSelectedCode": {
                    var payload = jsCallModel.getPayload();
                    var codeActionModel = JsonUtils.fromJson(JsonUtils.toJson(payload), CodeActionModel.class);

                    if (codeActionModel == null) {
                        return new JBCefJSQuery.Response("error");
                    }

                    replaceSelectionCode(codeActionModel.getContent());

                    TelemetryUtils.chatAccept(codeActionModel, ChatActionTypeEnum.REPLACE);

                    return new JBCefJSQuery.Response("success");
                }
                case "CreateNewFile": {
                    var payload = jsCallModel.getPayload();
                    var codeActionModel = JsonUtils.fromJson(JsonUtils.toJson(payload), CodeActionModel.class);

                    if (codeActionModel == null) {
                        return new JBCefJSQuery.Response("error");
                    }

                    var userMessage = service.getUserMessage(codeActionModel.getMessageId());

                    if (userMessage == null) {
                        return new JBCefJSQuery.Response("error");
                    }

                    ApplicationManager.getApplication().invokeLater(
                            () -> NewFileUtils.createNewFile(project, codeActionModel.getContent(),
                                    userMessage.getCodeRef(), codeActionModel.getLang()));

                    TelemetryUtils.chatAccept(codeActionModel, ChatActionTypeEnum.NEW_FILE);

                    return new JBCefJSQuery.Response("success");
                }
                case "GotoSelectedCode": {
                    var payload = jsCallModel.getPayload();
                    var codeReferenceModel = JsonUtils.fromJson(JsonUtils.toJson(payload), CodeReferenceModel.class);

                    if (codeReferenceModel == null) {
                        return new JBCefJSQuery.Response("error");
                    }

                    ApplicationManager.getApplication().invokeLater(
                            () -> EditorUtils.openFileAndSelectLines(project, codeReferenceModel.getFileUrl(),
                                    codeReferenceModel.getSelectedStartLine(), codeReferenceModel.getSelectedStartColumn(),
                                    codeReferenceModel.getSelectedEndLine(), codeReferenceModel.getSelectedEndColumn()));

                    return new JBCefJSQuery.Response("success");
                }
                case "DeleteMessage": {
                    var payload = jsCallModel.getPayload();
                    var messageModel = JsonUtils.fromJson(JsonUtils.toJson(payload), MessageModel.class);
                    if (messageModel == null || messageModel.getId() == null) {
                        return new JBCefJSQuery.Response("error");
                    }
                    service.deleteMessage(messageModel.getId());
                    return new JBCefJSQuery.Response("success");
                }
                case "RegenerateMessage": {
                    service.regenerateMessage();
                    return new JBCefJSQuery.Response("success");
                }
                case "FixCode":
                case "CommentCode":
                case "ExplainCode":
                case "TestCode": {
                    var payload = jsCallModel.getPayload();
                    var messageModel = JsonUtils.fromJson(JsonUtils.toJson(payload), MessageModel.class);
                    if (messageModel == null) {
                        return new JBCefJSQuery.Response("error");
                    }

                    service.handleActions(messageModel.getCodeRef(), codeActionMap.get(command), null);
                    return new JBCefJSQuery.Response("success");
                }
                case "CopyCode": {
                    var payload = jsCallModel.getPayload();
                    var codeActionModel = JsonUtils.fromJson(JsonUtils.toJson(payload), CodeActionModel.class);

                    if (codeActionModel == null || codeActionModel.getContent() == null) {
                        return new JBCefJSQuery.Response("error");
                    }

                    var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(new StringSelection(codeActionModel.getContent()), null);

                    TelemetryUtils.chatAccept(codeActionModel, ChatActionTypeEnum.COPY);

                    return new JBCefJSQuery.Response("success");
                }
                case "OpenFile": {
                    var payload = jsCallModel.getPayload();
                    var codeActionModel = JsonUtils.fromJson(JsonUtils.toJson(payload), CodeActionModel.class);

                    if (codeActionModel == null || codeActionModel.getContent() == null) {
                        return new JBCefJSQuery.Response("error");
                    }

                    String relativePath = codeActionModel.getContent();
                    String repo = codeActionModel.getRepo();
                    ApplicationManager.getApplication().invokeLater(
                            () -> EditorUtils.openFileByRelativePath(repo, project, relativePath));

                    return new JBCefJSQuery.Response("success");
                }
                case "Login": {
                    LoginUtils.gotoLogin();
                    return new JBCefJSQuery.Response("success");
                }
                case "DislikeMessage":
                case "LikeMessage": {
                    var payload = jsCallModel.getPayload();
                    var messageModel = JsonUtils.fromJson(JsonUtils.toJson(payload), MessageModel.class);
                    if (messageModel == null || messageModel.getId() == null) {
                        return new JBCefJSQuery.Response("error");
                    }

                    var id = messageModel.getId();
                    var action = !command.equals("DislikeMessage");

                    TelemetryUtils.messageFeedback(id, action);
                    return new JBCefJSQuery.Response("success");
                }
                default:
                    return new JBCefJSQuery.Response("success");
            }
        });

        browser.getJBCefClient().addLoadHandler(new CefLoadHandler() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {

            }

            @Override
            public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
                browser.executeJavaScript(
                        "window.sendToIntelliJ = function(query) { " + myQuery.inject("query") + "};",
                        null,
                        0
                );

                var format = "window.intellijConfig = {theme: '%s', locale: '%s', username: '%s', loggedIn: %s, env: '%s', version: '%s', platform: '%s'};";
                var configModel = ConfigChangeUtils.configInit();
                var code = String.format(format, configModel.getTheme(),
                        configModel.getLocale(), configModel.getUsername(), configModel.isLoggedIn(),
                        configModel.getEnv(), configModel.getVersion(), configModel.getPlatform());

                browser.executeJavaScript(code, null, 0);
            }

            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {

            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {

            }
        }, browser.getCefBrowser());

    }

    private void registerLifeSpanHandler(JBCefBrowser browser) {
        final CefLifeSpanHandlerAdapter lifeSpanHandlerAdapter = new CefLifeSpanHandlerAdapter() {
            @Override
            public void onAfterCreated(CefBrowser browse) {
                CefApp.getInstance().registerSchemeHandlerFactory("http", "devpilot", new DevPilotCustomHandlerFactory());
            }
        };
        browser.getJBCefClient().addLifeSpanHandler(lifeSpanHandlerAdapter, browser.getCefBrowser());
    }

    public JComponent getDevPilotChatToolWindowPanel() {
        if (jbCefBrowser() != null) {
            return jbCefBrowser().getComponent();
        }
        return null;
    }

    private void replaceSelectionCode(String generatedText) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Editor textEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (textEditor == null || !textEditor.getSelectionModel().hasSelection()) {
                return;
            }

            WriteCommandAction.runWriteCommandAction(project, () -> {
                textEditor.getDocument().replaceString(textEditor.getSelectionModel().getSelectionStart(),
                        textEditor.getSelectionModel().getSelectionEnd(), generatedText);

                PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(textEditor.getDocument());
                if (psiFile != null) {
                    CodeStyleManager.getInstance(project).reformatText(psiFile, textEditor.getSelectionModel().getSelectionStart(),
                            textEditor.getSelectionModel().getSelectionEnd());
                }

                textEditor.getContentComponent().requestFocusInWindow();
            });
        });
    }

    private void insertAtCaret(String generatedText) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Editor textEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (textEditor == null) {
                return;
            }
            int offset = textEditor.getCaretModel().getOffset();

            WriteCommandAction.runWriteCommandAction(project, () -> {
                textEditor.getDocument().insertString(offset, generatedText);

                PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(textEditor.getDocument());
                if (psiFile != null) {
                    PsiDocumentManager.getInstance(project).commitDocument(textEditor.getDocument());
                    CodeStyleManager.getInstance(project).reformatText(psiFile, offset, offset + generatedText.length());
                }
            });
        });
    }
}
