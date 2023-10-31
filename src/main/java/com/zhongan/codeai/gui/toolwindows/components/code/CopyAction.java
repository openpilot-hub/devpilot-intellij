package com.zhongan.codeai.gui.toolwindows.components.code;

import com.intellij.openapi.editor.Editor;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CopyAction implements ActionListener {

    private final Editor editor;

    public CopyAction(Editor editor) {
        this.editor = editor;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String generatedText = this.editor.getSelectionModel().hasSelection() ?
                this.editor.getSelectionModel().getSelectedText() : this.editor.getDocument().getText();
        systemClipboard.setContents(new StringSelection(generatedText), null);
    }

}
