package com.zhongan.devpilot.util;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotInstructCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;

import java.util.HashMap;
import java.util.Map;

import static com.zhongan.devpilot.constant.DefaultConst.REQUEST_ENCODING_ON;
import static com.zhongan.devpilot.util.VirtualFileUtil.getRelativeFilePath;

public class GatewayRequestUtils {
    public static String completionRequestPureJson(DevPilotInstructCompletionRequest instructCompletionRequest) {
        int offset = instructCompletionRequest.getOffset();
        Editor editor = instructCompletionRequest.getEditor();
        final Document[] document = new Document[1];
        final Language[] language = new Language[1];
        final VirtualFile[] virtualFile = new VirtualFile[1];
        final String[] relativePath = new String[1];

        ApplicationManager.getApplication().runReadAction(() -> {
            document[0] = editor.getDocument();
            language[0] = PsiDocumentManager.getInstance(editor.getProject()).getPsiFile(document[0]).getLanguage();
            virtualFile[0] = FileDocumentManager.getInstance().getFile(document[0]);
            relativePath[0] = getRelativeFilePath(editor.getProject(), virtualFile[0]);
        });

        String text = document[0].getText();

        Map<String, Object> map = new HashMap<>();
        map.put("document", text);
        map.put("position", String.valueOf(offset));
        map.put("language", language[0].getID());
        map.put("filePath", relativePath[0]);
        map.put("completionType", instructCompletionRequest.getCompletionType());
        map.put("additionalContext", instructCompletionRequest.getRelatedCodeInfos());

        return JsonUtils.toJson(map);
    }

    public static String completionRequestJson(DevPilotInstructCompletionRequest instructCompletionRequest) {
        int offset = instructCompletionRequest.getOffset();
        Editor editor = instructCompletionRequest.getEditor();
        final Document[] document = new Document[1];
        final Language[] language = new Language[1];
        final VirtualFile[] virtualFile = new VirtualFile[1];
        final String[] relativePath = new String[1];

        ApplicationManager.getApplication().runReadAction(() -> {
            document[0] = editor.getDocument();
            language[0] = PsiDocumentManager.getInstance(editor.getProject()).getPsiFile(document[0]).getLanguage();
            virtualFile[0] = FileDocumentManager.getInstance().getFile(document[0]);
            relativePath[0] = getRelativeFilePath(editor.getProject(), virtualFile[0]);
        });

        String text = document[0].getText();

        if (isRequestEncoding()) {
            instructCompletionRequest.setEncoding("base64");
            text = Base64Utils.base64Encoding(text);
        }

        Map<String, String> map = new HashMap<>();
        map.put("document", text);
        map.put("position", String.valueOf(offset));
        map.put("language", language[0].getID());
        map.put("filePath", relativePath[0]);
        map.put("completionType", instructCompletionRequest.getCompletionType());
        map.put("encoding", instructCompletionRequest.getEncoding());

        return JsonUtils.toJson(map);
    }

    public static String chatRequestJson(DevPilotChatCompletionRequest chatCompletionRequest) {
        if (isRequestEncoding()) {
            chatCompletionRequest.setEncoding("base64");

            var messageList = chatCompletionRequest.getMessages();

            for (DevPilotMessage devPilotMessage : messageList) {
                if (devPilotMessage.getContent() != null) {
                    devPilotMessage.setContent(Base64Utils.base64Encoding(devPilotMessage.getContent()));
                }

                // avoid immutable map
                if (devPilotMessage.getPromptData() != null) {
                    var promptData = new HashMap<>(devPilotMessage.getPromptData());
                    for (Map.Entry<String, String> entry : promptData.entrySet()) {
                        entry.setValue(Base64Utils.base64Encoding(entry.getValue()));
                    }
                    devPilotMessage.setPromptData(promptData);
                }
            }
        }

        return JsonUtils.toJson(chatCompletionRequest);
    }

    private static boolean isRequestEncoding() {
        return REQUEST_ENCODING_ON;
    }
}
