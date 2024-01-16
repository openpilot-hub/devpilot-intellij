package com.zhongan.devpilot.completions.inline;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.util.ui.JBUI;

import java.awt.BorderLayout;

import javax.swing.JPanel;

public class InlineKeybindingHintComponent extends JPanel {
    public InlineKeybindingHintComponent(SimpleColoredComponent component) {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty());
        add(component, BorderLayout.CENTER);
        setOpaque(true);
        setBackground(component.getBackground());
        revalidate();
        repaint();
    }
}