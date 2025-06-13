package com.zhongan.devpilot.actions.editor.inlay;

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.codeInsight.hints.presentation.SequencePresentation;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.SmartList;
import com.zhongan.devpilot.DevPilotIcons;
import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.settings.state.ChatShortcutSettingState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        var chatShortcutSetting = ChatShortcutSettingState.getInstance();
        if (!chatShortcutSetting.getEnable()) {
            return true;
        }

        boolean isSourceCode = isSourceCode(psiElement, editor);
        var elementType = PsiUtilCore.getElementType(psiElement).toString();

        if ("CLASS".equals(elementType)) {
            if (isSourceCode) {
                inlineRenderInlayPresentation(psiElement, editor, inlayHintsSink, Collections.singletonList(EditorActionEnum.GENERATE_TESTS));
            }
            return true;
        }

        if (!isSourceCode && supportedElementTypes.contains(elementType)) {
            inlineRenderInlayPresentation(psiElement, editor, inlayHintsSink, Collections.singletonList(EditorActionEnum.EXPLAIN_CODE));
            return true;
        }

        if (isSourceCode && supportedElementTypes.contains(elementType)) {
            if (chatShortcutSetting.isInlineDisplay()) {
                inlineRenderInlayPresentation(psiElement, editor, inlayHintsSink, buildInlayPresentationGroupData());
            } else {
                groupRenderPopupPresentation(inlayHintsSink, psiElement, editor);
            }
        }
        return true;
    }

    private InlayPresentation createInlayElement(Editor editor, PsiElement psiElement, List<EditorActionEnum> actions) {
        List<InlayPresentation> presentations = new ArrayList<>();

        presentations.add(factory.textSpacePlaceholder(computeInitialWhitespace(editor, psiElement), false));
        presentations.add(factory.icon(DevPilotIcons.SYSTEM_ICON_INLAY));

        for (int i = 0, len = actions.size(); i < len; i++) {
            String prefix = (i == 0) ? " " : StringUtils.EMPTY;
            String postfix = (i == len - 1) ? StringUtils.EMPTY : " | ";
            presentations.add(buildClickableInlayPresentation(prefix, postfix, actions.get(i), psiElement));
        }

        return factory.seq(presentations.toArray(new InlayPresentation[0]));
    }

    private void inlineRenderInlayPresentation(PsiElement psiElement, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink, List<EditorActionEnum> actions) {
        inlayHintsSink.addBlockElement(getAnchorOffset(psiElement),
                true,
                true,
                1000,
                createInlayElement(editor, psiElement, actions));
    }

    private void groupRenderPopupPresentation(InlayHintsSink inlayHintsSink, PsiElement psiElement, Editor editor) {
        PresentationFactory factory = getFactory();
        Document document = editor.getDocument();
        int offset = getAnchorOffset(psiElement);
        int line = document.getLineNumber(offset);
        int startOffset = document.getLineStartOffset(line);
        InlayPresentation finalPresentation = createPopupPresentation(factory, editor, psiElement, startOffset, offset);
        inlayHintsSink.addBlockElement(startOffset, true, true, 300, finalPresentation);
    }

    private InlayPresentation createPopupPresentation(PresentationFactory factory, Editor editor,
                                                      PsiElement psiElement, int startOffset, int offset) {
        int gap = computeInitialWhitespace(editor, psiElement);
        List<InlayPresentation> presentations = new SmartList<>();
        presentations.add(factory.textSpacePlaceholder(gap, true));
        presentations.add(factory.smallScaledIcon(DevPilotIcons.SYSTEM_ICON_INLAY));
        presentations.add(factory.smallScaledIcon(AllIcons.Actions.FindAndShowNextMatchesSmall));
        presentations.add(factory.textSpacePlaceholder(1, true));

        SequencePresentation shiftedPresentation = new SequencePresentation(presentations);

        return factory.referenceOnHover(shiftedPresentation, (event, translated) ->
                showPopup(editor, psiElement, event)
        );
    }

    private void showPopup(Editor editor, PsiElement psiElement, MouseEvent event) {
        List<EditorActionEnum> options = buildInlayPresentationGroupData();
        JBPopup popup = JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<EditorActionEnum>(StringUtils.EMPTY, options) {
            public @NotNull String getTextFor(EditorActionEnum value) {
                return DevPilotMessageBundle.get(value.getInlayLabel());
            }

            public @Nullable PopupStep<?> onChosen(EditorActionEnum selectedValue, boolean finalChoice) {
                handleActionCallback(selectedValue, psiElement);
                return FINAL_CHOICE;
            }
        });
        popup.showInScreenCoordinates(editor.getComponent(), event.getLocationOnScreen());
    }

    private List<EditorActionEnum> buildInlayPresentationGroupData() {
        List<EditorActionEnum> options = new ArrayList<>();
        options.add(EditorActionEnum.EXPLAIN_CODE);
        options.add(EditorActionEnum.FIX_CODE);
        options.add(EditorActionEnum.GENERATE_COMMENTS);
        options.add(EditorActionEnum.COMMENT_METHOD);
        options.add(EditorActionEnum.GENERATE_TESTS);
        return options;
    }

    public static boolean isSourceCode(PsiElement psiElement, Editor editor) {
        Document document = editor.getDocument();
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(psiElement);
        if (virtualFile == null || !virtualFile.isWritable()) {
            return false;
        }
        FileIndexFacade indexFacade = FileIndexFacade.getInstance(psiElement.getProject());
        FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(virtualFile.getName());
        return !fileType.isBinary() && !document.getText().trim().isEmpty() && (!indexFacade.isInLibrarySource(virtualFile) && !indexFacade.isInLibraryClasses(virtualFile));
    }

    private InlayPresentation buildClickableInlayPresentation(String displayPrefixText, String displaySuffixText, EditorActionEnum actionEnum, PsiElement psiElement) {
        return factory.seq(factory.referenceOnHover(factory.smallText(displayPrefixText + DevPilotMessageBundle.get(actionEnum.getInlayLabel()) + displaySuffixText), (mouseEvent, point) -> {
            handleActionCallback(actionEnum, psiElement);
        }));
    }

    private void handleActionCallback(EditorActionEnum actionEnum, PsiElement psiElement) {
        TextRange textRange = psiElement.getTextRange();
        editor.getSelectionModel().setSelection(textRange.getStartOffset(), textRange.getEndOffset());

        if (EditorActionEnum.COMMENT_METHOD.equals(actionEnum)) {
            ApplicationManager.getApplication().invokeLater(() -> {
                moveCareToPreviousLineStart(editor, textRange.getStartOffset());
            });
        }

        service.handleActions(actionEnum, psiElement, DefaultConst.SMART_CHAT_TYPE);
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

    public void moveCareToPreviousLineStart(Editor editor, int offset) {
        Project project = editor.getProject();
        int previousLineNumber = getPreviousLineNumber(project, offset);
        if (previousLineNumber == -1) {
            return;
        }
        int lineStartOffset = editor.getDocument().getLineStartOffset(previousLineNumber);
        CaretModel caretModel = editor.getCaretModel();
        caretModel.moveToOffset(lineStartOffset);
    }

    public int getPreviousLineNumber(Project project, int offset) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

        if (editor != null) {
            Document document = editor.getDocument();
            int lineNumber = document.getLineNumber(offset);
            return lineNumber > 0 ? lineNumber - 1 : -1;
        }

        return -1;
    }
}
