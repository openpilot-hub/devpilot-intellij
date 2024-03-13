//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.util.ui.JBUI.Borders;
import java.awt.BorderLayout;
import javax.swing.JPanel;

public class InlineKeybindingHintComponent extends JPanel {
    public InlineKeybindingHintComponent(SimpleColoredComponent component) {
        this.setLayout(new BorderLayout());
        this.setBorder(Borders.empty());
        this.add(component, "Center");
        this.setOpaque(true);
        this.setBackground(component.getBackground());
        this.revalidate();
        this.repaint();
    }
}
