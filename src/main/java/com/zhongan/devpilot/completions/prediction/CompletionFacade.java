package com.zhongan.devpilot.completions.prediction;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ObjectUtils;
import com.zhongan.devpilot.completions.inline.CompletionAdjustment;
import com.zhongan.devpilot.completions.requests.AutocompleteRequest;
import com.zhongan.devpilot.completions.requests.AutocompleteResponse;
import com.zhongan.devpilot.completions.requests.ResultEntry;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.integrations.llms.LlmProviderFactory;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotInstructCompletionRequest;
import com.zhongan.devpilot.integrations.llms.entity.DevPilotMessage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.zhongan.devpilot.completions.general.StaticConfig.MAX_COMPLETIONS;
import static com.zhongan.devpilot.completions.general.StaticConfig.MAX_INSTRUCT_COMPLETION_TOKENS;
import static com.zhongan.devpilot.completions.general.StaticConfig.MIN_CHAT_COMPLETION_MESSAGE_LENGTH;
import static com.zhongan.devpilot.completions.general.StaticConfig.MIN_OFFSET;

public class CompletionFacade {

    public CompletionFacade() {
    }

    @Nullable
    public static String getFilename(@Nullable VirtualFile file) {
        return ObjectUtils.doIfNotNull(file, VirtualFile::getPath);
    }

    @Nullable
    public AutocompleteResponse retrieveCompletions(
        @NotNull Editor editor,
        int offset,
        @Nullable Integer tabSize,
        @Nullable CompletionAdjustment completionAdjustment) {
        try {
            String filename =
                getFilename(FileDocumentManager.getInstance().getFile(editor.getDocument()));
            return retrieveCompletions(editor, offset, filename, tabSize, completionAdjustment);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private AutocompleteResponse retrieveCompletions(
        @NotNull Editor editor,
        int offset,
        @Nullable String filename,
        @Nullable Integer tabSize,
        @Nullable CompletionAdjustment completionAdjustment) {
        Document document = editor.getDocument();

        int begin = Integer.max(0, offset - MIN_OFFSET);
        int end = Integer.min(document.getTextLength(), offset + MIN_OFFSET);
        AutocompleteRequest req = new AutocompleteRequest();
        req.before = document.getText(new TextRange(begin, offset));
        req.after = document.getText(new TextRange(offset, end));
        req.filename = filename;
        req.maxResults = MAX_COMPLETIONS;
        req.regionIncludesBeginning = (begin == 0);
        req.regionIncludesEnd = (end == document.getTextLength());
        req.offset = offset;
        req.line = document.getLineNumber(offset);
        req.character = offset - document.getLineStartOffset(req.line);
        req.indentationSize = tabSize;
        req.sdkPath = getSdkPath(editor);

        if (completionAdjustment != null) {
            completionAdjustment.adjustRequest(req);
        }

        //Simulating request response. Calling OpenAI or returning a value.
        DevPilotMessage devPilotMessage = new DevPilotMessage();
        devPilotMessage.setRole("user");
        String content = EditorActionEnum.CODE_COMPLETIONS.getPrompt()
            .replace("{{offsetCode}}", document.getText(new TextRange(req.before.lastIndexOf("\n"), offset)))
            .replace("{{selectedCode}}", getFileExtension(editor) + " " + req.before)
            .replace("{{maxCompletionLength}}", MIN_CHAT_COMPLETION_MESSAGE_LENGTH);
        devPilotMessage.setContent(content);

        devPilotMessage.setContent(req.before);
        DevPilotInstructCompletionRequest request = new DevPilotInstructCompletionRequest();
        request.setPrompt(req.before);
        request.setSuffix(req.after);
        request.setMaxTokens(MAX_INSTRUCT_COMPLETION_TOKENS);

        final var response = new LlmProviderFactory().getLlmProvider(editor.getProject()).instructCompletion(request);
        if (response == null) {
            return null;
        }

        AutocompleteResponse autocompleteResponse = new AutocompleteResponse();
        autocompleteResponse.oldPrefix = "";
        autocompleteResponse.userMessage = new String[] {};
        ResultEntry resultEntry = new ResultEntry();
        resultEntry.id = response.getId();
        resultEntry.newPrefix = response.getContent();
        resultEntry.oldSuffix = "";
        resultEntry.newSuffix = "";
        ResultEntry[] resultEntries = new ResultEntry[] {resultEntry};
        autocompleteResponse.results = resultEntries;

        if (completionAdjustment != null) {
            completionAdjustment.adjustResponse(autocompleteResponse);
        }
        return autocompleteResponse;
    }

    private String getSdkPath(Editor editor) {
        if (editor == null) {
            return null;
        }

        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (virtualFile == null
            || virtualFile.getExtension() == null
            || !virtualFile.getExtension().equals("java")) {
            return null;
        }

        if (editor.getProject() == null) {
            return null;
        }

        ProjectRootManager rootManager = ProjectRootManager.getInstance(editor.getProject());
        if (rootManager == null) {
            return null;
        }

        Sdk sdk = rootManager.getProjectSdk();
        if (sdk == null) {
            return null;
        }

        if (!sdk.getSdkType().isLocalSdk(sdk)) {
            return null;
        }

        String homePath = sdk.getHomePath();

        if (!isValidJavaHome(homePath)) {
            return null;
        }

        return homePath;
    }

    private boolean isValidJavaHome(String homePath) {
        Path path = Paths.get(homePath, "bin", "java");
        return Files.exists(path);
    }

/*    private int determineTimeoutBy(@NotNull String before) {
        if (!suggestionsModeService.getSuggestionMode().isInlineEnabled()) {
            return COMPLETION_TIME_THRESHOLD;
        }

        int lastNewline = before.lastIndexOf("\n");
        String lastLine = lastNewline >= 0 ? before.substring(lastNewline) : "";
        boolean endsWithWhitespacesOnly = lastLine.trim().isEmpty();
        return endsWithWhitespacesOnly ? NEWLINE_COMPLETION_TIME_THRESHOLD : COMPLETION_TIME_THRESHOLD;
    }*/

    private String getFileExtension(Editor editor) {
        if (editor == null) {
            return null;
        }

        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (virtualFile == null || virtualFile.getExtension() == null) {
            return null;
        }

        return virtualFile.getExtension();
    }

}
