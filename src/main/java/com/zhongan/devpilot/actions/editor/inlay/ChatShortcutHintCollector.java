package com.zhongan.devpilot.actions.editor.inlay;

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiUtilCore;
import com.zhongan.devpilot.DevPilotIcons;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.settings.state.ChatShortcutSettingState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import java.util.List;

import org.jetbrains.annotations.NotNull;

public class ChatShortcutHintCollector extends FactoryInlayHintsCollector {

    private List<String> supportedElementTypes;

    protected PresentationFactory factory;

    private DevPilotChatToolWindowService service;

    private Editor editor;

    public ChatShortcutHintCollector(@NotNull Editor editor, List<String> supportedElementsType) {
        super(editor);
        this.editor = editor;
        this.supportedElementTypes = supportedElementsType;
        this.factory = this.getFactory();
        this.service = editor.getProject().getService(DevPilotChatToolWindowService.class);
    }

    @Override
    public boolean collect(@NotNull PsiElement psiElement, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink) {
        if (ChatShortcutSettingState.getInstance().getEnable() &&
                supportedElementTypes.contains(PsiUtilCore.getElementType(psiElement).toString())) {

            inlayHintsSink.addBlockElement(getAnchorOffset(psiElement), true, true, 1000,
                    factory.seq(factory.textSpacePlaceholder(computeInitialWhitespace(editor, psiElement), false),
                            factory.icon(DevPilotIcons.SYSTEM_ICON_INLAY),
                            buildClickableTextChatShortcutEntry(" " + DevPilotMessageBundle.get("devpilot.inlay.shortcut.explain")
                                    + " | ", EditorActionEnum.EXPLAIN_THIS, psiElement),
                            buildClickableTextChatShortcutEntry(DevPilotMessageBundle.get("devpilot.inlay.shortcut.fix")
                                    + " | ", EditorActionEnum.FIX_THIS, psiElement),
                            buildClickableTextChatShortcutEntry(DevPilotMessageBundle.get("devpilot.inlay.shortcut.inlineComment")
                                    + " | ", EditorActionEnum.GENERATE_COMMENTS, psiElement),
                            buildClickableMethodCommentsShortcutEntry(DevPilotMessageBundle.get("devpilot.inlay.shortcut.methodComments") +
                                    " | ", psiElement),
                            buildClickableTextChatShortcutEntry(DevPilotMessageBundle.get("devpilot.inlay.shortcut.test"),
                                    EditorActionEnum.GENERATE_TESTS, psiElement)));
        }
        return true;
    }

    private InlayPresentation buildClickableTextChatShortcutEntry(String text, EditorActionEnum actionEnum, PsiElement psiElement) {
        return factory.seq(factory.referenceOnHover(factory.smallText(text), (mouseEvent, point) -> {
            TextRange textRange = psiElement.getTextRange();
            editor.getSelectionModel().setSelection(getAnchorOffset(psiElement), textRange.getEndOffset());

            service.handleActions(actionEnum);
        }));
    }

    private InlayPresentation buildClickableMethodCommentsShortcutEntry(String text, PsiElement psiElement) {
        return factory.seq(factory.referenceOnHover(factory.smallText(text), (mouseEvent, point) -> {
            TextRange textRange = psiElement.getTextRange();
            editor.getSelectionModel().setSelection(getAnchorOffset(psiElement), textRange.getEndOffset());

            ApplicationManager.getApplication().invokeLater(() -> {
                AnAction action = ActionManager.getInstance().getAction("com.zhongan.devpilot.actions.editor.generate.method.comments");
                DataContext context = SimpleDataContext.getProjectContext(editor.getProject());
                action.actionPerformed(new AnActionEvent(null, context, "", new Presentation(), ActionManager.getInstance(), 0));
            });
        }));
    }

    private static int getAnchorOffset(@NotNull PsiElement psiElement) {
        int anchorOffset = psiElement.getTextRange().getStartOffset();
        PsiElement[] children = psiElement.getChildren();
        for (PsiElement element : children) {
            if (!(element instanceof PsiComment) && !(element instanceof PsiWhiteSpace)) {
                anchorOffset = element.getTextRange().getStartOffset();
                break;
            }
        }
        return anchorOffset;
    }

    private int computeInitialWhitespace(Editor editor, PsiElement psiElement) {
        int lineNum = editor.getDocument().getLineNumber(psiElement.getTextRange().getStartOffset());
        String textOnLine = editor.getDocument().getText(new TextRange(editor.getDocument().getLineStartOffset(lineNum),
                                                                       editor.getDocument().getLineEndOffset(lineNum)));

        int whitespaceCounter = 0;
        for (char character : textOnLine.toCharArray()) {
            if (Character.isWhitespace(character)) {
                whitespaceCounter++;
            } else {
                break;
            }
        }

        return whitespaceCounter;
    }
}
