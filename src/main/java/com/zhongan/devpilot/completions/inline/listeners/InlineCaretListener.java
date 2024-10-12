package com.zhongan.devpilot.completions.inline.listeners;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.util.Disposer;
import com.zhongan.devpilot.completions.inline.CompletionPreview;
import com.zhongan.devpilot.completions.inline.InlineCompletionCache;

import org.jetbrains.annotations.NotNull;

public class InlineCaretListener implements CaretListener, Disposable {
    private final CompletionPreview completionPreview;

    public InlineCaretListener(CompletionPreview completionPreview) {
        this.completionPreview = completionPreview;
        Disposer.register(completionPreview, this);
        completionPreview.editor.getCaretModel().addCaretListener(this);
    }

    @Override
    public void caretPositionChanged(@NotNull CaretEvent event) {
        if (isSingleOffsetChange(event)) {
            return;
        }

        if (completionPreview.isByLineAcceptCaretChange(event)) {
            return;
        }
        Disposer.dispose(completionPreview);
        InlineCompletionCache.clear(event.getEditor());
    }

    private boolean isSingleOffsetChange(CaretEvent event) {
        return event.getOldPosition().line == event.getNewPosition().line;
    }

    @Override
    public void dispose() {
        completionPreview.editor.getCaretModel().removeCaretListener(this);
    }
}
