package com.zhongan.devpilot.gui.toolwindows.components;

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
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.parser.Parser;
import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.gui.toolwindows.components.code.CodeHeaderComponent;
import com.zhongan.devpilot.gui.toolwindows.components.code.GoToCode;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.MarkdownUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import org.apache.commons.lang3.StringUtils;

public class ContentComponent extends JPanel {

    public ContentComponent() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public Component createCodeComponent(Project project, String codeBlock, EditorActionEnum actionType, Editor chosenEditor) {
        com.vladsch.flexmark.util.ast.Document parse = Parser.builder().build().parse(codeBlock);
        FencedCodeBlock codeNode = (FencedCodeBlock) parse.getChildOfType(FencedCodeBlock.class);
        if (codeNode == null) {
            return null;
        }
        String code = codeNode.getContentChars().unescape().replaceAll("\\n$", "");
        String language = StringUtils.isEmpty(codeNode.getInfo().toString()) ? DefaultConst.DEFAULT_CODE_LANGUAGE : codeNode.getInfo().toString();

        String fileExt = MarkdownUtil.getFileExtensionFromLanguage(language);
        LightVirtualFile lightVirtualFile = new LightVirtualFile(System.currentTimeMillis() + fileExt, code);
        Document document = FileDocumentManager.getInstance().getDocument(lightVirtualFile);
        if (document == null) {
            document = EditorFactory.getInstance().createDocument(code);
        }

        Editor codeViewer = EditorFactory.getInstance().createEditor(document, project, lightVirtualFile, true, EditorKind.MAIN_EDITOR);
        codeViewer.setHeaderComponent(new CodeHeaderComponent(language, codeViewer, project, fileExt, actionType, chosenEditor));
        EditorSettings editorSettings = codeViewer.getSettings();
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
        codePanel.add(codeViewer.getComponent(), BorderLayout.CENTER);
        return codePanel;
    }

    public JTextPane createTextComponent(String textBlock) {
        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        textPane.setOpaque(true);
        textPane.setBackground(new JBColor(Gray._248, Gray._54));
        textPane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true);
        textPane.setBorder(JBUI.Borders.emptyLeft(5));
        textPane.setText(MarkdownUtil.textContent2Html(textBlock));
        return textPane;
    }

    public JPanel createRightActionComponent(String labelText, Project project, EditorInfo editorInfo) {
        JPanel rightActionPane = new JPanel();
        rightActionPane.setLayout(new BoxLayout(rightActionPane, BoxLayout.Y_AXIS));

        JTextPane rightActionLabel = createTextComponent(labelText);
        // to emphasize
        rightActionLabel.setBackground(new JBColor(Gray._252, Gray._60));
        rightActionPane.add(rightActionLabel);

        JPanel selectedCodeJumpPane = new JPanel();

        selectedCodeJumpPane.add(new JBLabel(DevPilotMessageBundle.get("devpilot.reference.content")));
        selectedCodeJumpPane.setBackground(new JBColor(Gray._248, Gray._54));
        selectedCodeJumpPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        selectedCodeJumpPane.add(new GoToCode(project, editorInfo));

        rightActionPane.add(selectedCodeJumpPane);
        return rightActionPane;
    }

}
