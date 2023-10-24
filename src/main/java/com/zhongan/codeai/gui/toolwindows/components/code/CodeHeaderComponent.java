package com.zhongan.codeai.gui.toolwindows.components.code;

import com.intellij.collaboration.ui.codereview.comment.RoundedPanel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.zhongan.codeai.CodeAIIcons;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

public class CodeHeaderComponent extends JPanel {

    private final Editor codeEditor;

    private final Project project;

    private final String fileExtension;

    public CodeHeaderComponent(
            String language,
            Editor editor,
            Project project,
            String fileExtension
    ) {
        super(new BorderLayout());
        this.codeEditor = editor;
        this.project = project;
        this.fileExtension = fileExtension;
        setBorder(JBUI.Borders.compound(JBUI.Borders.customLine(JBColor.border(), 1, 1, 1, 1),
                JBUI.Borders.empty(5)));
        add(new JBLabel(language), BorderLayout.LINE_START);
        add(createCodeActionsGroup(), BorderLayout.LINE_END);
    }

    private JPanel createCodeActionsGroup() {
        JPanel codeActionsGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));

        codeActionsGroup.add(withBackground(new IconJButton(CodeAIIcons.COPY_ICON, "Copy",
                new CopyAction(codeEditor))));
        codeActionsGroup.add(withBackground(new IconJButton(CodeAIIcons.INSERT_AT_CARET_ICON, "Insert at Caret",
                new InsertAtCaretAction(codeEditor, project))));
        return codeActionsGroup;
    }

    private JPanel withBackground(IconJButton iconJButton) {
        RoundedPanel buttonBackground = new RoundedPanel(new BorderLayout(), 6);
        buttonBackground.setBorder(new RoundedLineBorder(buttonBackground.getBackground(), 8));

        buttonBackground.setPreferredSize(new Dimension((int) (iconJButton.getPreferredSize().getWidth() + 8),
                                                        (int) (iconJButton.getPreferredSize().getHeight() + 8)));
        Color defaultBackground = buttonBackground.getBackground();
        iconJButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                buttonBackground.setBackground(new JBColor(Gray._225, Gray._95));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                buttonBackground.setBackground(defaultBackground);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                buttonBackground.setBackground(new JBColor(Gray._235, Gray._110));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                buttonBackground.setBackground(new JBColor(Gray._225, Gray._95));
            }
        });

        buttonBackground.add(iconJButton, BorderLayout.CENTER);
        return buttonBackground;
    }

}
