package com.zhongan.devpilot.completions.common.inline

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.util.Disposer
import com.zhongan.devpilot.completions.common.inline.render.DevPilotInlay
import java.awt.Component
import java.awt.Point
import javax.swing.SwingUtilities

class CompletionPreviewInsertionHint(
        private val editor: Editor,
        private val inlay: DevPilotInlay,
        private var suffix: String = ""
) : Disposable,
        EditorMouseMotionListener {
    init {
        editor.addEditorMouseMotionListener(this)
        Disposer.register(inlay, this)
    }

    override fun mouseMoved(e: EditorMouseEvent) {
        if (inlay.isEmpty || e.area !== EditorMouseEventArea.EDITING_AREA) {
            return
        }

        val mouseEvent = e.mouseEvent
        val point = mouseEvent.point

        if (!isOverPreview(point)) {
            return
        }

        InlineKeybindingHintUtil.createAndShowHint(
                editor,
                SwingUtilities.convertPoint(
                        mouseEvent.source as Component,
                        point,
                        editor.component.rootPane.layeredPane
                )
        )
    }

    override fun dispose() {
        editor.removeEditorMouseMotionListener(this)
    }

    private fun isOverPreview(p: Point): Boolean {
        return inlay.getBounds()?.contains(p) ?: isLogicallyInsideInlay(p)
    }

    private fun isLogicallyInsideInlay(p: Point): Boolean {
        val pos: LogicalPosition = editor.xyToLogicalPosition(p)

        if (pos.line >= editor.document.lineCount) {
            return false
        }

        val pointOffset = editor.logicalPositionToOffset(pos)
        val inlayOffset = inlay.offset ?: return false

        return pointOffset >= inlayOffset && pointOffset <= inlayOffset + suffix.length
    }
}
