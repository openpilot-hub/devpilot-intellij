package com.zhongan.devpilot.common.inline.render

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.zhongan.devpilot.common.prediction.DevPilotCompletion
import java.awt.Rectangle

interface TabnineInlay : Disposable {
    val offset: Int?
    val isEmpty: Boolean

    fun getBounds(): Rectangle?
    fun render(editor: Editor, completion: DevPilotCompletion, offset: Int)

    companion object {
        @JvmStatic
        fun create(parent: Disposable): TabnineInlay {
            return DefaultTabnineInlay(parent)
        }
    }
}
