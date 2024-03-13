//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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
import java.awt.event.MouseEvent;
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

    public void mouseMoved(EditorMouseEvent e) {
        if (!this.inlay.isEmpty() && e.getArea() == EditorMouseEventArea.EDITING_AREA) {
            MouseEvent mouseEvent = e.getMouseEvent();
            Point point = mouseEvent.getPoint();
            if (this.isOverPreview(point)) {
                InlineKeybindingHintUtil.createAndShowHint(this.editor, SwingUtilities.convertPoint((Component)mouseEvent.getSource(), point, this.editor.getComponent().getRootPane().getLayeredPane()));
            }
        }
    }

    public void dispose() {
        this.editor.removeEditorMouseMotionListener(this);
    }

    private boolean isOverPreview(Point p) {
        Rectangle bounds = this.inlay.getBounds();
        return bounds != null ? bounds.contains(p) : this.isLogicallyInsideInlay(p);
    }

    private boolean isLogicallyInsideInlay(Point p) {
        LogicalPosition pos = this.editor.xyToLogicalPosition(p);
        if (pos.line >= this.editor.getDocument().getLineCount()) {
            return false;
        } else {
            int pointOffset = this.editor.logicalPositionToOffset(pos);
            Integer inlayOffset = this.inlay.getOffset();
            if (inlayOffset == null) {
                return false;
            } else {
                return pointOffset >= inlayOffset && pointOffset <= inlayOffset + this.suffix.length();
            }
        }
    }
}
