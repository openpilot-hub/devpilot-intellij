package com.zhongan.codeai.gui.toolwindows.components.code;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;

public class IconJButton extends JButton {

    public IconJButton(Icon icon, String tipText, ActionListener actionListener) {
        super(icon);
        setToolTipText(tipText);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
        addActionListener(actionListener);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

}
