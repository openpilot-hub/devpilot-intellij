//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompletionFacade {
    public CompletionFacade() {
    }

    public static @Nullable String getFilename(@Nullable VirtualFile file) {
        return (String)ObjectUtils.doIfNotNull(file, VirtualFile::getPath);
    }

    public @Nullable AutocompleteResponse retrieveCompletions(@NotNull Editor editor, int offset, @Nullable Integer tabSize, @Nullable CompletionAdjustment completionAdjustment) {

        try {
            String filename = getFilename(FileDocumentManager.getInstance().getFile(editor.getDocument()));
            return this.retrieveCompletions(editor, offset, filename, tabSize, completionAdjustment);
        } catch (Exception var6) {
            return null;
        }
    }

    private @Nullable AutocompleteResponse retrieveCompletions(@NotNull Editor editor, int offset, @Nullable String filename, @Nullable Integer tabSize, @Nullable CompletionAdjustment completionAdjustment) {


        Document document = editor.getDocument();
        int begin = Integer.max(0, offset - 200);
        int end = Integer.min(document.getTextLength(), offset + 200);
        AutocompleteRequest req = new AutocompleteRequest();
        req.before = document.getText(new TextRange(begin, offset));
        req.after = document.getText(new TextRange(offset, end));
        req.filename = filename;
        req.maxResults = 5;
        req.regionIncludesBeginning = begin == 0;
        req.regionIncludesEnd = end == document.getTextLength();
        req.offset = offset;
        req.line = document.getLineNumber(offset);
        req.character = offset - document.getLineStartOffset(req.line);
        req.indentationSize = tabSize;
        req.sdkPath = this.getSdkPath(editor);
        if (completionAdjustment != null) {
            completionAdjustment.adjustRequest(req);
        }

        DevPilotMessage devPilotMessage = new DevPilotMessage();
        devPilotMessage.setRole("user");
        String var10000 = EditorActionEnum.CODE_COMPLETIONS.getPrompt().replace("{{offsetCode}}", document.getText(new TextRange(req.before.lastIndexOf("\n"), offset)));
        String var10002 = this.getFileExtension(editor);
        String content = var10000.replace("{{selectedCode}}", var10002 + " " + req.before).replace("{{maxCompletionLength}}", "200");
        devPilotMessage.setContent(content);
        devPilotMessage.setContent(req.before);
        DevPilotInstructCompletionRequest request = new DevPilotInstructCompletionRequest();
        request.setPrompt(req.before);
        request.getMessages().add(devPilotMessage);
        request.setSuffix(req.after);
        String response = (new LlmProviderFactory()).getLlmProvider(editor.getProject()).instructCompletion(request);
        if (response == null) {
            return null;
        } else {
            AutocompleteResponse autocompleteResponse = new AutocompleteResponse();
            autocompleteResponse.oldPrefix = "";
            autocompleteResponse.userMessage = new String[0];
            ResultEntry resultEntry = new ResultEntry();
            resultEntry.newPrefix = response;
            resultEntry.oldSuffix = "";
            resultEntry.newSuffix = "";
            ResultEntry[] resultEntries = new ResultEntry[]{resultEntry};
            autocompleteResponse.results = resultEntries;
            if (completionAdjustment != null) {
                completionAdjustment.adjustResponse(autocompleteResponse);
            }

            return autocompleteResponse;
        }
    }

    private String getSdkPath(Editor editor) {
        if (editor == null) {
            return null;
        } else {
            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
            if (virtualFile != null && virtualFile.getExtension() != null && virtualFile.getExtension().equals("java")) {
                if (editor.getProject() == null) {
                    return null;
                } else {
                    ProjectRootManager rootManager = ProjectRootManager.getInstance(editor.getProject());
                    if (rootManager == null) {
                        return null;
                    } else {
                        Sdk sdk = rootManager.getProjectSdk();
                        if (sdk == null) {
                            return null;
                        } else if (!sdk.getSdkType().isLocalSdk(sdk)) {
                            return null;
                        } else {
                            String homePath = sdk.getHomePath();
                            return !this.isValidJavaHome(homePath) ? null : homePath;
                        }
                    }
                }
            } else {
                return null;
            }
        }
    }

    private boolean isValidJavaHome(String homePath) {
        Path path = Paths.get(homePath, "bin", "java");
        return Files.exists(path, new LinkOption[0]);
    }

    private String getFileExtension(Editor editor) {
        if (editor == null) {
            return null;
        } else {
            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
            return virtualFile != null && virtualFile.getExtension() != null ? virtualFile.getExtension() : null;
        }
    }
}
