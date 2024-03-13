//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.inplace.InplaceRefactoring;
import com.zhongan.devpilot.completions.inline.render.DevPilotInlay;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompletionPreview implements Disposable {
    private static final Key<CompletionPreview> INLINE_COMPLETION_PREVIEW = Key.create("INLINE_COMPLETION_PREVIEW");
    public final Editor editor;
    private final int offset;
    private DevPilotInlay devPilotInlay;
    private List<DevPilotCompletion> completions;
    private int currentIndex;

    private CompletionPreview(@NotNull Editor editor, List<DevPilotCompletion> completions, int offset) {

        super();
        this.currentIndex = 0;
        this.editor = editor;
        this.completions = completions;
        this.offset = offset;
        EditorUtil.disposeWithEditor(editor, this);
        this.devPilotInlay = DevPilotInlay.create(this);
    }

    public static DevPilotCompletion createInstance(Editor editor, List<DevPilotCompletion> completions, int offset) {
        CompletionPreview preview = getInstance(editor);
        if (preview != null) {
            Disposer.dispose(preview);
        }

        preview = new CompletionPreview(editor, completions, offset);
        editor.putUserData(INLINE_COMPLETION_PREVIEW, preview);
        return preview.createPreview();
    }

    public static @Nullable DevPilotCompletion getCurrentCompletion(Editor editor) {
        CompletionPreview preview = getInstance(editor);
        return preview == null ? null : preview.getCurrentCompletion();
    }

    public static @Nullable CompletionPreview getInstance(@NotNull Editor editor) {

        return (CompletionPreview)editor.getUserData(INLINE_COMPLETION_PREVIEW);
    }

    public static void clear(@NotNull Editor editor) {


        CompletionPreview completionPreview = getInstance(editor);
        if (completionPreview != null) {
            Disposer.dispose(completionPreview);
        }

    }

    public DevPilotCompletion getCurrentCompletion() {
        return (DevPilotCompletion)this.completions.get(this.currentIndex);
    }

    private DevPilotCompletion createPreview() {
        DevPilotCompletion completion = (DevPilotCompletion)this.completions.get(this.currentIndex);
        if (this.editor instanceof EditorImpl && !this.editor.getSelectionModel().hasSelection() && InplaceRefactoring.getActiveInplaceRenamer(this.editor) == null) {
            DevPilotCompletion var2;
            try {
                this.editor.getDocument().startGuardedBlockChecking();
                this.devPilotInlay.render(this.editor, completion, this.offset);
                var2 = completion;
            } finally {
                this.editor.getDocument().stopGuardedBlockChecking();
            }

            return var2;
        } else {
            return null;
        }
    }

    public void dispose() {
        this.editor.putUserData(INLINE_COMPLETION_PREVIEW, (CompletionPreview) null);
    }

    public void applyPreview(@Nullable Caret caret) {
        if (caret != null) {
            Project project = this.editor.getProject();
            if (project != null) {
                PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(this.editor.getDocument());
                if (file != null) {
                    try {
                        this.applyPreviewInternal(caret.getOffset(), project, file);
                    } catch (Throwable var8) {
                        Logger.getInstance(this.getClass()).warn("Failed in the processes of accepting completion", var8);
                    } finally {
                        Disposer.dispose(this);
                    }

                }
            }
        }
    }

    private void applyPreviewInternal(@NotNull Integer cursorOffset, Project project, PsiFile file) {

        clear(this.editor);
        DevPilotCompletion completion = (DevPilotCompletion)this.completions.get(this.currentIndex);
        String suffix = completion.getSuffix();
        int startOffset = cursorOffset - completion.oldPrefix.length();
        int endOffset = cursorOffset + suffix.length();
        if (CompletionPreviewUtils.shouldRemoveSuffix(completion)) {
            this.editor.getDocument().deleteString(cursorOffset, cursorOffset + completion.oldSuffix.length());
        }

        this.editor.getDocument().insertString(cursorOffset, suffix);
        this.editor.getCaretModel().moveToOffset(startOffset + completion.newPrefix.length());
    }
}
