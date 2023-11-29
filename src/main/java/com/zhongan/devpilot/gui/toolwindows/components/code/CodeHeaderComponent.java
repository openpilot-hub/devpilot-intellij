package com.zhongan.devpilot.gui.toolwindows.components.code;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.zhongan.devpilot.DevPilotIcons;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.gui.toolwindows.components.RoundedPanel;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JPanel;

public class CodeHeaderComponent extends JPanel {

    private final Editor codeEditor;

    private final Project project;

    private final String fileExtension;

    private final EditorActionEnum editorActionEnum;

    private final Editor chosenEditor;

    public CodeHeaderComponent(
            String language,
            Editor editor,
            Project project,
            String fileExtension,
            EditorActionEnum editorActionEnum,
            Editor chosenEditor
    ) {
        super(new BorderLayout());
        this.codeEditor = editor;
        this.project = project;
        this.fileExtension = fileExtension;
        this.editorActionEnum = editorActionEnum;
        this.chosenEditor = chosenEditor;
        setBorder(JBUI.Borders.compound(JBUI.Borders.customLine(JBColor.border(), 1, 1, 1, 1),
                JBUI.Borders.empty(5)));
        add(new JBLabel(language), BorderLayout.LINE_START);
        add(createCodeActionsGroup(), BorderLayout.LINE_END);
    }

    private JPanel createCodeActionsGroup() {
        JPanel codeActionsGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));

        codeActionsGroup.add(new RoundedPanel()
                                .addIconJButton(new IconJButton(DevPilotIcons.COPY_ICON, DevPilotMessageBundle.get("devpilot.button.copy.tipText"),
                                                new CopyAction(codeEditor))));
        codeActionsGroup.add(new RoundedPanel()
                                .addIconJButton(new IconJButton(DevPilotIcons.INSERT_AT_CARET_ICON, DevPilotMessageBundle.get("devpilot.button.insert.tipText"),
                                                new InsertAtCaretAction(codeEditor, project))));
        codeActionsGroup.add(new RoundedPanel()
                                .addIconJButton(new IconJButton(DevPilotIcons.REPLACE_ICON, DevPilotMessageBundle.get("devpilot.button.replace.tipText"),
                                                new ReplaceSelectionAction(codeEditor, project))));
        codeActionsGroup.add(new RoundedPanel()
                               .addIconJButton(new IconJButton(DevPilotIcons.NEW_FILE_ICON, DevPilotMessageBundle.get("devpilot.button.new.tipText"),
                                               new NewFileAction(codeEditor, fileExtension, project, editorActionEnum, chosenEditor))));
        return codeActionsGroup;
    }

}
