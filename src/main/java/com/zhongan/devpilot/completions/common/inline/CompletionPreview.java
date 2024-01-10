package com.zhongan.devpilot.completions.common.inline;

import static com.zhongan.devpilot.completions.common.inline.CompletionPreviewUtilsKt.*;

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
import com.zhongan.devpilot.completions.common.inline.render.DevPilotInlay;
import com.zhongan.devpilot.completions.common.prediction.DevPilotCompletion;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompletionPreview implements Disposable {
  private static final Key<CompletionPreview> INLINE_COMPLETION_PREVIEW =
      Key.create("INLINE_COMPLETION_PREVIEW");

  public final Editor editor;
  private DevPilotInlay devPilotInlay;
  private List<DevPilotCompletion> completions;
  private final int offset;
  private int currentIndex = 0;

  private CompletionPreview(
          @NotNull Editor editor, List<DevPilotCompletion> completions, int offset) {
    this.editor = editor;
    this.completions = completions;
    this.offset = offset;
    EditorUtil.disposeWithEditor(editor, this);

    devPilotInlay = DevPilotInlay.create(this);
  }

  public static DevPilotCompletion createInstance(
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

    editor.getDocument().insertString(cursorOffset, suffix);
    editor.getCaretModel().moveToOffset(startOffset + completion.newPrefix.length());
    //TODO 接受率异步上报
  }
}
