package com.zhongan.devpilot.gui.toolwindows.components;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.zhongan.devpilot.gui.toolwindows.components.code.IconJButton;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

public class RoundedPanel extends JPanel {

    private final int radius;

    public RoundedPanel() {
        this(new BorderLayout(), 10);
    }

    public RoundedPanel(LayoutManager layout, int radius) {
        super(layout);
        this.radius = radius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g2.dispose();
    }

    public RoundedPanel addIconJButton(IconJButton iconJButton) {
        this.setPreferredSize(new Dimension((int) (iconJButton.getPreferredSize().getWidth() + 8),
                                            (int) (iconJButton.getPreferredSize().getHeight() + 8)));

        iconJButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(new JBColor(Gray._225, Gray._95));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(new JBColor(Gray._242, Gray._61));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                setBackground(new JBColor(Gray._235, Gray._110));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setBackground(new JBColor(Gray._225, Gray._95));
            }
        });
        add(iconJButton, BorderLayout.CENTER);
        return this;
    }

}
