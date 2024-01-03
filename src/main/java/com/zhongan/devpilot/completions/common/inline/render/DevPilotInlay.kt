package com.zhongan.devpilot.completions.common.inline.render

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.zhongan.devpilot.completions.common.prediction.DevPilotCompletion
import java.awt.Rectangle

interface DevPilotInlay : Disposable {
    val offset: Int?
    val isEmpty: Boolean

    fun getBounds(): Rectangle?
    fun render(editor: Editor, completion: DevPilotCompletion, offset: Int)

    companion object {
        @JvmStatic
        fun create(parent: Disposable): DevPilotInlay {
            return DefaultDevPilotInlay(parent)
        }
    }
}
