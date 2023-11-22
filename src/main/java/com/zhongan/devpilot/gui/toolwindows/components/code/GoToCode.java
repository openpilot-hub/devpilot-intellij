package com.zhongan.devpilot.gui.toolwindows.components.code;

import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleColoredComponent;
import com.zhongan.devpilot.gui.toolwindows.components.EditorInfo;
import com.zhongan.devpilot.util.EditorUtils;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import static com.intellij.ui.SimpleTextAttributes.GRAY_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;

public class GoToCode extends JPanel {

    public GoToCode(Project project, EditorInfo editorInfo) {

        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        this.setToolTipText(editorInfo.getFilePresentableUrl());
        this.add(createSimpleColoredComponent(editorInfo));
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                EditorUtils.openFileAndSelectLines(project, editorInfo.getFileUrl(),
                        editorInfo.getSelectedStartLine(), editorInfo.getSelectedEndLine());
            }
        });
    }

    private SimpleColoredComponent createSimpleColoredComponent(EditorInfo editorInfo) {
        SimpleColoredComponent simpleColoredComponent = new SimpleColoredComponent();
        simpleColoredComponent.setOpaque(false);
        simpleColoredComponent.setIcon(editorInfo.getFileIcon());
        simpleColoredComponent.append(editorInfo.getFileName(), REGULAR_ATTRIBUTES);
        simpleColoredComponent.append(":" + editorInfo.getSelectedStartLine() + "-" + editorInfo.getSelectedEndLine(),
                GRAY_ATTRIBUTES);
        return simpleColoredComponent;
    }

}
