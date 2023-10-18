package com.zhongan.codeai.gui.toolwindows.components.code;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.zhongan.codeai.CodeAIIcons;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

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
                JBUI.Borders.empty(8, 8, 8, 1)));
        add(new JBLabel(language), BorderLayout.LINE_START);
        add(createCodeActionsGroup(), BorderLayout.LINE_END);
    }

    private JPanel createCodeActionsGroup() {
        JPanel codeActionsGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        codeActionsGroup.add(new IconJButton(CodeAIIcons.COPY_ICON, "Copy", new CopyAction(codeEditor)));
        codeActionsGroup.add(new IconJButton(CodeAIIcons.INSERT_AT_CARET_ICON, "Insert at Caret", new InsertAtCaretAction(codeEditor, project)));
        return codeActionsGroup;
    }

}
