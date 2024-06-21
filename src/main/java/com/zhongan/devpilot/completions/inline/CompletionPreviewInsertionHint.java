package com.zhongan.devpilot.completions.inline;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.util.Disposer;
import com.zhongan.devpilot.completions.inline.render.DevPilotInlay;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.SwingUtilities;

public class CompletionPreviewInsertionHint implements Disposable, EditorMouseMotionListener {
    private Editor editor;

    private DevPilotInlay inlay;

    private String suffix;

    public CompletionPreviewInsertionHint(Editor editor, DevPilotInlay inlay, String suffix) {
        this.editor = editor;
        this.inlay = inlay;
        this.suffix = suffix;
        editor.addEditorMouseMotionListener(this);
        Disposer.register(inlay, this);
    }

    @Override
    public void mouseMoved(EditorMouseEvent e) {
        if (inlay.isEmpty() || e.getArea() != EditorMouseEventArea.EDITING_AREA) {
            return;
        }

        java.awt.event.MouseEvent mouseEvent = e.getMouseEvent();
        Point point = mouseEvent.getPoint();

        if (!isOverPreview(point)) {
            return;
        }

        InlineKeybindingHintUtil.createAndShowHint(
            editor,
            SwingUtilities.convertPoint(
                (Component) mouseEvent.getSource(),
                point,
                editor.getComponent().getRootPane().getLayeredPane()
            )
        );
    }

    @Override
    public void dispose() {
        editor.removeEditorMouseMotionListener(this);
    }

    private boolean isOverPreview(Point p) {
        Rectangle bounds = inlay.getBounds();
        if (bounds != null) {
            return bounds.contains(p);
        } else {
            return isLogicallyInsideInlay(p);
        }
    }

    private boolean isLogicallyInsideInlay(Point p) {
        LogicalPosition pos = editor.xyToLogicalPosition(p);

        if (pos.line >= editor.getDocument().getLineCount()) {
            return false;
        }

        int pointOffset = editor.logicalPositionToOffset(pos);
        Integer inlayOffset = inlay.getOffset();

        if (inlayOffset != null) {
            return pointOffset >= inlayOffset && pointOffset <= inlayOffset + suffix.length();
        }

        return false;
    }
}
