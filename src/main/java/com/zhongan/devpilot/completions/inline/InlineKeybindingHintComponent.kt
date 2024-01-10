package com.zhongan.devpilot.completions.inline

import com.intellij.ui.SimpleColoredComponent
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

internal class InlineKeybindingHintComponent(component: SimpleColoredComponent) : JPanel(BorderLayout()) {
    init {
        border = JBUI.Borders.empty()
        add(component, BorderLayout.CENTER)
        isOpaque = true
        background = component.background
        revalidate()
        repaint()
    }
}
