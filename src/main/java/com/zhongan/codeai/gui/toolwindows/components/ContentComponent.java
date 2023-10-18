package com.zhongan.codeai.gui.toolwindows.components;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ui.JBUI;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.parser.Parser;
import com.zhongan.codeai.gui.toolwindows.components.code.CodeHeaderComponent;
import com.zhongan.codeai.util.MarkdownUtil;

import org.apache.commons.lang3.StringUtils;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JPanel;

public class ContentComponent extends JPanel {

    public ContentComponent() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public Component createCodeComponent(Project project, String codeBlock) {
        com.vladsch.flexmark.util.ast.Document parse = Parser.builder().build().parse(codeBlock);
        FencedCodeBlock codeNode = (FencedCodeBlock) parse.getChildOfType(FencedCodeBlock.class);
        if (codeNode == null) {
            return null;
        }
        String code = codeNode.getContentChars().unescape().replaceAll("\\n$", "");
        String language = StringUtils.isEmpty(codeNode.getInfo().toString()) ? "text" : codeNode.getInfo().toString();

        String fileExt = MarkdownUtil.getFileExtensionFromLanguage(language);
        LightVirtualFile lightVirtualFile = new LightVirtualFile(System.currentTimeMillis() + fileExt, code);
        Document document = FileDocumentManager.getInstance().getDocument(lightVirtualFile);
        if (document == null) {
            document = EditorFactory.getInstance().createDocument(code);
        }

        Editor editor = EditorFactory.getInstance().createEditor(document, project, lightVirtualFile, true, EditorKind.MAIN_EDITOR);
        editor.setHeaderComponent(new CodeHeaderComponent(language, editor, project, fileExt));
        EditorSettings editorSettings = editor.getSettings();
        editorSettings.setGutterIconsShown(false);
        editorSettings.setShowIntentionBulb(false);
        editorSettings.setAdditionalLinesCount(0);
        editorSettings.setAdditionalColumnsCount(0);
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (psiFile != null) {
            // close code analyze
            DaemonCodeAnalyzer.getInstance(project).setHighlightingEnabled(psiFile, false);
        }

        JPanel codePanel = new JPanel();
        codePanel.setLayout(new BorderLayout());
        codePanel.add(editor.getComponent(), BorderLayout.CENTER);
        return codePanel;
    }

    public JEditorPane createTextComponent(String textBlock) {
        JEditorPane textPane = new JEditorPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        textPane.setBorder(JBUI.Borders.empty());
        textPane.setText(MarkdownUtil.mark2Html(textBlock));
        textPane.setOpaque(false);
        return textPane;
    }

}
