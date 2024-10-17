package com.zhongan.devpilot.completions.inline;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.inplace.InplaceRefactoring;
import com.zhongan.devpilot.completions.autoimport.handler.AutoImportHandler;
import com.zhongan.devpilot.completions.inline.listeners.InlineCaretListener;
import com.zhongan.devpilot.completions.inline.render.DevPilotInlay;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;
import com.zhongan.devpilot.treesitter.TreeSitterParser;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.TelemetryUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.zhongan.devpilot.completions.CompletionUtils.createSimpleDevpilotCompletion;
import static com.zhongan.devpilot.completions.inline.CompletionPreviewUtils.shouldRemoveSuffix;

public class CompletionPreview implements Disposable {
    private static final Key<CompletionPreview> INLINE_COMPLETION_PREVIEW =
            Key.create("INLINE_COMPLETION_PREVIEW");

    public final Editor editor;

    private final int offset;

    private DevPilotInlay devPilotInlay;

    private List<DevPilotCompletion> completions;

    private int currentIndex = 0;

    private InlineCaretListener inlineCaretListener;

    private CompletionPreview(
            @NotNull Editor editor, List<DevPilotCompletion> completions, int offset) {
        this.editor = editor;
        this.completions = completions;
        this.offset = offset;
        EditorUtil.disposeWithEditor(editor, this);

        this.inlineCaretListener = new InlineCaretListener(this);

        devPilotInlay = DevPilotInlay.create(this);
    }

    public static synchronized DevPilotCompletion createInstance(
        Editor editor, List<DevPilotCompletion> completions, int offset) {
        CompletionPreview preview = getInstance(editor);

        if (preview != null) {
            Disposer.dispose(preview);
        }

        preview = new CompletionPreview(editor, completions, offset);

        editor.putUserData(INLINE_COMPLETION_PREVIEW, preview);

        return preview.createPreview();
    }

    @Nullable
    public static DevPilotCompletion getCurrentCompletion(Editor editor) {
        CompletionPreview preview = getInstance(editor);
        if (preview == null) return null;

        return preview.getCurrentCompletion();
    }

    @Nullable
    public static CompletionPreview getInstance(@NotNull Editor editor) {
        return editor.getUserData(INLINE_COMPLETION_PREVIEW);
    }

    public static void clear(@NotNull Editor editor) {
        CompletionPreview completionPreview = getInstance(editor);
        if (completionPreview != null) {
            Disposer.dispose(completionPreview);
        }
    }

/*  public void togglePreview(CompletionOrder order) {
    int nextIndex = currentIndex + order.diff();
    currentIndex = (completions.size() + nextIndex) % completions.size();

    Disposer.dispose(devPilotInlay);
    devPilotInlay = DevPilotInlay.create(this);

    createPreview();
  }*/

    public DevPilotCompletion getCurrentCompletion() {
        return completions.get(currentIndex);
    }

    private DevPilotCompletion createPreview() {
        DevPilotCompletion completion = completions.get(currentIndex);

        if (!(editor instanceof EditorImpl)
                || editor.getSelectionModel().hasSelection()
                || InplaceRefactoring.getActiveInplaceRenamer(editor) != null) {
            return null;
        }

        try {
            editor.getDocument().startGuardedBlockChecking();
            devPilotInlay.render(this.editor, completion, offset);
            return completion;
        } finally {
            editor.getDocument().stopGuardedBlockChecking();
        }
    }

    public void dispose() {
        editor.putUserData(INLINE_COMPLETION_PREVIEW, null);
    }

    public void applyPreview(@Nullable Caret caret) {
        if (caret == null) {
            return;
        }

        Project project = editor.getProject();

        if (project == null) {
            return;
        }

        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

        if (file == null) {
            return;
        }

        try {
            applyPreviewInternal(caret.getOffset(), project, file);
        } catch (Throwable e) {
            Logger.getInstance(getClass()).warn("Failed in the processes of accepting completion", e);
        } finally {
            Disposer.dispose(this);
        }
    }

    private void applyPreviewInternal(@NotNull Integer cursorOffset, Project project, PsiFile file) {
        CompletionPreview.clear(editor);
        DevPilotCompletion completion = completions.get(currentIndex);
        String suffix = completion.getSuffix();
        int startOffset = cursorOffset - completion.oldPrefix.length();
        int endOffset = cursorOffset + suffix.length();

        if (shouldRemoveSuffix(completion)) {
            editor.getDocument().deleteString(cursorOffset, cursorOffset + completion.oldSuffix.length());
        }

        var name = file.getName();
        var fileExtension = name.substring(name.lastIndexOf(".") + 1);
        if (!suffix.contains("\n")) {
            suffix = TreeSitterParser.getInstance(fileExtension)
                    .parse(editor.getDocument().getText(), cursorOffset, suffix);
        }

        editor.getDocument().insertString(cursorOffset, suffix);
        editor.getCaretModel().moveToOffset(startOffset + suffix.length());

        PsiDocumentManager.getInstance(project).commitAllDocuments();

        PsiFile fileAfterCompletion = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            getAutoImportHandler(editor, fileAfterCompletion, startOffset, endOffset).invoke();
        });

        TelemetryUtils.completionAccept(completion.id, file, completion.getUnacceptedLines());
    }

    public void applyPreviewByLine(@Nullable Caret caret) {
        if (caret == null) {
            return;
        }

        Project project = editor.getProject();

        if (project == null) {
            return;
        }

        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

        if (file == null) {
            return;
        }

        try {
            applyPreviewInternalByLine(project, file);
        } catch (Throwable e) {
            Logger.getInstance(getClass()).warn("Failed in the processes of accepting completion by line", e);
        } finally {
            Disposer.dispose(this);
        }
    }

    private void applyPreviewInternalByLine(Project project, PsiFile file) {
        DevPilotCompletion completion = completions.get(currentIndex);
        Document document = editor.getDocument();
        String line = completion.getNextUnacceptLineState().getLine();
        LogicalPosition currentPos = editor.getCaretModel().getLogicalPosition();
        int insertionOffset = editor.logicalPositionToOffset(currentPos);
        if (StringUtils.isEmpty(line)) {
            completion.acceptLine(insertionOffset);
            line = completion.getNextUnacceptLineState().getLine();
        }
        if (completion.getLineStateItems().getIndex() > 0) {
            insertionOffset += "\n".length();  // can't remove
        }
        completion.acceptLine(insertionOffset + line.length());
        document.insertString(insertionOffset, line + "\n");  // offset don't change
        editor.getCaretModel().moveToOffset(insertionOffset + line.length());
        Objects.requireNonNull(CompletionPreview.getInstance(editor)).continuePreview();
        PsiFile fileAfterCompletion = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        int startOffset = insertionOffset - completion.oldPrefix.length();
        int endOffset = insertionOffset + line.length();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            getAutoImportHandler(editor, fileAfterCompletion, startOffset, endOffset).invoke();
        });
        TelemetryUtils.completionAccept(completion.id, file, line);
    }

    public void continuePreview() {
        DevPilotCompletion completion = completions.get(currentIndex);
        if (!(editor instanceof EditorImpl)
                || editor.getSelectionModel().hasSelection()
                || InplaceRefactoring.getActiveInplaceRenamer(editor) != null) {
            return;
        }

        try {
            editor.getDocument().startGuardedBlockChecking();
            DevPilotCompletion simpleDevpilotCompletion = createSimpleDevpilotCompletion(editor, editor.getCaretModel().getOffset(),
                    "",
                    completion.getLineStateItems().getUnacceptedLines(),
                    UUID.randomUUID().toString(), editor.getDocument());
            CompletionPreview.clear(editor);
            CompletionPreview.createInstance(editor, Collections.singletonList(simpleDevpilotCompletion), editor.getCaretModel().getOffset());
        } finally {
            editor.getDocument().stopGuardedBlockChecking();
        }
    }

    public boolean isByLineAcceptCaretChange(CaretEvent caretEvent) {
        int newOffset = editor.logicalPositionToOffset(caretEvent.getNewPosition());
        DevPilotCompletion completion = completions.get(currentIndex);
        int currentCompletionPosition = completion.getCurrentCompletionPosition();
        return newOffset == currentCompletionPosition;
    }

    public boolean isByLineAcceptDocumentChange(DocumentEvent documentEvent) {
        int previousOffset = documentEvent.getOffset();
        int newOffset = previousOffset + documentEvent.getNewLength();
        if (newOffset < 0 || previousOffset >= newOffset) return false; // previousOffset == newOffset   ctr + z
        String addedText = editor.getDocument().getText(new TextRange(previousOffset, newOffset));

        DevPilotCompletion completion = completions.get(currentIndex);
        String completionCode = completion.getCurrentCompletionCode();
        return StringUtils.equals(addedText, completionCode);
    }

    private static AutoImportHandler getAutoImportHandler(Editor editor, PsiFile file, int startOffset, int endOffset) {
        return new AutoImportHandler(startOffset, endOffset, editor, file);
    }

    public static String byLineAcceptHintText() {
        String acceptShortcut = getByLineAcceptShortcutText();
        return String.format("%s %s", acceptShortcut, DevPilotMessageBundle.get("completion.apply.partial.tooltips"));
    }

    private static String getByLineAcceptShortcutText() {
        return StringUtil.defaultIfEmpty(
                KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(AcceptDevPilotInlineCompletionByLineAction.ACTION_ID)),
                "Missing shortcut key");
    }
}
